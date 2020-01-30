package com.box.l10n.mojito.phabricator.conduit.payload;

public class BuildSearchFields {
    String buildablePHID;

    public String getBuildablePHID() {
        return buildablePHID;
    }

    public void setBuildablePHID(String buildablePHID) {
        this.buildablePHID = buildablePHID;
    }
}
