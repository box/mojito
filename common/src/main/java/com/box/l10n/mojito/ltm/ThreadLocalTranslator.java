package com.box.l10n.mojito.ltm;

import java.util.Map;

/**
 * ThreadLocal translator class that loads translations from a default "messages" bundle and with basic global locale
 * activation method.
 * <p>
 * If looking for a global static For services that process multiple requests in parrallel with different locales.
 */
public class ThreadLocalTranslator {
    static final String MESSAGES_BASENAME = "messages";

    static ThreadLocal<Translator> translatorThreadLocal = ThreadLocal.withInitial(() -> Translators.get(MESSAGES_BASENAME, "en"));

    public static void activateLocale(String bcp47tag) {
        translatorThreadLocal.set(Translators.get(MESSAGES_BASENAME, bcp47tag));
    }

    static Translator getTranslator() {
        return translatorThreadLocal.get();
    }

    public static String t(MyKeys key) {
        return getTranslator().get(key);
    }

    public static String t(MyKeys key, String p1, Object v1) {
        return getTranslator().get(key, p1, v1);
    }

    public static String t(MyKeys key, String p1, Object v1, String p2, Object v2) {
        return getTranslator().get(key, p1, v1, p2, v2);
    }

    public static String t(MyKeys key, String p1, Object v1, String p2, Object v2, String p3, Object v3) {
        return getTranslator().get(key, p1, v1, p2, v2, p3, v3);
    }

    public static String t(MyKeys key, Map<String, Object> params) {
        return getTranslator().get(key, params);
    }
}
