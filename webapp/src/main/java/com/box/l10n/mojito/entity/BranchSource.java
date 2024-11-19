package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "branch_source",
    indexes = {@Index(name = "I__BRANCH_SOURCE__BRANCH_ID", columnList = "branch_id")})
public class BranchSource extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "FK__BRANCH_SOURCE__BRANCH_ID"))
  private Branch branch;

  @Column(name = "url")
  private String url;

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
