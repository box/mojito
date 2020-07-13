package com.box.l10n.mojito.ltm.merger;

public class Match {
    BranchStateTextUnit source;
    BranchStateTextUnit match;
    boolean uniqueMatch;
    boolean translationNeededIfUniqueMatch;

    public Match(BranchStateTextUnit source, BranchStateTextUnit match, boolean uniqueMatch, boolean translationNeededIfUniqueMatch) {
        this.source = source;
        this.match = match;
        this.uniqueMatch = uniqueMatch;
        this.translationNeededIfUniqueMatch = translationNeededIfUniqueMatch;
    }

    public BranchStateTextUnit getMatch() {
        return match;
    }

    public boolean isUniqueMatch() {
        return uniqueMatch;
    }

    public boolean isTranslationNeededIfUniqueMatch() {
        return translationNeededIfUniqueMatch;
    }

    public BranchStateTextUnit getSource() {
        return source;
    }
}
