package com.box.l10n.mojito.utils;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;

import java.util.List;
import java.util.function.BinaryOperator;

public class MergeFunctions {

    public static <T> BinaryOperator<T> keepLast() {
        return (current, newEntry) -> newEntry;
    }
}
