package com.box.l10n.mojito.utils;


import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class Optionals {

    public static <T, R> Optional<R> or(T input, Function<T, Optional<R>>... functions) {
        Stream<Function<T, Optional<R>>> stream = Stream.of(functions);
        return stream.map((f) -> f.apply(input)).filter(Optional::isPresent).map(Optional::get).findFirst();
    }
}
