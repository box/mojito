package com.box.l10n.mojito.service.drop.exporter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Date;

/**
 * @author jaurambault
 */
@JsonPropertyOrder(alphabetic = true)
public class BoxDropExporterConfig {

  String dropFolderId;
  String localizedFolderId;
  String importedFolderId;
  String sourceFolderId;
  String quotesFolderId;
  String queriesFolderId;
  Date uploadDate;

  public String getDropFolderId() {
    return dropFolderId;
  }

  public void setDropFolderId(String dropFolderId) {
    this.dropFolderId = dropFolderId;
  }

  public String getLocalizedFolderId() {
    return localizedFolderId;
  }

  public void setLocalizedFolderId(String localizedFolderId) {
    this.localizedFolderId = localizedFolderId;
  }

  public String getImportedFolderId() {
    return importedFolderId;
  }

  public void setImportedFolderId(String importedFolderId) {
    this.importedFolderId = importedFolderId;
  }

  public String getSourceFolderId() {
    return sourceFolderId;
  }

  public void setSourceFolderId(String sourceFolderId) {
    this.sourceFolderId = sourceFolderId;
  }

  public String getQuotesFolderId() {
    return quotesFolderId;
  }

  public void setQuotesFolderId(String quotesFolderId) {
    this.quotesFolderId = quotesFolderId;
  }

  public String getQueriesFolderId() {
    return queriesFolderId;
  }

  public void setQueriesFolderId(String queriesFolderId) {
    this.queriesFolderId = queriesFolderId;
  }

  public Date getUploadDate() {
    return uploadDate;
  }

  public void setUploadDate(Date uploadDate) {
    this.uploadDate = uploadDate;
  }
}
