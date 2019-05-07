package com.box.l10n.mojito.utils;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

public class Predicates {

    public static <T> Predicate<T> not(Predicate<T> t) {
        return t.negate();
    }


    public static <T> Predicate<T> logIfFalse(Predicate<T> predicate, Logger logger, String message, Function<T, ?>... forArguments) {
        return input -> {
            boolean test = predicate.test(input);
            if (!test) {
                Object[] arguments = Arrays.stream(forArguments).map(f -> f.apply(input)).toArray();
                logger.debug(message, arguments);
            }
            return test;
        };
    }
}
