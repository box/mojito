package com.box.l10n.mojito.ltm.merger;

import java.util.Date;
import java.util.Objects;

public class Branch {
    String name;
    Date createdAt;

    public Branch() {
    }

    public Branch(String name, Date createdAt) {
        this.name = name;
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Branch branch = (Branch) o;
        return Objects.equals(name, branch.name) &&
                Objects.equals(createdAt, branch.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, createdAt);
    }

    @Override
    public String toString() {
        return "Branch{" +
                "name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
