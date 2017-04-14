package com.box.l10n.mojito.cli.filefinder.locale;

import java.util.Locale;

/**
 *
 * @author jaurambault
 */
public class POLocaleType extends LocaleType {
    
    @Override
    public String getTargetLocaleRegex() {
        return "(?!LC_MESSAGES)(?:[^/]+)";
    }

    @Override
    public String getTargetLocaleRepresentation(String targetLocale) {
        Locale forLanguageTag = Locale.forLanguageTag(targetLocale);
        return forLanguageTag.toString();
    }

}
