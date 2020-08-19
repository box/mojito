package com.box.l10n.mojito.ltm.merger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class MultiBranchStateJson {

    ImmutableList<BranchStateTextUnitJson> branchStateTextUnitJsons = ImmutableList.of();

    ImmutableSet<Branch> branches = ImmutableSet.of();

    public ImmutableList<BranchStateTextUnitJson> getBranchStateTextUnitJsons() {
        return branchStateTextUnitJsons;
    }

    public void setBranchStateTextUnitJsons(ImmutableList<BranchStateTextUnitJson> branchStateTextUnitJsons) {
        this.branchStateTextUnitJsons = branchStateTextUnitJsons;
    }

    public ImmutableSet<Branch> getBranches() {
        return branches;
    }

    public void setBranches(ImmutableSet<Branch> branches) {
        this.branches = branches;
    }
}
