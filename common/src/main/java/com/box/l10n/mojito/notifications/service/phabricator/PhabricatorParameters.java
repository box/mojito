package com.box.l10n.mojito.notifications.service.phabricator;

public enum PhabricatorParameters {

    REVISION_ID("phab_revision_id");

    private String paramKey;

    PhabricatorParameters(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getParamKey() {
        return paramKey;
    }
}
