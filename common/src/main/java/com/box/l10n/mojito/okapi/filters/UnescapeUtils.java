package com.box.l10n.mojito.okapi.filters;

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

  private static final Pattern ESCAPED_CARIAGE_RETURN = Pattern.compile("\\\\r");
  private static final Pattern ESCAPED_LINE_FEED = Pattern.compile("\\\\n");
  private static final Pattern ESCAPED_QUOTES = Pattern.compile("\\\\(\"|')");
  private static final Pattern ESCAPED_BACKQUOTES = Pattern.compile("\\\\(`)");
  private static final Pattern ESCAPED_CHARACTERS = Pattern.compile("\\\\(.)?");
  private static final Pattern ESCAPED_UNICODE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
  private static final Pattern SPACES = Pattern.compile("\\s+");
  private static final Pattern LINE_FEED = Pattern.compile("\n");

  /**
   * Unescapes line feed, cariage return, single quote and double quote
   *
   * @param text
   * @return
   */
  public String unescape(String text) {
    String unescapedText = replaceEscapedCarriageReturn(text);
    unescapedText = replaceEscapedLineFeed(unescapedText);
    unescapedText = replaceEscapedQuotes(unescapedText);
    return unescapedText;
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
