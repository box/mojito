package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that there are the same HTML tags in the source and target content. It checks all
 * HTML-style elements within the string - eg. <b>, <i>, <u>, <span>, <div>, etc. See
 * https://developer.android.com/guide/topics/resources/string-resource.html#FormattingAndStyling
 *
 * @author jyi
 */
public class HtmlTagIntegrityChecker extends RegexIntegrityChecker {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(HtmlTagIntegrityChecker.class);

  @Override
  public String getRegex() {
    return "(<[a-zA-Z][\\w-]*(\\s+\\w+(\\s*=\\s*('([^']*?)'|\"([^\"]*?)\"))?)*\\s*/?>|<\\/[a-zA-Z][\\w-]*>)";
  }

  @Override
  public void check(String sourceContent, String targetContent) throws IntegrityCheckException {
    logger.debug("Get Html tags of the target");
    List<String> targetHtmlTags = getHtmlTags(targetContent);
    logger.debug("Target Html tags: {}", targetHtmlTags);

    logger.debug("Get Html tags of the source");
    List<String> sourceHtmlTags = getHtmlTags(sourceContent);
    logger.debug("Source Html tags: {}", sourceHtmlTags);

    Map<String, Long> sourceTagCount =
        sourceHtmlTags.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    Map<String, Long> targetTagCount =
        targetHtmlTags.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    logger.debug("Make sure the target has the same Html tags as the source");
    if (!sourceTagCount.equals(targetTagCount)) {
      StringBuilder differences = new StringBuilder();

      Set<String> allTags = new HashSet<>();
      allTags.addAll(sourceTagCount.keySet());
      allTags.addAll(targetTagCount.keySet());

      for (String tag : allTags) {
        Long sourceCount = sourceTagCount.getOrDefault(tag, 0L);
        Long targetCount = targetTagCount.getOrDefault(tag, 0L);
        if (sourceCount > targetCount) {
          differences.append(
              String.format("Target is missing %d tag(s) '%s'%n", sourceCount - targetCount, tag));
        } else if (sourceCount < targetCount) {
          differences.append(
              String.format("Target has extra %d tag(s) '%s'%n", targetCount - sourceCount, tag));
        }
      }
      throw new HtmlTagIntegrityCheckerException(
          "HTML tag counts in source and target are different:\n" + differences.toString().trim());
    }

    logger.debug("Make sure the target tags are in valid order");
    if (!isValidTagOrder(targetHtmlTags)) {
      throw new HtmlTagIntegrityCheckerException("HTML tags in target are not in valid order");
    }

    logger.debug("Ad-hoc checks");
    checkDoubleAnnotationElements(sourceContent, targetContent);
  }

  /** Adhoc check to unblock. Eventually needs a better solution. */
  void checkDoubleAnnotationElements(String sourceContent, String targetContent) {
    String doubleAnnotationString = "<annotation <annotation";
    if (!sourceContent.contains(doubleAnnotationString)
        && targetContent.contains(doubleAnnotationString)) {
      throw new HtmlTagIntegrityCheckerException(
          "Target must not contain: '%s'".formatted(doubleAnnotationString));
    }
  }

  /**
   * Returns the HTML tags in the string.
   *
   * @param string
   * @return
   */
  List<String> getHtmlTags(String string) {
    List<String> tags = new ArrayList<>();

    if (string != null) {

      Matcher matcher = getPattern().matcher(string);

      while (matcher.find()) {
        tags.add(matcher.group());
      }
    }

    return tags;
  }

  public static boolean isValidTagOrder(List<String> tags) {

    boolean res = true;

    ArrayDeque<String> stack = new ArrayDeque<>();

    for (String tag : tags) {
      if (!tag.startsWith("</")) {
        stack.push(tag);
      } else {
        if (stack.isEmpty()
            || !stack.peek().startsWith(tag.substring(0, tag.length() - 1).replace("</", "<"))) {
          res = false;
          break;
        }
        stack.pop();
      }
    }

    return res;
  }
}
