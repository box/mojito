package com.box.l10n.mojito.ltm;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Factory for {@link Translator}
 */
public class Translators {

    static <T> Translator get(String baseName, String languageTag) {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, Locale.forLanguageTag(languageTag), new UTF8ResourceBundleControl());
        Translator<T> translator = new Translator(bundle);
        return translator;
    }

}
