package com.box.l10n.mojito.ltm.merger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BranchData {

    List<String> usages = new ArrayList<>();

    public List<String> getUsages() {
        return usages;
    }

    public void setUsages(List<String> usages) {
        this.usages = usages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BranchData that = (BranchData) o;
        return Objects.equals(usages, that.usages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usages);
    }
}

