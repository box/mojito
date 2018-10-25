package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;

/**
 * Repository statistic per locale.
 *
 * @author jaurambault
 */
@Entity
@Table(name = "repository_locale_statistic",
        indexes = {
            @Index(name = "UK__REPO_LOCALE_STAT___REPO_STAT_ID_LOCALE_ID", columnList = "repository_statistic_id, locale_id", unique = true)
        }
)
@SqlResultSetMapping(
        name = "RepositoryLocaleStatistic.computeLocaleStatistics",
        classes = {
            @ConstructorResult(
                    targetClass = RepositoryLocaleStatistic.class,
                    columns = {
                        @ColumnResult(name = "translated_count", type = Long.class),
                        @ColumnResult(name = "translated_word_count", type = Long.class),
                        @ColumnResult(name = "translation_needed_count", type = Long.class),
                        @ColumnResult(name = "translation_needed_word_count", type = Long.class),
                        @ColumnResult(name = "review_needed_count", type = Long.class),
                        @ColumnResult(name = "review_needed_word_count", type = Long.class),
                        @ColumnResult(name = "included_in_localized_file_count", type = Long.class),
                        @ColumnResult(name = "included_in_localized_file_word_count", type = Long.class)
                    }
            )
        }
)
@NamedNativeQueries(
        @NamedNativeQuery(name = "RepositoryLocaleStatistic.computeLocaleStatistics",
                query
                = "select "
                + "   coalesce(sum(case when (atu.do_not_translate = false) then 1 else 0 end), 0) as translated_count, "
                + "   coalesce(sum(case when (atu.do_not_translate = false) then tu.word_count else 0 end), 0) as translated_word_count, "
                + "   coalesce(sum(case when (tuv.status = 'TRANSLATION_NEEDED') then 1 else 0 end), 0) as translation_needed_count, "
                + "   coalesce(sum(case when (tuv.status = 'TRANSLATION_NEEDED') then tu.word_count else 0 end), 0) as translation_needed_word_count, "
                + "   coalesce(sum(case when (tuv.status = 'REVIEW_NEEDED'     ) then 1 else 0 end), 0) as review_needed_count, "
                + "   coalesce(sum(case when (tuv.status = 'REVIEW_NEEDED'     ) then tu.word_count else 0 end), 0) as review_needed_word_count, "
                + "   coalesce(sum(case when (tuv.included_in_localized_file and atu.do_not_translate = false) then 1 else 0 end), 0) as included_in_localized_file_count, "
                + "   coalesce(sum(case when (tuv.included_in_localized_file and atu.do_not_translate = false) then tu.word_count else 0 end), 0) as included_in_localized_file_word_count "
                + "from tm_text_unit tu "
                + "   inner join asset a on a.id = tu.asset_id "
                + "   left outer join asset_text_unit_to_tm_text_unit map on tu.id = map.tm_text_unit_id "
                + "   left outer join asset_text_unit atu on atu.id = map.asset_text_unit_id "
                + "   inner join tm_text_unit_current_variant tucv on tucv.tm_text_unit_id = tu.id "
                + "   inner join tm_text_unit_variant tuv on tuv.id = tucv.tm_text_unit_variant_id "
                + "   inner join repository_locale rl on (rl.locale_id = tuv.locale_id and rl.repository_id = a.repository_id) "
                + "where "
                + "   map.id is not null "
                + "   and map.asset_extraction_id = a.last_successful_asset_extraction_id "
                + "   and a.deleted = false "
                + "   and rl.id = ?1 ",
                resultSetMapping = "RepositoryLocaleStatistic.computeLocaleStatistics"
        )
)
public class RepositoryLocaleStatistic extends BaseEntity {

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "repository_statistic_id", foreignKey = @ForeignKey(name = "FK__REPOSITORY_LOCALE_STATISTIC__REPOSITORY__ID"))
    private RepositoryStatistic repositoryStatistic;

    @JsonView(View.LocaleSummary.class)
    @ManyToOne
    @JoinColumn(name = "locale_id", foreignKey = @ForeignKey(name = "FK__REPOSITORY_LOCALE_STATISTIC__LOCALE__ID"), nullable = false)
    private Locale locale;

    /**
     * Number of translated text unit (includes needs review and rejected)
     */
    @JsonView(View.RepositorySummary.class)
    private Long translatedCount = 0L;

    /**
     * Word count of translated text unit (includes needs review and rejected)
     */
    @JsonView(View.RepositorySummary.class)
    private Long translatedWordCount = 0L;

    /**
     * Number of text unit with status
     * {@link TMTextUnitVariant.Status#TRANSLATION_NEEDED}
     */
    @JsonView(View.RepositorySummary.class)
    private Long translationNeededCount = 0L;

    /**
     * Word count of text unit with status
     * {@link TMTextUnitVariant.Status#TRANSLATION_NEEDED}
     */
    @JsonView(View.RepositorySummary.class)
    private Long translationNeededWordCount = 0L;

    /**
     * Number of translations with status
     * {@link TMTextUnitVariant.Status#REVIEW_NEEDED}
     */
    @JsonView(View.RepositorySummary.class)
    private Long reviewNeededCount = 0L;

    /**
     * Word count of translations with status
     * {@link TMTextUnitVariant.Status#REVIEW_NEEDED}
     */
    @JsonView(View.RepositorySummary.class)
    private Long reviewNeededWordCount = 0L;

    /**
     * Number of translations that are included in files
     */
    @JsonView(View.RepositorySummary.class)
    private Long includeInFileCount = 0L;

    /**
     * Word count of translations that are included in files
     */
    @JsonView(View.RepositorySummary.class)
    private Long includeInFileWordCount = 0L;

    @JsonView(View.RepositorySummary.class)
    private Long diffToSourcePluralCount = 0L;

    @JsonView(View.RepositorySummary.class)
    private Long forTranslationCount = 0L;

    @JsonView(View.RepositorySummary.class)
    private Long forTranslationWordCount = 0L;

    public RepositoryLocaleStatistic() {
    }

    public RepositoryLocaleStatistic(
            Long translatedCount,
            Long translatedWordCount,
            Long translationNeededCount,
            Long translationNeededWordCount,
            Long reviewNeededCount,
            Long reviewNeededWordCount,
            Long includeInFileCount,
            Long includeInFileWordCount) {

        this.translatedCount = translatedCount;
        this.translatedWordCount = translatedWordCount;
        this.translationNeededCount = translationNeededCount;
        this.translationNeededWordCount = translationNeededWordCount;
        this.reviewNeededCount = reviewNeededCount;
        this.reviewNeededWordCount = reviewNeededWordCount;
        this.includeInFileCount = includeInFileCount;
        this.includeInFileWordCount = includeInFileWordCount;
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }

    /**
     * @return the repositoryStatistic
     */
    public RepositoryStatistic getRepositoryStatistic() {
        return repositoryStatistic;
    }

    /**
     * @param repositoryStatistic the repositoryStatistic to set
     */
    public void setRepositoryStatistic(RepositoryStatistic repositoryStatistic) {
        this.repositoryStatistic = repositoryStatistic;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @return the translatedCount
     */
    public Long getTranslatedCount() {
        return translatedCount;
    }

    /**
     * @param translatedCount the translatedCount to set
     */
    public void setTranslatedCount(Long translatedCount) {
        this.translatedCount = translatedCount;
    }

    /**
     * @return the reviewNeededCount
     */
    public Long getReviewNeededCount() {
        return reviewNeededCount;
    }

    /**
     * @param reviewNeededCount the reviewNeededCount to set
     */
    public void setReviewNeededCount(Long reviewNeededCount) {
        this.reviewNeededCount = reviewNeededCount;
    }

    /**
     * @return the includeInFileCount
     */
    public Long getIncludeInFileCount() {
        return includeInFileCount;
    }

    /**
     * @param includeInFileCount the includeInFileCount to set
     */
    public void setIncludeInFileCount(Long includeInFileCount) {
        this.includeInFileCount = includeInFileCount;
    }

    /**
     * @return the translationNeededCount
     */
    public Long getTranslationNeededCount() {
        return translationNeededCount;
    }

    /**
     * @param translationNeededCount the translationNeededCount to set
     */
    public void setTranslationNeededCount(Long translationNeededCount) {
        this.translationNeededCount = translationNeededCount;
    }

    public Long getTranslatedWordCount() {
        return translatedWordCount;
    }

    public void setTranslatedWordCount(Long translatedWordCount) {
        this.translatedWordCount = translatedWordCount;
    }

    public Long getTranslationNeededWordCount() {
        return translationNeededWordCount;
    }

    public void setTranslationNeededWordCount(Long translationNeededWordCount) {
        this.translationNeededWordCount = translationNeededWordCount;
    }

    public Long getReviewNeededWordCount() {
        return reviewNeededWordCount;
    }

    public void setReviewNeededWordCount(Long reviewNeededWordCount) {
        this.reviewNeededWordCount = reviewNeededWordCount;
    }

    public Long getIncludeInFileWordCount() {
        return includeInFileWordCount;
    }

    public void setIncludeInFileWordCount(Long includeInFileWordCount) {
        this.includeInFileWordCount = includeInFileWordCount;
    }

    public Long getDiffToSourcePluralCount() {
        return diffToSourcePluralCount;
    }

    public void setDiffToSourcePluralCount(Long diffToSourcePluralCount) {
        this.diffToSourcePluralCount = diffToSourcePluralCount;
    }

    public Long getForTranslationCount() {
        return forTranslationCount;
    }

    public void setForTranslationCount(Long forTranslationCount) {
        this.forTranslationCount = forTranslationCount;
    }

    public Long getForTranslationWordCount() {
        return forTranslationWordCount;
    }

    public void setForTranslationWordCount(Long forTranslationWordCount) {
        this.forTranslationWordCount = forTranslationWordCount;
    }

}
