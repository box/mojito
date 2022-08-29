package com.box.l10n.mojito.phabricator.payload;

public class RevisionSearchFields {

  String revisionPHID;

  String testPlan;

  public String getRevisionPHID() {
    return revisionPHID;
  }

  public void setRevisionPHID(String revisionPHID) {
    this.revisionPHID = revisionPHID;
  }

  public String getTestPlan() {
    return testPlan;
  }

  public void setTestPlan(String testPlan) {
    this.testPlan = testPlan;
  }
}
