package com.box.l10n.mojito.service.tm;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PluralNameParser {

    public String getPrefix(String nameWithPluralForm) {
        return nameWithPluralForm.replaceAll("_(zero|one|two|few|many|other)$", "");
    }

    public String getPrefix(String name, String separator) {

        Pattern pattern = Pattern.compile("(.*)" + separator + "(zero|one|two|few|many|other)");

        return Optional.of(pattern.matcher(name))
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .orElse(name);
    }

    public String toPluralName(String prefix, String name, String separator){
        return String.format("%s%s%s", prefix, separator, name);
    }
}
