package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedBy;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a text unit.
 *
 * A text unit is an entity that needs to be translated from a source language
 * into multiple target languages.
 *
 * A text unit is uniquely identified within a {@link TM} by its name + content
 * + comment (which is also the MD5 value).
 *
 * @author jaurambault
 */
@Entity
@Table(name = "tm_text_unit",
        indexes = {
            @Index(name = "UK__TM_TEXT_UNIT__MD5__TM_ID__ASSET_ID", columnList = "md5, tm_id, asset_id", unique = true),
            @Index(name = "I__TM_TEXT_UNIT__NAME", columnList = "name"),
            @Index(name = "I__TM_TEXT_UNIT__CONTENT_MD5", columnList = "content_md5"),
            @Index(name = "I__TM_TEXT_UNIT__PLURAL_FORM_OTHER", columnList = "plural_form_other")
        }
)
@BatchSize(size = 1000)
public class TMTextUnit extends SettableAuditableEntity {

    @JsonView(View.IdAndName.class)
    @Column(name = "name", length = Integer.MAX_VALUE)
    private String name;

    @JsonView(View.TmTextUnitSummary.class)
    @Column(name = "content", length = Integer.MAX_VALUE)
    private String content;

    /**
     * should be built from the name, content and the comment field
     */
    @Column(name = "md5", length = 32)
    String md5;

    /**
     * should be built from the content only
     */
    @Column(name = "content_md5", length = 32)
    private String contentMd5;

    @Column(name = "comment", length = Integer.MAX_VALUE)
    private String comment;

    @Column(name = "word_count")
    private Integer wordCount;

    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "tm_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT__TM__ID"))
    private TM tm;

    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT__ASSET__ID"), nullable = false)
    private Asset asset;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT__USER__ID"))
    protected User createdByUser;
 
    @ManyToOne
    @JoinColumn(name = "plural_form_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT__PLURAL_FORM__ID"))
    protected PluralForm pluralForm;
    
    @Column(name = "plural_form_other", length = Integer.MAX_VALUE)
    protected String pluralFormOther;

    @OneToOne(mappedBy = "tmTextUnit", fetch = FetchType.LAZY)
    protected TMTextUnitStatistic tmTextUnitStatistic;

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public TM getTm() {
        return tm;
    }

    public void setTm(TM tm) {
        this.tm = tm;
    }

    public PluralForm getPluralForm() {
        return pluralForm;
    }

    public void setPluralForm(PluralForm pluralForm) {
        this.pluralForm = pluralForm;
    }

    public String getPluralFormOther() {
        return pluralFormOther;
    }

    public void setPluralFormOther(String pluralFormOther) {
        this.pluralFormOther = pluralFormOther;
    }

    public TMTextUnitStatistic getStatistic() {
        return tmTextUnitStatistic;
    }

    public void setStatistic(TMTextUnitStatistic tmTextUnitStatistic) {
        this.tmTextUnitStatistic = tmTextUnitStatistic;
    }

}
