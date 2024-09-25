package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.ExtractUsagesFromTextUnitComments;
import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation;
import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation.OutputDocumentPostProcessorBase;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.regex.RegexFilter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Overrides {@link RegexFilter} to handle escape/unescape special characters
 *
 * @author jyi
 */
public class MacStringsFilter extends RegexEscapeDoubleQuoteFilter {

  public static final String FILTER_CONFIG_ID = "okf_regex@mojito";
  private static final String REMOVE_COMMENTS = "removeComment";

  @Autowired ExtractUsagesFromTextUnitComments extractUsagesFromTextUnitComments;

  boolean removeComment = false;

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public void open(RawDocument input) {
    super.open(input);
    applyFilterOptions(input);
    input.setAnnotation(
        new RemoveUntranslatedStategyAnnotation(
            RemoveUntranslatedStrategy.PLACEHOLDER_AND_POST_PROCESSING));
    input.setAnnotation(
        new OutputDocumentPostProcessingAnnotation(
            new MacStringsFilterPostProcessor(removeComment)));
  }

  void applyFilterOptions(RawDocument input) {
    FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);

    if (filterOptions != null) {
      filterOptions.getBoolean(
          REMOVE_COMMENTS,
          b -> {
            removeComment = b;
          });
    }
  }

  @Override
  public List<FilterConfiguration> getConfigurations() {
    List<FilterConfiguration> list = new ArrayList<>();
    list.add(
        new FilterConfiguration(
            getName() + "-macStrings",
            getMimeType(),
            getClass().getName(),
            "Text (Mac Strings)",
            "Configuration for Macintosh .strings files.",
            "macStrings_mojito.fprm"));
    return list;
  }

  @Override
  public Event next() {
    Event event = super.next();

    if (event.getEventType() == EventType.TEXT_UNIT) {
      TextUnit textUnit = (TextUnit) event.getTextUnit();
      extractUsagesFromTextUnitComments.addUsagesToTextUnit(textUnit);
    }

    return event;
  }

  static class MacStringsFilterPostProcessor extends OutputDocumentPostProcessorBase {

    static final Pattern COMMENT_KEY_VALUE_PATTERN =
        Pattern.compile("(?s)(/\\*.*?\\*/\\s*)?(\".*?\"\\s*=\\s*\".*?\";)");
    static final Pattern COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/");

    boolean removeComment = false;

    public MacStringsFilterPostProcessor(boolean removeComment) {
      this.removeComment = removeComment;
    }

    public String execute(String fileContent) {
      String result = fileContent;

      if (hasRemoveUntranslated()) {
        result = removeUntranslated(fileContent);
      }

      if (removeComment) {
        result = removeComments(fileContent);
      }

      return result;
    }

    public static String removeUntranslated(String fileContent) {

      Matcher matcher = COMMENT_KEY_VALUE_PATTERN.matcher(fileContent);
      StringBuilder stringBuilder = new StringBuilder();

      while (matcher.find()) {
        String entry = matcher.group();
        if (entry.contains(RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER)) {
          matcher.appendReplacement(stringBuilder, "");
        } else {
          matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(entry));
        }
      }
      matcher.appendTail(stringBuilder);

      String result = collapseBlankLines(stringBuilder.toString());
      result = ensureEndLineAsInInput(result, fileContent);

      return result;
    }

    public static String removeComments(String fileContent) {
      Matcher matcher = COMMENT_PATTERN.matcher(fileContent);
      String result = matcher.replaceAll("");
      result = collapseBlankLines(result);
      result = ensureEndLineAsInInput(result, fileContent);
      return result;
    }

    static String ensureEndLineAsInInput(String output, String input) {
      String result = output.trim();

      if (input.isBlank()) {
        return "\n".repeat(input.length()) + result;
      } else {

        int leadingNewlines = 0;
        int trailingNewlines = 0;
        int length = input.length();

        int index = 0;
        while (index < length && input.charAt(index) == '\n') {
          leadingNewlines++;
          index++;
        }

        index = length - 1;
        while (index >= 0 && input.charAt(index) == '\n') {
          trailingNewlines++;
          index--;
        }
        return "\n".repeat(leadingNewlines) + result + "\n".repeat(trailingNewlines);
      }
    }

    static String collapseBlankLines(String input) {
      String result = input;
      if (!Strings.isNullOrEmpty(input)) {
        result = input.replaceAll("(?m)^(\\s*\\n){2,}", "\n");
      }
      return result;
    }
  }
}
