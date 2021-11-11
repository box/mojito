package com.box.l10n.mojito.cli.command.checks;

import java.util.List;

class GlossarySearchResult {
    List<String> failures;
    boolean isSuccess;
    boolean isMajorFailure;
    final String source;

    public GlossarySearchResult(String source) {
        this.source = source;
        this.isSuccess = true;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean isMajorFailure() {
        return isMajorFailure;
    }

    public List<String> getFailures() {
        return failures;
    }

    public String getSource() {
        return source;
    }
}
