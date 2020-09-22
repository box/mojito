package com.box.l10n.mojito.ltm;

import java.util.Map;

/**
 * Global static translator class that loads translation from a default "messages" bundle and with basic global locale
 * activation method.
 *
 * This is generally not advised to use, unless for an app that is single threaded or mono locale. Consider at
 * least {@link ThreadLocalTranslator} or managing the translator instances yourself.
 */
public class GlobalTranslator {

    static final String MESSAGES_BASENAME = "messages";

    static Translator globalTranslator = Translators.get(MESSAGES_BASENAME, "en");

    public static void activateLocale(String bcp47tag) {
        globalTranslator = Translators.get(MESSAGES_BASENAME, bcp47tag);
    }

    public static String t(MyKeys key) {
        return globalTranslator.get(key);
    }

    public static String t(MyKeys key, String p1, Object v1) {
        return globalTranslator.get(key, p1, v1);
    }

    public static String t(MyKeys key, String p1, Object v1, String p2, Object v2) {
        return globalTranslator.get(key, p1, v1, p2, v2);
    }

    public static String t(MyKeys key, String p1, Object v1, String p2, Object v2, String p3, Object v3) {
        return globalTranslator.get(key, p1, v1, p2, v2, p3, v3);
    }

    public static String t(MyKeys key, Map<String, Object> params) {
        return globalTranslator.get(key, params);
    }
}
