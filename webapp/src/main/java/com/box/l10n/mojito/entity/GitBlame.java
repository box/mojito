package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author jaurambault
 */
@Entity
@Table(
        name = "git_blame",
        indexes = {
                @Index(name = "I__GIT_BLAME__AUTHOR_EMAIL", columnList = "author_email"),
                @Index(name = "UK__GIT_BLAME__TM_TEXT_UNIT_ID", columnList = "tm_text_unit_id", unique = true)
        }
)
public class GitBlame extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "tm_text_unit_id", foreignKey = @ForeignKey(name = "FK__GIT_BLAME__TM_TEXT_UNIT__ID"))
    @JsonIgnore
    private TMTextUnit tmTextUnit;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "commit_time")
    private String commitTime;

    @Column(name = "commit_name")
    private String commitName;

    @Column(name = "pull_request_id")
    private String pullRequestId;

    public TMTextUnit getTmTextUnit() {
        return tmTextUnit;
    }

    public void setTmTextUnit(TMTextUnit tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
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

    public String getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(String commitTime) {
        this.commitTime = commitTime;
    }

    public String getCommitName() {
        return commitName;
    }

    public void setCommitName(String commitName) {
        this.commitName = commitName;
    }

    public String getPullRequestId() {
        return pullRequestId;
    }

    public void setPullRequestId(String pullRequestId) {
        this.pullRequestId = pullRequestId;
    }
}