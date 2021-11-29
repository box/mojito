package com.box.l10n.mojito.collect;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

public class ImmutableMapCollectors {
    public static <K,V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> mapEntriesToImmutableMap() {
        return ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    public static <K,V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> mapEntriesToImmutableMap(BinaryOperator<V> mergeFunction) {
        return ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction);
    }
}
