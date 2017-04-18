package com.box.l10n.mojito.cli.filefinder.locale;

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
        return targetLocale.replace("-","_");
    }

}
