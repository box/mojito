package com.box.l10n.mojito.phabricator.payload;

public class ObjectResult extends ResultWithError {
    Object result;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
