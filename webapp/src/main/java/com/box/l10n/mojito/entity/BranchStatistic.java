package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jeanaurambault
 */
@Entity
@Table(
        name = "branch_statistic",
        indexes = {
                @Index(name = "UK__BRANCH_STATISTIC__BRANCH_ID", columnList = "branch_id", unique = true),
        }
)
@NamedEntityGraph(
        name = "BranchStatisticGraph",
        attributeNodes = {
            @NamedAttributeNode(value = "branch", subgraph = "branchGraph"),
            @NamedAttributeNode("branchTextUnitStatistics")},
        subgraphs = @NamedSubgraph(name = "branchGraph",
                attributeNodes = {
                    @NamedAttributeNode(value = "screenshots"),
                    @NamedAttributeNode(value = "repository"),
                }))
public class BranchStatistic extends BaseEntity {

    @JsonView(View.BranchStatistic.class)
    @OneToOne(optional = false)
    @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "FK__BRANCH_STATISTIC__BRANCH__ID"))
    private Branch branch;

    @JsonView(View.BranchStatistic.class)
    @JsonManagedReference
    @OneToMany(mappedBy = "branchStatistic")
    @OrderBy("tmTextUnit.id")
    private Set<BranchTextUnitStatistic> branchTextUnitStatistics = new HashSet<>();

    @JsonView(View.BranchStatistic.class)
    @Column(name="total_count")
    private long totalCount = 0;

    @JsonView(View.BranchStatistic.class)
    @Column(name="for_translation_count")
    private long forTranslationCount = 0;

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Set<BranchTextUnitStatistic> getBranchTextUnitStatistics() {
        return branchTextUnitStatistics;
    }

    public void setBranchTextUnitStatistics(Set<BranchTextUnitStatistic> branchTextUnitStatistics) {
        this.branchTextUnitStatistics = branchTextUnitStatistics;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getForTranslationCount() {
        return forTranslationCount;
    }

    public void setForTranslationCount(long forTranslationCount) {
        this.forTranslationCount = forTranslationCount;
    }
}
