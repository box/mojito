package com.box.l10n.mojito.utils;

import java.util.function.BinaryOperator;

public class MergeFunctions {

    public static <T> BinaryOperator<T> keepLast() {
        return (current, newEntry) -> newEntry;
    }
}
