package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.drop.exporter.DropExporterType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import java.time.ZonedDateTime;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedBy;

/**
 * Entity that contains a Drop and the related TranslationKit associated with the drop
 *
 * @author wyau
 */
@Entity
@Table(name = "`drop`")
public class Drop extends AuditableEntity {

  @Column(name = "drop_exporter_type")
  @Enumerated(EnumType.STRING)
  DropExporterType dropExporterType;

  @Column(name = "name")
  @JsonView(View.IdAndName.class)
  String name;

  @Column(name = "drop_exporter_config", length = Integer.MAX_VALUE)
  @JsonView(View.DropSummary.class)
  String dropExporterConfig;

  @OneToOne
  @Basic(optional = true)
  @JoinColumn(
      name = "import_pollable_task_id",
      foreignKey = @ForeignKey(name = "FK__DROP__IMPORT_POLLABLE_TASK__ID"))
  @JsonView(View.DropSummary.class)
  PollableTask importPollableTask;

  @OneToOne
  @Basic(optional = true)
  @JoinColumn(
      name = "export_pollable_task_id",
      foreignKey = @ForeignKey(name = "FK__DROP__EXPORT_POLLABLE_TASK__ID"))
  @JsonView(View.DropSummary.class)
  PollableTask exportPollableTask;

  @OneToMany(mappedBy = "drop")
  @JsonView(View.DropSummary.class)
  private Set<TranslationKit> translationKits;

  @ManyToOne
  @JoinColumn(name = "repository_id", foreignKey = @ForeignKey(name = "FK__DROP__REPOSITORY__ID"))
  @JsonView(View.DropSummary.class)
  private Repository repository;

  /**
   * For now we keep track of the latest import only, actually import will be additive if we track
   * the status only in one place
   */
  @Column(name = "last_imported_date")
  @JsonView(View.DropSummary.class)
  protected ZonedDateTime lastImportedDate;

  /**
   * To mark a Drop as canceled so it can be hidden in a dashboard. This shouldn't prevent to
   * perform other regular action (like import) but is just for reporting purpose.
   */
  @Column(name = "canceled")
  @JsonView(View.DropSummary.class)
  protected Boolean canceled;

  @CreatedBy
  @ManyToOne
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__DROP__USER__ID"))
  @JsonView(View.DropSummary.class)
  protected User createdByUser;

  @Column(name = "exportFailed")
  @JsonView(View.DropSummary.class)
  protected Boolean exportFailed;

  @Column(name = "importFailed")
  @JsonView(View.DropSummary.class)
  protected Boolean importFailed;

  @JsonView(View.DropSummary.class)
  @Column(name = "partially_imported")
  private Boolean partiallyImported = false;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  @JsonManagedReference
  public Set<TranslationKit> getTranslationKits() {
    return translationKits;
  }

  public void setTranslationKits(Set<TranslationKit> translationKits) {
    this.translationKits = translationKits;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public DropExporterType getDropExporterType() {
    return dropExporterType;
  }

  public void setDropExporterType(DropExporterType dropExporterType) {
    this.dropExporterType = dropExporterType;
  }

  public String getDropExporterConfig() {
    return dropExporterConfig;
  }

  public void setDropExporterConfig(String dropExporterConfig) {
    this.dropExporterConfig = dropExporterConfig;
  }

  public ZonedDateTime getLastImportedDate() {
    return lastImportedDate;
  }

  public void setLastImportedDate(ZonedDateTime lastImportedDate) {
    this.lastImportedDate = lastImportedDate;
  }

  public Boolean getCanceled() {
    return canceled;
  }

  public void setCanceled(Boolean canceled) {
    this.canceled = canceled;
  }

  public PollableTask getImportPollableTask() {
    return importPollableTask;
  }

  public void setImportPollableTask(PollableTask importPollableTask) {
    this.importPollableTask = importPollableTask;
  }

  public PollableTask getExportPollableTask() {
    return exportPollableTask;
  }

  public void setExportPollableTask(PollableTask exportPollableTask) {
    this.exportPollableTask = exportPollableTask;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getExportFailed() {
    return exportFailed;
  }

  public void setExportFailed(Boolean exportFailed) {
    this.exportFailed = exportFailed;
  }

  public Boolean getImportFailed() {
    return importFailed;
  }

  public void setImportFailed(Boolean importFailed) {
    this.importFailed = importFailed;
  }

  public Boolean getPartiallyImported() {
    return partiallyImported;
  }

  public void setPartiallyImported(Boolean partiallyImported) {
    this.partiallyImported = partiallyImported;
  }
}
