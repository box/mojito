package com.box.l10n.mojito.ltm.merger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class MultiBranchStateJson {

    ImmutableList<BranchStateTextUnitJson> md5ToBranchStateTextUnits = ImmutableList.of();

    ImmutableSet<Branch> branches = ImmutableSet.of();

    public ImmutableList<BranchStateTextUnitJson> getMd5ToBranchStateTextUnits() {
        return md5ToBranchStateTextUnits;
    }

    public void setMd5ToBranchStateTextUnits(ImmutableList<BranchStateTextUnitJson> md5ToBranchStateTextUnits) {
        this.md5ToBranchStateTextUnits = md5ToBranchStateTextUnits;
    }

    public ImmutableSet<Branch> getBranches() {
        return branches;
    }

    public void setBranches(ImmutableSet<Branch> branches) {
        this.branches = branches;
    }
}
