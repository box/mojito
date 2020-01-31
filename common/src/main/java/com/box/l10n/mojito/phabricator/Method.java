package com.box.l10n.mojito.phabricator;

public enum Method {

    HARBORMASTER_TARGET_SEARCH("harbormaster.target.search"),
    HARBORMASTER_BUILD_SEARCH("harbormaster.build.search"),
    HARBORMASTER_BUILDABLE_SEARCH("harbormaster.buildable.search"),

    DIFFERENTIAL_DIFF_SEARCH("differential.diff.search"),
    DIFFERENTIAL_REVISION_SEARCH("differential.revision.search"),
    DIFFERENTIAL_REVISION_EDIT("differential.revision.edit");

    String method;

    Method(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}
