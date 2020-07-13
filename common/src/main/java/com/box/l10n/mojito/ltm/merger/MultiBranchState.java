package com.box.l10n.mojito.ltm.merger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class MultiBranchState {

    ImmutableMap<String, BranchStateTextUnit> md5ToBranchStateTextUnits = ImmutableMap.of();

    ImmutableSet<Branch> branches = ImmutableSet.of();

    public ImmutableMap<String, BranchStateTextUnit> getMd5ToBranchStateTextUnits() {
        return md5ToBranchStateTextUnits;
    }

    public void setMd5ToBranchStateTextUnits(ImmutableMap<String, BranchStateTextUnit> md5ToBranchStateTextUnits) {
        this.md5ToBranchStateTextUnits = md5ToBranchStateTextUnits;
    }

    public ImmutableSet<Branch> getBranches() {
        return branches;
    }

    public void setBranches(ImmutableSet<Branch> branches) {
        this.branches = branches;
    }
}
