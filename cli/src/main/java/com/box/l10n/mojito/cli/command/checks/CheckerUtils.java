package com.box.l10n.mojito.cli.command.checks;

import com.ibm.icu.text.BreakIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckerUtils {

    public static List<String> getWordsInString(String str) {
        List<String> words = new ArrayList<>();
        BreakIterator wordBreakIterator = BreakIterator.getWordInstance(Locale.ENGLISH);
        wordBreakIterator.setText(str);
        int start = wordBreakIterator.first();
        for (int end = wordBreakIterator.next(); end != BreakIterator.DONE; start = end, end = wordBreakIterator.next()) {
            if (wordBreakIterator.getRuleStatus() != BreakIterator.WORD_NONE) {
                words.add(str.substring(start, end));
            }
        }
        return words;
    }
}
