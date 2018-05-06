package com.box.l10n.mojito.service.tm.search;

/**
 *
 * @author jeanaurambault
 */
public class TextUnitAndWordCount {

    long textUnitCount;
    long textUnitWordCount;

    public TextUnitAndWordCount(long textUnitCount, long textUnitWordCount) {
        this.textUnitCount = textUnitCount;
        this.textUnitWordCount = textUnitWordCount;
    }

    public long getTextUnitCount() {
        return textUnitCount;
    }

    public void setTextUnitCount(long textUnitCount) {
        this.textUnitCount = textUnitCount;
    }

    public long getTextUnitWordCount() {
        return textUnitWordCount;
    }

    public void setTextUnitWordCount(long textUnitWordCount) {
        this.textUnitWordCount = textUnitWordCount;
    }

}
