package com.box.l10n.mojito.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "tm_text_unit_to_branch",
    indexes = {
      @Index(name = "I__TEXT_UNIT_TO_BRANCH__TEXT_UNIT_ID", columnList = "tm_text_unit_id")
    })
public class TMTextUnitToBranch extends BaseEntity {
  @ManyToOne
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_TO_BRANCH__TM_TEXT_UNIT_ID"))
  private TMTextUnit tmTextUnit;

  @ManyToOne
  @JoinColumn(
      name = "branch_id",
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_TO_BRANCH__BRANCH_ID"))
  private Branch branch;

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }
}
