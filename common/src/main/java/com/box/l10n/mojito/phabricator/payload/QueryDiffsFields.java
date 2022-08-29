package com.box.l10n.mojito.phabricator.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryDiffsFields {

  @JsonProperty("revisionID")
  String revisionId;

  String sourceControlBaseRevision;

  String authorEmail;

  public String getRevisionId() {
    return revisionId;
  }

  public void setRevisionId(String revisionId) {
    this.revisionId = revisionId;
  }

  public String getSourceControlBaseRevision() {
    return sourceControlBaseRevision;
  }

  public void setSourceControlBaseRevision(String sourceControlBaseRevision) {
    this.sourceControlBaseRevision = sourceControlBaseRevision;
  }

  public String getAuthorEmail() {
    return authorEmail;
  }

  public void setAuthorEmail(String authorEmail) {
    this.authorEmail = authorEmail;
  }
}
