package com.box.l10n.mojito.ltm;

import com.google.common.base.Preconditions;
import com.ibm.icu.text.MessageFormat;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * {@link MessageFormat}s are expensive to compute so they are cached.
 * <p>
 * This is class is thread safe. {@link MessageFormat} instances are not safe so they are cached in a {@link Map} that
 * is stored in a {@link ThreadLocal}.
 * <p>
 *
 * @param <T> Type of the keys. Can be used to ensure proper keys are used.
 */
public class Translator<T> {

    ThreadLocal<Map<String, MessageFormat>> messageFormatThreadLocal = ThreadLocal.withInitial(() -> new HashMap<>());

    ResourceBundle resourceBundle;

    public Translator(ResourceBundle resourceBundle) {
        this.resourceBundle = Preconditions.checkNotNull(resourceBundle);
    }

    public static Map.Entry<String, Object> toEntry(String string, Object object) {
        return new AbstractMap.SimpleImmutableEntry<>(string, object);
    }

    public static Map<String, Object> fromEntries(Map.Entry<String, Object>... entries) {
        return Arrays.stream(entries).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public String get(T key) {
        return resourceBundle.getString(Preconditions.checkNotNull(key.toString()));
    }

    public String get(T key, String p1, Object v1) {
        return get(key, fromEntries(toEntry(p1, v1)));
    }

    public String get(T key, String p1, Object v1, String p2, Object v2) {
        return get(key, fromEntries(toEntry(p1, v1), toEntry(p2, v2)));
    }

    public String get(T key, String p1, Object v1, String p2, Object v2, String p3, Object v3) {
        return get(key, fromEntries(toEntry(p1, v1), toEntry(p2, v2), toEntry(p3, v3)));
    }

    public String get(T key, String p1, Object v1, String p2, Object v2, String p3, Object v3, String p4, Object v4) {
        return get(key, fromEntries(toEntry(p1, v1), toEntry(p2, v2), toEntry(p3, v3), toEntry(p4, v4)));
    }

    public String get(T key, String p1, Object v1, String p2, Object v2, String p3, Object v3, String p4, Object v4, String p5, Object v5) {
        return get(key, fromEntries(toEntry(p1, v1), toEntry(p2, v2), toEntry(p3, v3), toEntry(p4, v4), toEntry(p5, v5)));
    }

    public String get(T key, Map<String, Object> params) {
        String pattern = get(key);
        if (!Preconditions.checkNotNull(params).isEmpty()) {
            MessageFormat messageFormat = getMessageFormat(pattern);
            pattern = messageFormat.format(params);
        }
        return pattern;
    }

    MessageFormat getMessageFormat(String pattern) {
        return messageFormatThreadLocal.get().computeIfAbsent(pattern, p -> new MessageFormat(p, resourceBundle.getLocale()));
    }
}