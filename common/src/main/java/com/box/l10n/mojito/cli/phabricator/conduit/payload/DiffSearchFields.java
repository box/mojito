package com.box.l10n.mojito.cli.phabricator.conduit.payload;

public class DiffSearchFields {

    String revisionPHID;

    public String getRevisionPHID() {
        return revisionPHID;
    }

    public void setRevisionPHID(String revisionPHID) {
        this.revisionPHID = revisionPHID;
    }
}
