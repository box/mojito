package com.box.l10n.mojito.cli.command.checks;

import com.ibm.icu.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
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

  /**
   * Pattern matching for values inside curly braces, which usually are placeholders.
   *
   * <p>"<" matches an opening curly brace, "[^}]*" matches one or more characters that are not a
   * curly brace and "}" matches a closing curly brace
   */
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{[^\\}]*\\}");

  /**
   * Pattern to match simplified email addresses.
   *
   * <p>[a-zA-Z0-9._%+-]+ : Matches one or more (+) of the allowed characters (a-z, A-Z, 0-9, ., _,
   * %, +, -) in the local-part (before the @). @ : Matches the @ character exactly. [a-zA-Z0-9.-]+
   * : Matches one or more (+) of the allowed characters (a-z, A-Z, 0-9, ., -) in the domain part
   * (between the @ and the . for the TLD). \. : Matches the . character exactly (escaped because .
   * is a special character in regex). [a-zA-Z]{2,6} : Matches between 2 and 6 of the allowed
   * characters (a-z, A-Z) in the TLD (top-level domain, like .com or .org).
   */
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");

  private static final Pattern WISHED_HTML_ATTRIBUTES_VALUE_PATTERN;

  private static final List<String> WISHED_HTML_ATTRIBUTES_FOR_SPELL_CHECK =
      Arrays.asList("title=", "alt=", "value=", "placeholder=");

  /**
   * Initializes regex pattern for the wished_html_attributes_value_pattern. The regex matches html
   * tags which contains one of the attributes from the WISHED_HTML_ATTRIBUTES_FOR_SPELL_CHECK list.
   */
  static {
    String pattern =
        "<[^>]+(?:" + String.join("|", WISHED_HTML_ATTRIBUTES_FOR_SPELL_CHECK) + ")\"(.*?)\".*?>";

    WISHED_HTML_ATTRIBUTES_VALUE_PATTERN = Pattern.compile(pattern);
  }

  public static List<String> getWordsInString(String str) {
    if (StringUtils.isEmpty(str)) {
      return new ArrayList<>();
    }
    List<String> words = new ArrayList<>();
    BreakIterator wordBreakIterator = BreakIterator.getWordInstance(Locale.ENGLISH);
    str = removeHtmlTagsAndKeepWishedAttribute(str);
    str = str.replaceAll(PLACEHOLDER_PATTERN.pattern(), "");
    str = str.replaceAll(EMAIL_PATTERN.pattern(), "");
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

  /**
   * Removes all the html tags present in a string, but keeping a wished attribute value (attached
   * at the end of the string) when applicable.
   *
   * @param str A string which may or may not contain html tags.
   * @return 1 - The same input string if there are no html tags present; or 2 - The input string
   *     without any html tag content; or 3 - The input string removing the html tags, but attaching
   *     the value of the attribute (at the end) if matches any of the attributes from the
   *     WISHED_HTML_ATTRIBUTES_FOR_SPELL_CHECK list.
   */
  protected static String removeHtmlTagsAndKeepWishedAttribute(String str) {
    if (StringUtils.isEmpty(str)) {
      return str;
    }

    Matcher matcher = WISHED_HTML_ATTRIBUTES_VALUE_PATTERN.matcher(str);
    String wishedHtmlAttValue = null;
    if (matcher.find()) {
      wishedHtmlAttValue = matcher.group(1);
    }
    str = str.replaceAll(HTML_TAGS_PATTERN.pattern(), " ");

    return wishedHtmlAttValue != null ? str + " " + wishedHtmlAttValue : str;
  }
}
