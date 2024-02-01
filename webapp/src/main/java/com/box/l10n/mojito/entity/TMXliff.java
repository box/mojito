package com.box.l10n.mojito.entity;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author jyi
 */
@Entity
@Table(name = "tm_xliff")
public class TMXliff extends AuditableEntity {

  @Basic(optional = false)
  @ManyToOne
  @JoinColumn(
      name = "asset_id",
      foreignKey = @ForeignKey(name = "FK__TM_XLIFF__ASSET__ID"),
      nullable = false)
  private Asset asset;

  @OneToOne
  @JoinColumn(name = "locale_id", foreignKey = @ForeignKey(name = "FK__TM_XLIFF__LOCALE__ID"))
  private Locale locale;

  @OneToOne
  @Basic(optional = true)
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
