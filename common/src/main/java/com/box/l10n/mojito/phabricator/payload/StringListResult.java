package com.box.l10n.mojito.phabricator.payload;

import java.util.List;

public class StringListResult extends ResultWithError {

    List<String> result;

    public List<String> getResult() {
        return result;
    }

    public void setResult(List<String> result) {
        this.result = result;
    }
}
