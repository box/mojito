package com.box.l10n.mojito.cli.filefinder.locale;

/**
 * Generic {@link LocaleType} implementation, accepts any string as locale.
 * The target locale can be anything but the source locale.
 *
 * @author jaurambault
 */
public class AnyLocaleTargetNotSourceType extends LocaleType {

    @Override
    public String getTargetLocaleRegex() {
        return "(?!" + getSourceLocale() + ")[^/]+?|" + getSourceLocale() + "-[^/]+?";
    }

}
