package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.data.annotation.CreatedBy;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;

/**
 * Represents a translation of a {@link TMTextUnit} in a target language.
 *
 * @author jaurambault
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(
        name = "tm_text_unit_variant"
)
public class TMTextUnitVariant extends SettableAuditableEntity {

    /**
     * Status of a text unit variant.
     *
     * The usual workflow is the following (action in parenthesis):
     *  
     * NEW / TRANSLATION_NEEDED --> (translate) --> REVIEW_NEEDED --> (review)
     * --> APPROVED
     *
     * NEW is text unit that don't translation yet in the TM.
     *
     */
    public enum Status {

        /**
         * Indicates that translation is needed for that text unit and locale.
         *
         * Usually the case when string has been leveraged and the translation
         * must be re-done or if the translation is improper and was flagged.
         *
         */
        TRANSLATION_NEEDED,
        /**
         * Indicates that the text unit needs to be reviewed.
         *
         * <p>
         * Review comes after the translation. It doesn't tell you anything about
         * the quality of the translation, it can be good or bad, it just means
         * that someone must review it.
         * <p>
         * When a string is identified as improper it should be marked for
         * re-translation using the TRANSLATION_NEEDED status along with a
         * comment.
         *
         */
        REVIEW_NEEDED,
        /**
         * A string that doesn't need any work to be performed on it.
         */
        APPROVED;

    };

    @Column(name = "content", length = Integer.MAX_VALUE)
    private String content;

    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "tm_text_unit_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_VARIANT__TM_TEXT_UNIT__ID"))
    private TMTextUnit tmTextUnit;

    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "locale_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_VARIANT__LOCALE__ID"))
    private Locale locale;

    /**
     * should be built from the content field
     */
    @Column(name = "content_md5", length = 32)
    String contentMD5;

    /**
     * translation comment, used to explain why this translation was chosen
     * (Usually used in case it's not obvious for example when deliberately
     * using the same text as the source).
     */
    @Column(name = "comment", length = Integer.MAX_VALUE)
    private String comment;
    
    @Basic(optional = false)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.APPROVED;

    /**
     * Indicates if the translation should be used to generate localized files.
     *
     * <p>
     * Typically, translations with issue in placeholder must not be added to
     * localized file because it could break a build or application runtime.
     * This attribute will be usually set by an automatic process that looks for
     * potential issues and can be overridden manually by a user after having
     * checked.
     */
    @Column(name = "included_in_localized_file")
    private boolean includedInLocalizedFile = true;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_VARIANT__USER__ID"))
    protected User createdByUser;

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    @JsonManagedReference
    @OneToMany(mappedBy = "tmTextUnitVariant", fetch = FetchType.EAGER)
    private Set<TMTextUnitVariantComment> tmTextUnitVariantComments = new HashSet<>();
   
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public TMTextUnit getTmTextUnit() {
        return tmTextUnit;
    }

    public void setTmTextUnit(TMTextUnit tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getContentMD5() {
        return contentMD5;
    }

    public void setContentMD5(String contentMD5) {
        this.contentMD5 = contentMD5;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isIncludedInLocalizedFile() {
        return includedInLocalizedFile;
    }

    public void setIncludedInLocalizedFile(boolean includedInLocalizedFile) {
        this.includedInLocalizedFile = includedInLocalizedFile;
    }

    public Set<TMTextUnitVariantComment> getTmTextUnitVariantComments() {
        return tmTextUnitVariantComments;
    }

    public void setTmTextUnitVariantComments(Set<TMTextUnitVariantComment> tmTextUnitVariantComments) {
        this.tmTextUnitVariantComments = tmTextUnitVariantComments;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
 
}
