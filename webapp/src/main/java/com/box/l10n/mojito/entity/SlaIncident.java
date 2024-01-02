package com.box.l10n.mojito.entity;

import java.time.ZonedDateTime;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/** @author jaurambault */
@Entity
@Table(name = "sla_incident")
public class SlaIncident extends AuditableEntity {

  @Column(name = "closed_date")
  private ZonedDateTime closedDate;

  @ManyToMany(fetch = FetchType.EAGER)
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
