package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.json.JsonObjectRemoverByValue;
import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation;
import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation.OutputDocumentPostProcessorBase;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.annotation.Note;
import net.sf.okapi.common.annotation.NoteAnnotation;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.json.JsonEventBuilder;
import net.sf.okapi.filters.json.Parameters;
import net.sf.okapi.filters.json.parser.JsonKeyTypes;
import net.sf.okapi.filters.json.parser.JsonValueTypes;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeanaurambault
 */
public class JSONFilter extends net.sf.okapi.filters.json.JSONFilter {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(JSONFilter.class);

  public static final String FILTER_CONFIG_ID = "okf_json@mojito";

  Pattern noteKeyPattern = null;
  Pattern usagesKeyPattern = null;

  /**
   * To keep the usage until a new one is found. When false it will reset the usages when the text
   * unit/object end is reached.
   */
  boolean usagesKeepOrReplace = false;

  /**
   * To keep the note until a new one is found. When false it will reset the usages when the text
   * unit/object end is reached.
   */
  boolean noteKeepOrReplace = false;

  /**
   * Remove the suffix from the key.
   *
   * <p>Typically useful for FormatJS:
   *
   * <p>"text-unit-name": { "defaultMessage": "example", "description": "example description" }
   *
   * <p>text unit name would be text-unit-name/defaultMessage. With removeKeySuffix set, the
   * "/defaultMessage" can be removed.
   */
  String removeKeySuffix = null;

  NoteAnnotation noteAnnotation;
  UsagesAnnotation usagesAnnotation;
  String currentKeyName;
  String comment = null;

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public List<FilterConfiguration> getConfigurations() {
    List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();

    list.add(
        new FilterConfiguration(
            this.getName(),
            "application/json",
            this.getClass().getName(),
            "JSON (JavaScript Object Notation)",
            "Configuration for JSON files"));
    return list;
  }

  @Override
  public void open(RawDocument input) {
    applyFilterOptions(input);
    super.open(input);
    input.setAnnotation(
        new RemoveUntranslatedStategyAnnotation(
            RemoveUntranslatedStrategy.PLACEHOLDER_AND_POST_PROCESSING));
    input.setAnnotation(
        new OutputDocumentPostProcessingAnnotation(
            new OutputDocumentPostProcessorBase() {
              @Override
              public String execute(String content) {

                if (hasRemoveUntranslated()) {
                  content = removeUntranslated(content);
                }

                return content;
              }
            }));
  }

  static String removeUntranslated(String content) {
    return JsonObjectRemoverByValue.remove(
        content, RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER);
  }

  void applyFilterOptions(RawDocument input) {
    FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);
    Parameters parameters = this.getParameters();
    logger.debug("Set default value for the filter");
    parameters.setUseFullKeyPath(true);
    parameters.setUseLeadingSlashOnKeyPath(false);

    logger.debug("Override with filter options");
    if (filterOptions != null) {
      // okapi options
      filterOptions.getBoolean("useFullKeyPath", b -> parameters.setUseFullKeyPath(b));
      filterOptions.getBoolean("extractAllPairs", b -> parameters.setExtractAllPairs(b));
      filterOptions.getString("exceptions", s -> parameters.setExceptions(s));
      filterOptions.getString("codeFinderData", s -> parameters.setCodeFinderData(s));

      // mojito options
      filterOptions.getString("noteKeyPattern", s -> noteKeyPattern = Pattern.compile(s));
      filterOptions.getString("usagesKeyPattern", s -> usagesKeyPattern = Pattern.compile(s));
      filterOptions.getBoolean("noteKeepOrReplace", b -> noteKeepOrReplace = b);
      filterOptions.getBoolean("usagesKeepOrReplace", b -> usagesKeepOrReplace = b);
      filterOptions.getString("removeKeySuffix", s -> removeKeySuffix = s);
      filterOptions.getBoolean(
          "convertToHtmlCodes",
          b -> {
            if (b) {
              input.setAnnotation(new ConvertToHtmlCodesAnnotation());
              parameters.setUseCodeFinder(true);
            }
          });
    }
  }

  @Override
  public Event next() {
    Event next = super.next();

    if (next.isTextUnit()) {
      ITextUnit textUnit = next.getTextUnit();
      textUnit.setName(removeKeySuffixIfMatch(textUnit.getName()));
    }

    return next;
  }

  @Override
  public void handleComment(String c) {
    comment = c;
    super.handleComment(c);
  }

  @Override
  public void handleKey(String key, JsonValueTypes valueType, JsonKeyTypes keyType) {
    currentKeyName = key;
    super.handleKey(key, valueType, keyType);
  }

  @Override
  public void handleValue(String value, JsonValueTypes valueType) {
    extractNoteIfMatch(value);
    extractUsageIfMatch(value);
    super.handleValue(value, valueType);
    processComment();
  }

  void extractUsageIfMatch(String value) {
    if (usagesKeyPattern != null) {
      Matcher m = usagesKeyPattern.matcher(currentKeyName);

      if (m.matches()) {
        logger.debug("key matches usagesKeyPattern, add the value as usage");

        if (usagesAnnotation == null || usagesKeepOrReplace) {
          usagesAnnotation = new UsagesAnnotation(new HashSet<>());
        }

        usagesAnnotation.getUsages().add(value);
      }
    }
  }

  void addNote(String value) {
    Note note = new Note(value);
    note.setFrom(currentKeyName);
    note.setAnnotates(Note.Annotates.SOURCE);

    if (noteAnnotation == null || noteKeepOrReplace) {
      logger.debug("create the note annotation");
      noteAnnotation = new NoteAnnotation();
    }

    noteAnnotation.add(note);
  }

  String removeKeySuffixIfMatch(String key) {
    if (removeKeySuffix != null) {
      if (key.endsWith(removeKeySuffix)) {
        key = key.substring(0, key.length() - removeKeySuffix.length());
      }
    }
    return key;
  }

  void extractNoteIfMatch(String value) {
    if (noteKeyPattern != null) {
      Matcher m = noteKeyPattern.matcher(currentKeyName);

      if (m.matches()) {
        logger.debug("key matches noteKeyPattern, add the value as note");
        addNote(value);
      }
    }
  }

  void processComment() {
    if (comment != null) {
      ITextUnit textUnit = getEventTextUnit();
      if (textUnit != null) {
        String note = comment.replace("//", "").trim();
        addNote(note);
        textUnit.setAnnotation(noteAnnotation);
        noteAnnotation = null;
      }
      comment = null;
    }
  }

  @Override
  public void handleObjectEnd() {

    if (noteAnnotation != null || usagesAnnotation != null) {

      ITextUnit textUnit = getEventTextUnit();

      if (textUnit != null) {
        if (noteAnnotation != null) {
          logger.debug("Set note on text unit with name: {}", textUnit.getName());
          textUnit.setAnnotation(noteAnnotation);
        }

        if (usagesAnnotation != null) {
          logger.debug("Set usages on text unit with name: {}", textUnit.getName());
          textUnit.setAnnotation(usagesAnnotation);
        }
      } else {
        logger.debug("Annotation but no text unit. Skip them");
      }
    }

    logger.debug("Reset the noteAnnotation and Usage Annotation if not using keepOrReplace option");
    if (!noteKeepOrReplace) {
      noteAnnotation = null;
    }

    if (!usagesKeepOrReplace) {
      usagesAnnotation = null;
    }

    super.handleObjectEnd();
  }

  private ITextUnit getEventTextUnit() {
    ITextUnit textUnit = null;
    JsonEventBuilder eventBuilder = null;
    List<Event> events = null;

    try {
      eventBuilder = (JsonEventBuilder) FieldUtils.readField(this, "eventBuilder", true);
      events = (List<Event>) FieldUtils.readField(eventBuilder, "filterEvents", true);
    } catch (Exception e) {
      logger.error("Can't get the eventBuilder", eventBuilder);
      throw new RuntimeException(e);
    }

    Optional<Event> event = Lists.reverse(events).stream().filter(Event::isTextUnit).findFirst();

    if (event.isPresent()) {
      textUnit = event.get().getTextUnit();
    }
    return textUnit;
  }
}
