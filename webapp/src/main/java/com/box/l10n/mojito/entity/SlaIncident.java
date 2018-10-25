package com.box.l10n.mojito.entity;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

/**
 * @author jaurambault
 */
@Entity
@Table(name = "sla_incident")
public class SlaIncident extends AuditableEntity {

    @Column(name = "closed_date")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime closedDate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "sla_incident_repositories", 
        joinColumns = { @JoinColumn(name = "sla_incident_id") }, 
        inverseJoinColumns = { @JoinColumn(name = "repository_id") }
    )
    private Set<Repository> repositories;

    public Set<Repository> getRepositories() {
        return repositories;
    }

    public void setRepositories(Set<Repository> repositories) {
        this.repositories = repositories;
    }

    public DateTime getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(DateTime closedDate) {
        this.closedDate = closedDate;
    }

}
