package com.box.l10n.mojito.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

/**
 * Entity to keep track of which repository's statistics are outdated.
 * 
 * @author jyi
 */
@Entity
@Table(
        name = "statistics_schedule",
        indexes = {
            @Index(name = "I__STATISTICS_SCHEDULE__REPOSITORY_ID", columnList = "repository_id", unique = false)
        }
)
public class StatisticsSchedule extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "repository_id", foreignKey = @ForeignKey(name = "FK__STATISTICS_SCHEDULE__REPOSITORY_ID"))
    private Repository repository;

    @Column(name = "time_to_update")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    protected DateTime timeToUpdate;

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public DateTime getTimeToUpdate() {
        return timeToUpdate;
    }

    public void setTimeToUpdate(DateTime timeToUpdate) {
        this.timeToUpdate = timeToUpdate;
    }

}
