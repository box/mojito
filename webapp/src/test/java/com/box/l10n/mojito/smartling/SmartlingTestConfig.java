package com.box.l10n.mojito.smartling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("test.l10n.smartling")
public class SmartlingTestConfig {
  public String projectId = null;
  public String fileUri = null;

  public String accountId = null;

  public String glossaryId = null;

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getFileUri() {
    return fileUri;
  }

  public void setFileUri(String fileUri) {
    this.fileUri = fileUri;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getGlossaryId() {
    return glossaryId;
  }

  public void setGlossaryId(String glossaryId) {
    this.glossaryId = glossaryId;
  }
}
