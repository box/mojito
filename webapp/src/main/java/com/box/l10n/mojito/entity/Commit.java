package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author garion
 */
@Entity
@Table(
        name = "commit",
        indexes = {
                @Index(name = "UK__COMMIT__NAME_REPOSITORY_ID", columnList = "name, repository_id", unique = true)
        }
)
public class Commit extends AuditableEntity {
    @ManyToOne
    @JoinColumn(name = "repository_id",
            foreignKey = @ForeignKey(name = "FK__COMMIT__NAME__REPOSITORY_ID"))
    private Repository repository;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "name")
    private String name;

    @Column(name = "source_creation_date")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime sourceCreationDate;

    @OneToOne(mappedBy = "commit")
    @JsonManagedReference
    private CommitToPushRun commitToPushRun;

    @OneToOne(mappedBy = "commit")
    @JsonManagedReference
    private CommitToPullRun commitToPullRun;

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DateTime getSourceCreationDate() {
        return sourceCreationDate;
    }

    public void setSourceCreationDate(DateTime sourceCreationDate) {
        this.sourceCreationDate = sourceCreationDate;
    }

    public CommitToPushRun getCommitToPushRun() {
        return commitToPushRun;
    }

    public void setCommitToPushRun(CommitToPushRun commitToPushRun) {
        this.commitToPushRun = commitToPushRun;
    }

    public CommitToPullRun getCommitToPullRun() {
        return commitToPullRun;
    }

    public void setCommitToPullRun(CommitToPullRun commitToPullRun) {
        this.commitToPullRun = commitToPullRun;
    }
}
