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
 * Extends {@link net.sf.okapi.filters.po.POFilter} to provide human-readable text unit names and
 * add gettext plural support.
 *
 * <h3>Text Unit names</h3>
 *
 * Okapi's base filter produces text units with generated names. This subclass updates them on the
 * fly, replacing them with human-readable data, i.e. the source string followed by msgctxt if
 * present.
 *
 * <h3>Plural support</h3>
 *
 * <p>When parsing a plural group, Okapi's base filter simply emits one text unit per each target
 * variant (msgstr line). This subclass intercepts the event stream to:
 *
 * <ul>
 *   <li>Rewrite the Plural-Forms header to match rules for the target locale
 *   <li>Map PO plural forms (from msgstr indices) to CLDR forms (zero, one, two, few, many, other)
 *       (via {@link PoPluralRule}) and append to the text unit name
 *   <li>Complete missing CLDR forms according to rules for the target locale
 * </ul>
 *
 * <h3>Other notes</h3>
 *
 * <p>This filter also extracts source-code reference comments (#:) as usage annotations
 *
 * @author jaurambualt
 */
@Configurable
public class POFilter extends net.sf.okapi.filters.po.POFilter {

  static Logger logger = LoggerFactory.getLogger(POFilter.class);

  public static final String FILTER_CONFIG_ID = "okf_po@mojito";

  // Having " _" as plural separator wasn't very wise... Android filter for example doesn't have a
  // space: "_".
  // see {@link ThirdPartySyncCommand} too.
  // Consider cleanup but that not that simple as it would require migrating data...
  static final String PLURAL_SEPARATOR = " _";

  static final String USAGE_LOCATION_GROUP_NAME = "location";
  static final String USAGE_LOCATION_PATTERN = "#: (?<location>.*)";

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

  /**
   * The event loop is overridden to intercept events from the parent Okapi POFilter and:
   *
   * <ul>
   *   <li>Rewrite the Plural-Forms header when localizing to a target locale
   *   <li>Set text unit names to the source content, with msgctxt appended when present
   *   <li>Extract source-code reference comments (#:) as usage annotations
   *   <li>Buffer and rewrite plural text unit groups via {@link POFilter#processNextPluralGroup}
   * </ul>
   *
   * Non-plural events are processed and returned immediately. Plural groups consume multiple parent
   * events at once and buffer the results in {@link #eventQueue} so they can be yielded one at a
   * time by subsequent {@link #next()} calls.
   */
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
   *
   * <p>The parent's Okapi POFilter emits one TEXT_UNIT per msgstr[N]. The source on form 0 is the
   * msgid (singular), and on form 1+ it's msgid_plural. We capture these when processing the group
   * to set the correct text unit name and source on each plural variant.
   *
   * <p>The name of every plural variant is derived from the singular source (msgid), not from the
   * variant's own source — this keeps plural text unit names consistent across all forms.
   *
   * <p>The source must also be corrected to handle an edge case in how Okapi assigns sources when
   * the file has more plural forms than the target locale needs. See {@link
   * #extractPluralSourceFromSkeleton} for the workaround.
   */
  void processNextPluralGroup(Event startGroupEvent) {
    String startGroupSkeleton = startGroupEvent.getStartGroup().getSkeleton().toString();
    Set<String> usagesFromSkeleton = getUsagesFromSkeleton(startGroupSkeleton);

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
    // When nPlurals == 1 (e.g. Japanese) there is no second text unit, but we still need
    // msgid_plural — see extractPluralSourceFromSkeleton javadoc for details on the PO/CLDR
    // mismatch.
    String pluralSource =
        textUnitEvents.size() > 1
            ? textUnitEvents.get(1).getTextUnit().getSource().toString()
            : extractPluralSourceFromSkeleton(startGroupSkeleton);

    // Text units whose PO form index has no CLDR mapping (e.g. form 1 for Japanese ONE_FORM)
    // are filtered out
    List<Event> filteredEvents = new ArrayList<>();
    for (int i = 0; i < textUnitEvents.size(); i++) {
      String cldrForm = poPluralRule.poFormToCldrForm(Integer.toString(i));
      if (cldrForm != null) {
        ITextUnit textUnit = textUnitEvents.get(i).getTextUnit();
        setTextUnitName(textUnit, singularSource);
        appendPluralFormToName(textUnit, cldrForm);
        filteredEvents.add(textUnitEvents.get(i));
      }
    }

    PluralsHolder pluralsHolder = new PoPluralsHolder(singularSource, pluralSource);
    pluralsHolder.loadEvents(filteredEvents);
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
      // It's ok to map EN `other` to every non-"one" target category regardless of the target
      // language rules, because the UI i18n libraries themselves account for cross-language CLDR
      // category mappings at render time. Mojito just needs to generate enough categories to
      // satisfy the CLDR rules.
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

  void rewritePluralFormInHeader(DocumentPart documentPart) {
    if (targetLocale != null && !LocaleId.EMPTY.equals(targetLocale)) {
      documentPart.setProperty(new Property("pluralforms", poPluralRule.getRule()));
    }
  }

  /**
   * Extracts and unescapes the {@code msgid_plural} value from the START_GROUP skeleton.
   *
   * <p>This is needed because of a mismatch between PO and CLDR plural models. Okapi assigns
   * sources by PO index: {@code msgstr[0]} always gets {@code msgid} (singular) and {@code
   * msgstr[1+]} gets {@code msgid_plural}. But CLDR's "other" form — which should have the plural
   * source — can map to {@code msgstr[0]} for locales with a single plural form (e.g. Japanese
   * nPlurals=1). In that case Okapi only emits one TEXT_UNIT with the singular source, yet we need
   * the plural source to match the text unit that extraction stored in the database (extraction is
   * locale-independent and always assigns {@code msgid_plural} as the source for the "other" form).
   *
   * <p>The skeleton always contains the raw {@code msgid_plural "..."} line(s) since that is what
   * triggers the START_GROUP event in Okapi's POFilter. The raw quoted content is extracted here
   * and unescaped via the parent's {@code unescape} method (accessed through reflection because it
   * is private with no public alternative).
   */
  String extractPluralSourceFromSkeleton(String skeleton) {
    StringBuilder raw = new StringBuilder();
    boolean inMsgIdPlural = false;

    for (String line : skeleton.split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("msgid_plural")) {
        inMsgIdPlural = true;
        appendQuotedContent(raw, line);
      } else if (inMsgIdPlural && trimmed.startsWith("\"")) {
        appendQuotedContent(raw, line);
      } else if (inMsgIdPlural) {
        break;
      }
    }

    return unescapePo(raw.toString());
  }

  private static void appendQuotedContent(StringBuilder sb, String line) {
    int first = line.indexOf('"');
    int last = line.lastIndexOf('"');
    if (first >= 0 && last > first) {
      sb.append(line, first + 1, last);
    }
  }

  /** Delegates to the parent's private {@code unescape} method via reflection. */
  private String unescapePo(String text) {
    try {
      return (String) PARENT_UNESCAPE.invoke(this, text);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to invoke Okapi POFilter.unescape", e);
    }
  }

  private static final java.lang.reflect.Method PARENT_UNESCAPE;

  static {
    try {
      PARENT_UNESCAPE =
          net.sf.okapi.filters.po.POFilter.class.getDeclaredMethod("unescape", String.class);
      PARENT_UNESCAPE.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new ExceptionInInitializerError(e);
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
