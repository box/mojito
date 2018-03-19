package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;
import org.springframework.data.annotation.CreatedBy;

/**
 * Entity that contains statistics (word count, translated count, etc) of a
 * {@link Repository}
 *
 * @author jaurambault
 */
@Entity
@Table(name = "repository_statistic")
@SqlResultSetMapping(
        name = "RepositoryStatistic.computeBaseStatistics",
        classes = {
            @ConstructorResult(
                    targetClass = RepositoryStatistic.class,
                    columns = {
                        @ColumnResult(name = "usedTextUnitCount", type = Long.class),
                        @ColumnResult(name = "usedTextUnitWordCount", type = Long.class),
                        @ColumnResult(name = "unusedTextUnitCount", type = Long.class),
                        @ColumnResult(name = "unusedTextUnitWordCount", type = Long.class),
                        @ColumnResult(name = "uncommentedTextUnitCount", type = Long.class),
                        @ColumnResult(name = "pluralTextUnitCount", type = Long.class),
                        @ColumnResult(name = "pluralTextUnitWordCount", type = Long.class)
                    }
            )
        }
)
@NamedNativeQueries(
        @NamedNativeQuery(name = "RepositoryStatistic.computeBaseStatistics",
                query
                = "select "
                + "   coalesce(sum(case when map.id is not null and a.deleted = false then 1 else 0 end), 0) as usedTextUnitCount, "
                + "   coalesce(sum(case when map.id is not null and a.deleted = false then tu.word_count else 0 end), 0) as usedTextUnitWordCount, "
                + "   coalesce(sum(case when map.id is null or a.deleted = true then 1 else 0 end), 0) as unusedTextUnitCount, "
                + "   coalesce(sum(case when map.id is null or a.deleted = true then tu.word_count else 0 end), 0) as unusedTextUnitWordCount, "
                + "   coalesce(sum(case when (map.id is not null and a.deleted = false and tu.comment is null) then 1 else 0 end), 0) as uncommentedTextUnitCount, "
                + "   coalesce(sum(case when (map.id is not null and a.deleted = false and tu.plural_form_id is not null) then 1 else 0 end), 0) as pluralTextUnitCount, "
                + "   coalesce(sum(case when (map.id is not null and a.deleted = false and tu.plural_form_id is not null) then tu.word_count else 0 end), 0) as pluralTextUnitWordCount "
                + " "
                + "from tm_text_unit tu "
                + "   inner join asset a on a.id = tu.asset_id "
                + "   left outer join asset_text_unit_to_tm_text_unit map on tu.id = map.tm_text_unit_id and map.asset_extraction_id = a.last_successful_asset_extraction_id "
                + "where "
                + "   a.repository_id = ?1",
                resultSetMapping = "RepositoryStatistic.computeBaseStatistics"
        )
)
public class RepositoryStatistic extends AuditableEntity {

    /**
     * The number of used text units in the repository
     */
    @JsonView(View.RepositorySummary.class)
    private Long usedTextUnitCount = 0L;

    /**
     * The word count of used text units
     */
    @JsonView(View.RepositorySummary.class)
    private Long usedTextUnitWordCount = 0L;

    /**
     * The number of unused text units in the repository
     */
    private Long unusedTextUnitCount = 0L;

    /**
     * The word count of used text units
     */
    private Long unusedTextUnitWordCount = 0L;

    /**
     * The number of text unit for plural forms
     */
    @JsonView(View.RepositorySummary.class)
    private Long pluralTextUnitCount = 0L;
    
    /**
     * The number of words for plural forms
     */
    @JsonView(View.RepositorySummary.class)
     private Long pluralTextUnitWordCount = 0L;

    /**
     * The number of text unit without comments
     */
    private Long uncommentedTextUnitCount = 0L;

    @JsonView(View.RepositorySummary.class)
    @JsonManagedReference
    @OneToMany(mappedBy = "repositoryStatistic", fetch = FetchType.EAGER)
    @OrderBy(value = "locale")
    private Set<RepositoryLocaleStatistic> repositoryLocaleStatistics = new HashSet<>();

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__REPOSITORY_STATISTIC__USER__ID"))
    protected User createdByUser;

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public RepositoryStatistic() {
    }

    public RepositoryStatistic(Long usedTextUnitCount,
            Long usedTextUnitWordCount,
            Long unusedTextUnitCount,
            Long unusedTextUnitWordCount,
            Long uncommentedTextUnitCount,
            Long pluralTextUnitCount,
            Long pluralTextUnitWordCount) {

        this.usedTextUnitCount = usedTextUnitCount;
        this.usedTextUnitWordCount = usedTextUnitWordCount;
        this.unusedTextUnitCount = unusedTextUnitCount;
        this.unusedTextUnitWordCount = unusedTextUnitWordCount;
        this.uncommentedTextUnitCount = uncommentedTextUnitCount;
        this.pluralTextUnitCount = pluralTextUnitCount;
        this.pluralTextUnitWordCount = pluralTextUnitWordCount;
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }

    /**
     * @return the usedTextUnitCount
     */
    public Long getUsedTextUnitCount() {
        return usedTextUnitCount;
    }

    /**
     * @param usedTextUnitCount the usedTextUnitCount to set
     */
    public void setUsedTextUnitCount(Long usedTextUnitCount) {
        this.usedTextUnitCount = usedTextUnitCount;
    }

    /**
     * @return the usedTextUnitWordCount
     */
    public Long getUsedTextUnitWordCount() {
        return usedTextUnitWordCount;
    }

    /**
     * @param usedTextUnitWordCount the usedTextUnitWordCount to set
     */
    public void setUsedTextUnitWordCount(Long usedTextUnitWordCount) {
        this.usedTextUnitWordCount = usedTextUnitWordCount;
    }

    /**
     * @return the unusedTextUnitCount
     */
    public Long getUnusedTextUnitCount() {
        return unusedTextUnitCount;
    }

    /**
     * @param unusedTextUnitCount the unusedTextUnitCount to set
     */
    public void setUnusedTextUnitCount(Long unusedTextUnitCount) {
        this.unusedTextUnitCount = unusedTextUnitCount;
    }

    /**
     * @return the unusedTextUnitWordCount
     */
    public Long getUnusedTextUnitWordCount() {
        return unusedTextUnitWordCount;
    }

    /**
     * @param unusedTextUnitWordCount the unusedTextUnitWordCount to set
     */
    public void setUnusedTextUnitWordCount(Long unusedTextUnitWordCount) {
        this.unusedTextUnitWordCount = unusedTextUnitWordCount;
    }

    public Long getPluralTextUnitCount() {
        return pluralTextUnitCount;
    }

    public void setPluralTextUnitCount(Long pluralTextUnitCount) {
        this.pluralTextUnitCount = pluralTextUnitCount;
    }

    public Long getPluralTextUnitWordCount() {
        return pluralTextUnitWordCount;
    }

    public void setPluralTextUnitWordCount(Long pluralTextUnitWordCount) {
        this.pluralTextUnitWordCount = pluralTextUnitWordCount;
    }
    
    /**
     * @return the repositoryLocaleStatistics
     */
    public Set<RepositoryLocaleStatistic> getRepositoryLocaleStatistics() {
        return repositoryLocaleStatistics;
    }

    /**
     * @param repositoryLocaleStatistics the repositoryLocaleStatistics to set
     */
    public void setRepositoryLocaleStatistics(Set<RepositoryLocaleStatistic> repositoryLocaleStatistics) {
        this.repositoryLocaleStatistics = repositoryLocaleStatistics;
    }

    /**
     * @return the uncommentedTextUnitCount
     */
    public Long getUncommentedTextUnitCount() {
        return uncommentedTextUnitCount;
    }

    /**
     * @param uncommentedTextUnitCount the uncommentedTextUnitCount to set
     */
    public void setUncommentedTextUnitCount(Long uncommentedTextUnitCount) {
        this.uncommentedTextUnitCount = uncommentedTextUnitCount;
    }
    
}
