package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * @author jaurambault
 */
@Entity
@Table(name = "sla_incident")
public class SlaIncident extends AuditableEntity {

  @Column(name = "closed_date")
  private ZonedDateTime closedDate;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "sla_incident_repositories",
      joinColumns = {@JoinColumn(name = "sla_incident_id")},
      inverseJoinColumns = {@JoinColumn(name = "repository_id")},
      foreignKey = @ForeignKey(name = "FK__SLA_INCIDENT_REPOSITORIES__SLA_INCIDENT__ID"),
      inverseForeignKey = @ForeignKey(name = "FK__SLA_INCIDENT_REPOSITORIES__REPOSITORY__ID"))
  private Set<Repository> repositories;

  public Set<Repository> getRepositories() {
    return repositories;
  }

  public void setRepositories(Set<Repository> repositories) {
    this.repositories = repositories;
  }

  public ZonedDateTime getClosedDate() {
    return closedDate;
  }

  public void setClosedDate(ZonedDateTime closedDate) {
    this.closedDate = closedDate;
  }
}
