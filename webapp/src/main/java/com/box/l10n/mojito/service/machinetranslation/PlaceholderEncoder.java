package com.box.l10n.mojito.service.machinetranslation;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Encodes placeholders to ensure that they will be protected during the (machine) translation process.
 * The placeholders patterns are defined in {@Link PlaceholderPatternType}.
 * Any string matching that pattern will get wrapped into a <span translate="no">pattern</span> block.
 *
 * @author garion
 */
@Component
public class PlaceholderEncoder {

    public Pattern getPlaceholderPattern() {
        String placeholderRegex = Arrays.stream(PlaceholderPatternType.values())
                .map(PlaceholderPatternType::getValue)
                .collect(Collectors.joining("|"));

        return Pattern.compile(placeholderRegex);
    }

    public List<String> encode(List<String> textSources) {
        return textSources.stream()
                .map(this::encode)
                .collect(Collectors.toList());
    }

    public String encode(String textSource) {
        Matcher matcher = getPlaceholderPattern().matcher(textSource);

        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            matcher.appendReplacement(
                    stringBuffer,
                    Matcher.quoteReplacement("<span translate=\"no\">" + match + "</span>"));
        }
        matcher.appendTail(stringBuffer);

        return stringBuffer.toString();
    }

    public List<String> decode(List<String> textSources) {
        return textSources.stream()
                .map(this::decode)
                .collect(Collectors.toList());
    }

    public String decode(String textSource) {
        // Note: ensure that matching is enabled across lines as well by using (?s), for content containing end lines.
        return textSource.replaceAll("<span translate=\"no\">(?s)(.*?)</span>", "$1");
    }
}
