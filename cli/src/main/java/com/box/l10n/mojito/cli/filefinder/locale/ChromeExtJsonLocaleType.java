package com.box.l10n.mojito.cli.filefinder.locale;

/**
 * {@link LocaleType} implementation for Chrome extension. Use "_" in locale names.
 *
 * @author jaurambault
 */
public class ChromeExtJsonLocaleType extends AnyLocaleTargetNotSourceType {
    public String getTargetLocaleRepresentation(String targetLocale) {
        return targetLocale.replace("-", "_");
    }
}
