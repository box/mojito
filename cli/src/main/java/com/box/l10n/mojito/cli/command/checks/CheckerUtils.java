package com.box.l10n.mojito.cli.command.checks;

import com.ibm.icu.text.BreakIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class CheckerUtils {

  /**
   * Pattern matching for html tags.
   *
   * <p>"<" matches an opening angle bracket, "[^>]+" matches one or more characters that are not a
   * closing angle bracket and ">" matches a closing angle bracket
   */
  private static final Pattern HTML_TAGS_PATTERN = Pattern.compile("<[^>]+>");

  public static List<String> getWordsInString(String str) {
    if (StringUtils.isEmpty(str)) {
      return new ArrayList<>();
    }
    List<String> words = new ArrayList<>();
    BreakIterator wordBreakIterator = BreakIterator.getWordInstance(Locale.ENGLISH);
    str = str.replaceAll(HTML_TAGS_PATTERN.pattern(), "");
    wordBreakIterator.setText(str);
    int start = wordBreakIterator.first();
    for (int end = wordBreakIterator.next();
        end != BreakIterator.DONE;
        start = end, end = wordBreakIterator.next()) {
      if (wordBreakIterator.getRuleStatus() != BreakIterator.WORD_NONE) {
        words.add(str.substring(start, end));
      }
    }
    return words;
  }
}
