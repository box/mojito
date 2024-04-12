package com.box.l10n.mojito.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author jyi
 */
@Entity
@Table(name = "tm_xliff")
public class TMXliff extends AuditableEntity {

  @Basic(optional = false)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "asset_id",
      foreignKey = @ForeignKey(name = "FK__TM_XLIFF__ASSET__ID"),
      nullable = false)
  private Asset asset;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "locale_id", foreignKey = @ForeignKey(name = "FK__TM_XLIFF__LOCALE__ID"))
  private Locale locale;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "export_pollable_task_id",
      foreignKey = @ForeignKey(name = "FK__TM_XLIFF__EXPORT_POLLABLE_TASK__ID"))
  PollableTask pollableTask;

  @Column(name = "content", length = Integer.MAX_VALUE)
  private String content;

  public Asset getAsset() {
    return asset;
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public PollableTask getPollableTask() {
    return pollableTask;
  }

  public void setPollableTask(PollableTask pollableTask) {
    this.pollableTask = pollableTask;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
