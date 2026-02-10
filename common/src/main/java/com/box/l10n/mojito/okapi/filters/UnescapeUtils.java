package com.box.l10n.mojito.okapi.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author emagalindan
 */
@Component
public class UnescapeUtils {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(UnescapeUtils.class);

  private static final Pattern ESCAPED_BACKSLASH = Pattern.compile("\\\\\\\\");
  private static final Pattern ESCAPED_CARIAGE_RETURN = Pattern.compile("\\\\r");
  private static final Pattern ESCAPED_LINE_FEED = Pattern.compile("\\\\n");
  private static final Pattern ESCAPED_QUOTES = Pattern.compile("\\\\(\"|')");
  private static final Pattern ESCAPED_BACKQUOTES = Pattern.compile("\\\\(`)");
  private static final Pattern ESCAPED_CHARACTERS = Pattern.compile("\\\\(.)?");
  private static final Pattern ESCAPED_UNICODE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
  private static final Pattern SPACES = Pattern.compile("\\s+");
  private static final Pattern LINE_FEED = Pattern.compile("\n");

  /**
   * Single-pass pattern for C-style escape sequences used in GNU PO files. Matches exactly
   * two-character sequences starting with a backslash, so "\\\\n" (4 chars) matches "\\\\" first (→
   * \), leaving "n" as a literal — not "\n" (newline).
   *
   * <p>Covers the same set as Okapi's {@code POFilter.unescape()}: {@code \\[abfnrtv"'\\]}.
   */
  private static final Pattern ESCAPE_SEQUENCE = Pattern.compile("\\\\[abfnrtv\"'\\\\]");

  /**
   * Unescapes C-style escape sequences in a single pass, following the GNU PO file format (same
   * escaping rules as C strings).
   *
   * <p>Handles: {@code \\} (backslash), {@code \n} (newline), {@code \r} (CR), {@code \t} (tab),
   * {@code \"} (quote), {@code \'} (single quote), {@code \a} (bell), {@code \b} (backspace),
   * {@code \f} (form feed), {@code \v} (vertical tab).
   *
   * <p>A single-pass approach is required because sequential replacement can corrupt strings
   * containing ambiguous sequences like "\\\\n" (escaped-backslash followed by literal 'n'). With
   * sequential replacement, this would be incorrectly decoded as a newline character.
   *
   * @param text the escaped text
   * @return the unescaped text
   */
  public String unescape(String text) {
    Matcher matcher = ESCAPE_SEQUENCE.matcher(text);
    StringBuilder sb = new StringBuilder(text.length());
    while (matcher.find()) {
      String match = matcher.group();
      String replacement;
      switch (match.charAt(1)) {
        case '\\':
          replacement = "\\";
          break;
        case 'a':
          replacement = "\u0007"; // bell
          break;
        case 'b':
          replacement = "\b"; // backspace
          break;
        case 'f':
          replacement = "\f"; // form feed
          break;
        case 'n':
          replacement = "\n";
          break;
        case 'r':
          replacement = "\r";
          break;
        case 't':
          replacement = "\t";
          break;
        case 'v':
          replacement = "\u000B"; // vertical tab
          break;
        case '"':
          replacement = "\"";
          break;
        case '\'':
          replacement = "'";
          break;
        default:
          replacement = match;
          break;
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Replaces \\\\ with \\
   *
   * @param text
   * @return
   */
  String replaceEscapedBackslash(String text) {
    return ESCAPED_BACKSLASH.matcher(text).replaceAll("\\\\");
  }

  String replaceEscapedCarriageReturn(String text) {
    return ESCAPED_CARIAGE_RETURN.matcher(text).replaceAll("\r");
  }

  String replaceEscapedLineFeed(String text) {
    return ESCAPED_LINE_FEED.matcher(text).replaceAll("\n");
  }

  /**
   * Replaces \' and \" with ' or "
   *
   * @param text
   * @return
   */
  String replaceEscapedQuotes(String text) {
    return ESCAPED_QUOTES.matcher(text).replaceAll("$1");
  }

  /**
   * Replaces \` with `
   *
   * @param text
   * @return
   */
  String replaceEscapedBackquotes(String text) {
    return ESCAPED_BACKQUOTES.matcher(text).replaceAll("$1");
  }

  /**
   * Replace unicode escape character of the form \\uXXXX.
   *
   * <p>Must be call before calling other method that would unescape the "u" letter like {@link
   * #replaceEscapedCharacters(String)} (String)}
   *
   * @param text
   * @return
   */
  String replaceEscapedUnicode(String text) {
    return ESCAPED_UNICODE
        .matcher(text)
        .replaceAll(match -> new String(Character.toChars(Integer.parseInt(match.group(1), 16))));
  }

  /**
   * Replace other escape character with the character itself.
   *
   * <p>Must be call after replacing escape sequence that need a different treatment like {@link
   * #replaceEscapedLineFeed(String)}
   *
   * @param text
   * @return
   */
  String replaceEscapedCharacters(String text) {
    return ESCAPED_CHARACTERS.matcher(text).replaceAll("$1");
  }

  /**
   * Collapse multiple spaces into a single space.
   *
   * @param text
   * @return
   */
  String collapseSpaces(String text) {
    return SPACES.matcher(text).replaceAll(" ");
  }

  String replaceLineFeedWithSpace(String text) {
    return LINE_FEED.matcher(text).replaceAll(" ");
  }
}
