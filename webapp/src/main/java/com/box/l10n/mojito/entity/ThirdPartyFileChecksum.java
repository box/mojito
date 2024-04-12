package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Entity that stores the checksum of a translated file downloaded via a third party sync. */
@Entity
@Table(
    name = "third_party_sync_file_checksum",
    indexes = {
      @Index(
          name = "I__TPS_FILE_CHECKSUM__REPO_ID__LOCALE_ID__FILE_NAME",
          columnList = "repository_id, locale_id, file_name",
          unique = true),
    })
public class ThirdPartyFileChecksum extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__TPS_FILE_CHECKSUM__REPO__ID"))
  private Repository repository;

  @Column(name = "file_name")
  private String fileName;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__TPS_FILE_CHECKSUM__LOCALE__ID"))
  private Locale locale;

  @Column(name = "md5")
  private String md5;

  public ThirdPartyFileChecksum() {}

  public ThirdPartyFileChecksum(Repository repository, String fileName, Locale locale, String md5) {
    this.repository = repository;
    this.fileName = fileName;
    this.locale = locale;
    this.md5 = md5;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String checksum) {
    this.md5 = checksum;
  }

  public Locale getLocale() {
    return locale;
  }

  public Repository getRepository() {
    return repository;
  }

  public String getFileName() {
    return fileName;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
}
