package com.box.l10n.mojito.utils;


import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Optionals {

    public static <T, R> Optional<R> or(T input, Predicate<R> filter, Function<T, Optional<R>>... functions) {
        Stream<Function<T, Optional<R>>> stream = Stream.of(functions);
        return stream.map((f) -> f.apply(input))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(filter)
                .findFirst();
    }

    public static <T> Optional<List<T>> optionalToOptionalList(Optional<T> optional) {
        return Optional.ofNullable(optional.map(Collections::singletonList).orElse(null));
    }
}
