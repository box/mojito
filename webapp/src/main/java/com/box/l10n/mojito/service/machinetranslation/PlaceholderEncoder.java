package com.box.l10n.mojito.service.machinetranslation;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Encodes placeholders to ensure that they will be protected during the (machine) translation
 * process. The placeholders patterns are defined in {@Link PlaceholderPatternType}. Any string
 * matching that pattern will get wrapped into a <span translate="no">pattern</span> block.
 *
 * @author garion
 */
@Component
public class PlaceholderEncoder {
  Pattern placeholderPattern;

  // Note: ensure that matching is enabled across lines as well by using (?s), for content
  // containing end lines.
  Pattern translateInstructionSpanPattern =
      Pattern.compile("<span translate=\"no\">(?s)(.*?)</span>");

  public PlaceholderEncoder() {
    String placeholderRegex =
        Arrays.stream(PlaceholderPatternType.values())
            .map(PlaceholderPatternType::getValue)
            .collect(Collectors.joining("|"));

    placeholderPattern = Pattern.compile(placeholderRegex);
  }

  public Pattern getPlaceholderPattern() {
    return placeholderPattern;
  }

  public List<String> encode(List<String> textSources) {
    return textSources.stream().map(this::encode).collect(Collectors.toList());
  }

  public String encode(String textSource) {
    Matcher matcher = placeholderPattern.matcher(textSource);

    StringBuffer stringBuffer = new StringBuffer();
    while (matcher.find()) {
      String match = matcher.group();
      matcher.appendReplacement(
          stringBuffer, Matcher.quoteReplacement("<span translate=\"no\">" + match + "</span>"));
    }
    matcher.appendTail(stringBuffer);

    return stringBuffer.toString();
  }

  public List<String> decode(List<String> textSources) {
    return textSources.stream().map(this::decode).collect(Collectors.toList());
  }

  public String decode(String textSource) {
    return translateInstructionSpanPattern.matcher(textSource).replaceAll("$1");
  }
}
