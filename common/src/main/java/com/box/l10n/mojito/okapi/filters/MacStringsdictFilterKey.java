package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.ExtractUsagesFromTextUnitComments;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.*;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.*;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/** @author emagalindan */
@Configurable
public class MacStringsdictFilterKey extends XMLFilter {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(MacStringsdictFilterKey.class);

  public static final String FILTER_CONFIG_ID = "okf_macStringdict@mojito-key";
  public static final String MAC_STRINGSDICT_CONFIG_FILE_NAME = "macStringsdict_mojito.fprm";

  // Match single or multi-line comments
  private static final String XML_COMMENT_PATTERN = "\\s*?<!--(?<comment>(.*?\\s)*?)-->";
  private static final String XML_COMMENT_GROUP_NAME = "comment";

  LocaleId targetLocale;

  List<Event> eventQueue = new ArrayList<>();

  @Autowired TextUnitUtils textUnitUtils;

  @Autowired ExtractUsagesFromTextUnitComments extractUsagesFromTextUnitComments;

  @Autowired UnescapeUtils unescapeUtils;

  boolean hasAnnotation;

  String comment;

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
            "Apple Stringsdict",
            "Configuration for Apple Stringsdict files.",
            MAC_STRINGSDICT_CONFIG_FILE_NAME,
            ".stringsdict;"));
    return list;
  }

  @Override
  public boolean hasNext() {
    return !eventQueue.isEmpty() || super.hasNext();
  }

  @Override
  public Event next() {
    Event event;

    if (eventQueue.isEmpty()) {
      readNextEvents();
    }

    event = eventQueue.remove(0);

    return event;
  }

  @Override
  public void open(RawDocument input) {
    super.open(input);
    targetLocale = input.getTargetLocale();
    logger.debug("target locale: ", targetLocale);
    hasAnnotation = input.getAnnotation(CopyFormsOnImport.class) != null;
  }

  public void readNextEvents() {
    Event next = getNextWithProcess();

    if (next.isTextUnit() && isPluralGroupStarting(next.getResource())) {
      readPlurals(next);
    } else {
      eventQueue.add(next);
    }
  }

  private void processTextUnit(Event event) {
    if (event != null && event.isTextUnit()) {
      TextUnit textUnit = (TextUnit) event.getTextUnit();
      String sourceString = textUnitUtils.getSourceAsString(textUnit);
      textUnitUtils.replaceSourceString(textUnit, unescapeUtils.unescape(sourceString));
      extractNoteFromXMLCommentInSkeletonIfNone(textUnit);
      textUnitUtils.setNote(textUnit, comment);
      extractUsagesFromTextUnitComments.addUsagesToTextUnit(textUnit);
    }
  }

  /**
   * Extract the note from XML comments only if there is no note on the text unit. In other words if
   * a note was specify via attribute like description for android it won't be overridden by an
   * comments present in the XML file.
   *
   * @param textUnit the text unit for which comments should be extracted
   */
  protected void extractNoteFromXMLCommentInSkeletonIfNone(TextUnit textUnit) {

    String skeleton = textUnit.getSkeleton().toString();

    if (textUnit.getProperty(Property.NOTE) == null) {
      String note = getNoteFromXMLCommentsInSkeleton(skeleton);
      if (note != null) {
        comment = note;
      }
    }
  }

  /**
   * Gets the note from the XML comments in the skeleton.
   *
   * @param skeleton that may contains comments
   * @return the note or <code>null</code>
   */
  protected String getNoteFromXMLCommentsInSkeleton(String skeleton) {

    String note = null;

    StringBuilder commentBuilder = new StringBuilder();

    Pattern pattern = Pattern.compile(XML_COMMENT_PATTERN);
    Matcher matcher = pattern.matcher(skeleton);

    while (matcher.find()) {
      String comment = matcher.group(XML_COMMENT_GROUP_NAME).trim();
      if (!comment.startsWith("Location: ")) {
        if (commentBuilder.length() > 0) {
          commentBuilder.append(" ");
        }
        commentBuilder.append(comment);
      }
    }

    if (commentBuilder.length() > 0) {
      note = commentBuilder.toString();
    }

    return note;
  }

  private Event getNextWithProcess() {
    Event next = super.next();
    processTextUnit(next);
    return next;
  }

  /**
   * Read through events from the plural starting event until (but not including) the ending event
   *
   * @param next start event
   */
  private void readPlurals(Event next) {

    List<Event> pluralEvents = new ArrayList<>();

    do {
      pluralEvents.add(next);
      next = getNextWithProcess();
    } while (next != null && !isPluralGroupEnding(next.getResource()));

    pluralEvents = adaptPlurals(pluralEvents);

    eventQueue.addAll(pluralEvents);

    if (isPluralGroupStarting(next.getResource())) {
      readPlurals(next);
    } else {
      eventQueue.add(next);
    }
  }

  /**
   * Determine whether resource contains the start of a plural group
   *
   * @param resource resource used to determine if plural group is starting
   * @return True if resource is the start of a plural group, False otherwise
   */
  protected boolean isPluralGroupStarting(IResource resource) {
    String toString = resource.getSkeleton().toString();
    Pattern p = Pattern.compile("<key>NSStringFormatSpecTypeKey</key>");
    Matcher matcher = p.matcher(toString);
    boolean found = matcher.find();
    return found;
  }

  /**
   * Determine whether resource contains the end of a plural group
   *
   * @param resource resource used to determine if plural group is ending
   * @return True if resource is the end of a plural group, False otherwise
   */
  protected boolean isPluralGroupEnding(IResource resource) {
    String toString = resource.getSkeleton().toString();
    Pattern p = Pattern.compile("</dict>\n</dict>");
    Matcher matcher = p.matcher(toString);
    return matcher.find();
  }

  /**
   * Updates any missing CDLR plural forms, if any
   *
   * @param pluralEvents list of extracted plural events
   * @return list of all plural forms
   */
  protected List<Event> adaptPlurals(List<Event> pluralEvents) {
    logger.debug("Adapt plural forms if needed");
    PluralsHolder pluralsHolder = new MacStringsdictPluralsHolder();
    pluralsHolder.loadEvents(pluralEvents);
    logger.debug("target locale: ", targetLocale);
    List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);
    return completedForms;
  }

  class MacStringsdictPluralsHolder extends PluralsHolder {

    public static final String KEY_RES_KEY = "<key>(?<res>.+?)</key>";
    public static final String KEY_RES_GROUP = "res";

    public static final String PLURAL_FORM_KEY = "\n\\s*?<key>.+?$";
    String firstForm = null;

    @Override
    protected void loadEvents(List<Event> pluralEvents) {

      if (!pluralEvents.isEmpty()) {
        Event firstEvent = pluralEvents.get(0);
        firstForm = getPluralFormFromSkeleton(firstEvent.getResource());
      }

      super.loadEvents(pluralEvents);
    }

    @Override
    public List<Event> getCompletedForms(LocaleId localeId) {
      List<Event> completedForms = super.getCompletedForms(localeId);
      swapSkeletonBetweenOldFirstAndNewFirst(
          firstForm, getPluralFormFromSkeleton(completedForms.get(0).getResource()));

      return completedForms;
    }

    String getPluralFormFromSkeleton(IResource resource) {
      String toString = resource.getSkeleton().toString();
      Pattern p = Pattern.compile(KEY_RES_KEY);
      Matcher matcher = p.matcher(toString);
      String res = null;
      while (matcher.find()) {
        res = matcher.group(KEY_RES_GROUP).trim();
      }
      return res;
    }

    @Override
    void updateFormInSkeleton(ITextUnit textUnit) {
      boolean ignore = true;
      GenericSkeleton genericSkeleton = (GenericSkeleton) textUnit.getSkeleton();
      for (GenericSkeletonPart genericSkeletonPart : genericSkeleton.getParts()) {
        String partString = genericSkeletonPart.toString();
        Pattern p = Pattern.compile(PLURAL_FORM_KEY);
        Matcher matcher = p.matcher(partString);
        if (matcher.find()) {
          String match = matcher.group();
          genericSkeletonPart.setData(match);
          ignore = false;
        }
        if (ignore) {
          genericSkeletonPart.setData("");
        }
      }
    }

    @Override
    void replaceFormInSkeleton(
        GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
      for (GenericSkeletonPart part : genericSkeleton.getParts()) {
        StringBuilder sb = part.getData();
        String str = sb.toString().replace("<key>" + sourceForm, "<key>" + targetForm);
        sb.replace(0, sb.length(), str);
      }
    }
  }
}
