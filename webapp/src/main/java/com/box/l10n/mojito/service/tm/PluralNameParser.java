package com.box.l10n.mojito.service.tm;

import org.springframework.stereotype.Component;

@Component
public class PluralNameParser {

    public String getPrefix(String nameWithPluralForm) {
        return nameWithPluralForm.replaceAll("_(zero|one|two|few|many|other)$", "");
    }
}
