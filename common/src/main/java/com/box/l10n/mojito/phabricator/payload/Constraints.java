package com.box.l10n.mojito.phabricator.payload;

import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

@JsonRootName("constraints")
public class Constraints {
    List<String> phids;

    public List<String> getPhids() {
        return phids;
    }

    public void setPhids(List<String> phids) {
        this.phids = phids;
    }
}
