package com.box.l10n.mojito.ltm.merger;

import java.util.Map;

public class BranchState {

    Branch branch;

    Map<String, BranchStateTextUnit> md5ToCurrentStateTextUnits;

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Map<String, BranchStateTextUnit> getMd5ToCurrentStateTextUnits() {
        return md5ToCurrentStateTextUnits;
    }

    public void setMd5ToCurrentStateTextUnits(Map<String, BranchStateTextUnit> md5ToCurrentStateTextUnits) {
        this.md5ToCurrentStateTextUnits = md5ToCurrentStateTextUnits;
    }
}
