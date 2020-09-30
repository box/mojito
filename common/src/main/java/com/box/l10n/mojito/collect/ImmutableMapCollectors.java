package com.box.l10n.mojito.collect;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.stream.Collector;

public class ImmutableMapCollectors {
    public static <K,V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> MapEntriesToImmutableMap() {
        return ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
