package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation;
import com.box.l10n.mojito.po.PoPluralRule;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Extends {@link net.sf.okapi.filters.po.POFilter} to somehow support gettext
 * plural and surface message context as part of the textunit name.
 * <p>
 * Maps po plural form to cldr using {@link PoPluralRuleHelper
 *
 * @author jaurambualt
 */
@Configurable
public class POFilter extends net.sf.okapi.filters.po.POFilter {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(POFilter.class);

  public static final String FILTER_CONFIG_ID = "okf_po@mojito";

  static final String USAGE_LOCATION_GROUP_NAME = "location";
  static final String USAGE_LOCATION_PATTERN = "#: (?<location>.*)";

  // Having " _" as plural separator wasn't very wise... Android filter for example doesn't have a
  // space: "_".
  // see {@link ThirdPartySyncCommand} too.
  // Consider cleanup but that not that simple as it would require migrating data...
  static final String PLURAL_SEPARATOR = " _";

  List<Event> eventQueue = new ArrayList<>();

  LocaleId targetLocale;

  PoPluralRule poPluralRule;

  boolean hasCopyFormsOnImport = false;

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public List<FilterConfiguration> getConfigurations() {
    List<FilterConfiguration> list = new ArrayList<>();
    list.add(
        new FilterConfiguration(
            getName(),
            getMimeType(),
            getClass().getName(),
            "PO file with plural handling and text unit name including msgctxt",
            "Configuration for .po files."));

    return list;
  }

  @Override
  public void open(RawDocument input) {
    super.open(input);
    targetLocale = input.getTargetLocale();
    hasCopyFormsOnImport = input.getAnnotation(CopyFormsOnImport.class) != null;
    poPluralRule = PoPluralRule.fromBcp47Tag(targetLocale.toBCP47());

    input.setAnnotation(
        new RemoveUntranslatedStategyAnnotation(
            RemoveUntranslatedStrategy.PLACEHOLDER_AND_POST_PROCESSING));

    input.setAnnotation(
        new OutputDocumentPostProcessingAnnotation(
            new OutputDocumentPostProcessingAnnotation.OutputDocumentPostProcessorBase() {
              @Override
              public String execute(String content) {
                if (hasRemoveUntranslated()) {
                  content = removeUntranslated(content);
                }
                return content;
              }
            }));
  }

  @Override
  public boolean hasNext() {
    return !eventQueue.isEmpty() || super.hasNext();
  }

  @Override
  public Event next() {
    if (!eventQueue.isEmpty()) {
      return eventQueue.remove(0);
    }

    Event event = super.next();

    if (isPluralGroupStarting(event)) {
      processNextPluralGroup(event);
      return eventQueue.remove(0);
    }

    if (event.isDocumentPart()) {
      rewritePluralFormInHeader(event.getDocumentPart());
    }

    if (event.isTextUnit()) {
      TextUnit textUnit = (TextUnit) event.getTextUnit();
      setTextUnitName(textUnit, textUnit.getSource().toString());
      addUsagesToTextUnit(textUnit);
    }

    return event;
  }

  private boolean isPluralGroupStarting(Event event) {
    return event != null
        && event.isStartGroup()
        && "x-gettext-plurals".equals(event.getStartGroup().getType());
  }

  /**
   * Consumes a complete plural group from the parent filter (START_GROUP -> TEXT_UNIT* ->
   * END_GROUP), remaps PO form indices to CLDR plural forms, completes any missing forms for the
   * target locale, and buffers all resulting events in {@link #eventQueue}.
   */
  void processNextPluralGroup(Event startGroupEvent) {
    Set<String> usagesFromSkeleton =
        getUsagesFromSkeleton(startGroupEvent.getStartGroup().getSkeleton().toString());

    eventQueue.add(startGroupEvent);

    List<Event> textUnitEvents = new ArrayList<>();
    Event next = super.next();
    while (next != null && !next.isEndGroup()) {
      if (next.isTextUnit()) {
        textUnitEvents.add(next);
      }
      next = super.next();
    }

    String singularSource = textUnitEvents.get(0).getTextUnit().getSource().toString();

    // When nPlurals >= 2 the second text unit's source is the msgid_plural.
    // When nPlurals == 1 (e.g. Japanese) there is only one text unit — the next commit
    // adds extractPluralSourceFromSkeleton to handle that edge case correctly.
    String pluralSource =
        textUnitEvents.size() > 1
            ? textUnitEvents.get(1).getTextUnit().getSource().toString()
            : singularSource;

    for (int i = 0; i < textUnitEvents.size(); i++) {
      ITextUnit textUnit = textUnitEvents.get(i).getTextUnit();
      setTextUnitName(textUnit, singularSource);
      String cldrForm = poPluralRule.poFormToCldrForm(Integer.toString(i));
      if (cldrForm != null) {
        appendPluralFormToName(textUnit, cldrForm);
      }
    }

    PluralsHolder pluralsHolder = new PoPluralsHolder(singularSource, pluralSource);
    pluralsHolder.loadEvents(textUnitEvents);
    List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);

    for (Event e : completedForms) {
      if (e.isTextUnit()) {
        setUsagesAnnotationOnTextUnit(usagesFromSkeleton, e.getTextUnit());
      }
    }

    eventQueue.addAll(completedForms);

    if (next != null) {
      eventQueue.add(next);
    }
  }

  private void setUsagesAnnotationOnTextUnit(Set<String> usagesFromSkeleton, ITextUnit textUnit) {
    textUnit.setAnnotation(new UsagesAnnotation((usagesFromSkeleton)));
  }

  class PoPluralsHolder extends PluralsHolder {

    private final String singularSource;
    private final String pluralSource;

    PoPluralsHolder(String singularSource, String pluralSource) {
      this.singularSource = singularSource;
      this.pluralSource = pluralSource;
    }

    @Override
    public List<Event> getCompletedForms(LocaleId localeId) {

      if (other == null) {
        if (few != null) {
          logger.debug("other is not defined, copying from few (e.g. Russian)");
          other = createCopyOf(few, "few", "other");
        } else if (zero != null) {
          logger.debug("other and few are not defined, copying from zero (e.g. Arabic)");
          other = createCopyOf(zero, "zero", "other");
        } else if (two != null) {
          logger.debug("other, few and zero are not defined, copying from two");
          other = createCopyOf(two, "two", "other");
        }
      }

      return super.getCompletedForms(localeId);
    }

    @Override
    void adaptTextUnitToCLDRForm(ITextUnit textUnit, String cldrPluralForm) {
      if ("one".equals(cldrPluralForm)) {
        textUnit.setSource(new TextContainer(singularSource));
      } else {
        textUnit.setSource(new TextContainer(pluralSource));
      }
    }

    @Override
    void replaceFormInSkeleton(
        GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
      logger.debug(
          "Replace in skeleton form: {} to {} ({})",
          sourceForm,
          targetForm,
          poPluralRule.cldrFormToPoForm(targetForm));

      String cldrFormToGettextForm = poPluralRule.cldrFormToPoForm(targetForm);

      if (cldrFormToGettextForm != null) {
        for (GenericSkeletonPart part : genericSkeleton.getParts()) {
          StringBuilder sb = part.getData();
          String str =
              sb.toString().replaceAll("msgstr\\[\\d\\]", "msgstr[" + cldrFormToGettextForm + "]");
          sb.replace(0, sb.length(), str);
        }
      } else {
        logger.debug("No replacement, no PO idx for CLDR form: {}", targetForm);
      }
    }

    @Override
    Multimap<String, String> getFormsToCopy(LocaleId localeId) {
      Multimap<String, String> formsToCopy = super.getFormsToCopy(localeId);

      if (hasCopyFormsOnImport) {
        logger.debug("Copy required form for import");
        for (Map.Entry<String, String> entry :
            poPluralRule.getFormsToCopyOnImport().getFormMap().entries()) {
          formsToCopy.put(entry.getKey(), entry.getValue());
        }
      }

      return formsToCopy;
    }

    @Override
    void retainForms(LocaleId localeId, List<String> pluralForms) {
      if (localeId != null && !LocaleId.EMPTY.equals(localeId)) {
        Set<String> cldrRules = poPluralRule.getCldrForms();
        pluralForms.retainAll(cldrRules);
      }
    }
  }

  /**
   * Sets the text unit name to the given base name, appending the msgctxt when present. Examples:
   *
   * <ul>
   *   <li>{@code "Hello"} — simple entry
   *   <li>{@code "File --- menu"} — with msgctxt "menu"
   * </ul>
   */
  void setTextUnitName(ITextUnit textUnit, String baseName) {
    Property property = textUnit.getProperty(POFilter.PROPERTY_CONTEXT);

    StringBuilder newName = new StringBuilder(baseName);

    if (property != null) {
      newName.append(" --- ").append(property.getValue());
    }

    textUnit.setName(newName.toString());
  }

  /**
   * Appends a CLDR plural form suffix to the text unit's current name, e.g. {@code "item"} becomes
   * {@code "item _other"}. The suffix must be set before {@link PluralsHolder#loadEvents} so that
   * {@link PluralsHolder#getCldrPluralFormOfEvent} can parse the form back from the name.
   */
  void appendPluralFormToName(ITextUnit textUnit, String cldrPluralForm) {
    textUnit.setName(textUnit.getName() + PLURAL_SEPARATOR + cldrPluralForm);
  }

  /**
   * Rewrite the plural forms if processing a target locale.
   *
   * @param documentPart
   */
  void rewritePluralFormInHeader(DocumentPart documentPart) {
    if (targetLocale != null && !LocaleId.EMPTY.equals(targetLocale)) {
      documentPart.setProperty(new Property("pluralforms", poPluralRule.getRule()));
    }
  }

  void addUsagesToTextUnit(TextUnit textUnit) {
    Set<String> usageLocationsFromSkeleton =
        getUsagesFromSkeleton(textUnit.getSkeleton().toString());
    setUsagesAnnotationOnTextUnit(usageLocationsFromSkeleton, textUnit);
  }

  Set<String> getUsagesFromSkeleton(String skeleton) {
    Set<String> locations = new LinkedHashSet<>();

    Pattern pattern = Pattern.compile(USAGE_LOCATION_PATTERN);
    Matcher matcher = pattern.matcher(skeleton);

    while (matcher.find()) {
      locations.add(matcher.group(USAGE_LOCATION_GROUP_NAME));
    }

    return locations;
  }

  static String removeUntranslated(String poFile) {
    StringBuilder poFileWithoutUntranslatedPlural = new StringBuilder();
    Scanner scanner = new Scanner(poFile);

    StringBuilder block = new StringBuilder();
    boolean hasMsgId = false;
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();

      String trimedLine = line.trim();
      boolean lineStartWithMsgId = trimedLine.startsWith("msgid ");
      boolean lineHasCommentOrMsgId = lineStartWithMsgId || trimedLine.startsWith("#");

      if (lineHasCommentOrMsgId && hasMsgId) {
        if (block.indexOf(RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER) == -1) {
          poFileWithoutUntranslatedPlural.append(block);
        }
        block = new StringBuilder(line).append("\n");
        hasMsgId = false;
      } else {
        hasMsgId = hasMsgId || lineStartWithMsgId;
        block.append(line).append("\n");
      }
    }

    if (block.indexOf(RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER) == -1) {
      poFileWithoutUntranslatedPlural.append(block);
    }

    boolean hasBuilderEndReturnLine =
        poFileWithoutUntranslatedPlural.length() > 0
            && poFileWithoutUntranslatedPlural.charAt(poFileWithoutUntranslatedPlural.length() - 1)
                == '\n';

    if (!poFile.endsWith("\n") && hasBuilderEndReturnLine) {
      poFileWithoutUntranslatedPlural.deleteCharAt(poFileWithoutUntranslatedPlural.length() - 1);
    }

    return poFileWithoutUntranslatedPlural.toString();
  }
}
