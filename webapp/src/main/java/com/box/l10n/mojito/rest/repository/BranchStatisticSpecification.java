package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchStatistic_;
import com.box.l10n.mojito.entity.Branch_;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.entity.security.user.User_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import org.joda.time.DateTime;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * @author jeanaurambault
 */
public class BranchStatisticSpecification {

    public static SingleParamSpecification<BranchStatistic> branchEquals(final Long branchId) {
        return new SingleParamSpecification<BranchStatistic>(branchId) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return builder.equal(root.get(BranchStatistic_.branch), branchId);
            }
        };
    }

    public static SingleParamSpecification<BranchStatistic> branchNameEquals(final String branchName) {
        return new SingleParamSpecification<BranchStatistic>(branchName) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                Join<BranchStatistic, Branch> branchJoin = root.join(BranchStatistic_.branch, JoinType.LEFT);
                return builder.equal(branchJoin.get(Branch_.name), branchName);
            }
        };
    }

    public static SingleParamSpecification<BranchStatistic> search(final String search) {
        return new SingleParamSpecification<BranchStatistic>(search) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                Join<BranchStatistic, Branch> branchJoin = root.join(BranchStatistic_.branch, JoinType.LEFT);
                Join<Branch, User> userJoin = branchJoin.join(Branch_.createdByUser, JoinType.LEFT);

                String ilikeSearch = search.toLowerCase();

                return builder.or(
                        builder.like(builder.lower(branchJoin.get(Branch_.name)), ilikeSearch),
                        builder.like(builder.lower(userJoin.get(User_.username)), ilikeSearch)
                );
            }
        };
    }

    public static SingleParamSpecification<BranchStatistic> createdByUserNameEquals(final String createdByUserName) {
        return new SingleParamSpecification<BranchStatistic>(createdByUserName) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                Join<BranchStatistic, Branch> branchJoin = root.join(BranchStatistic_.branch, JoinType.LEFT);
                Join<Branch, User> userJoin = branchJoin.join(Branch_.createdByUser, JoinType.LEFT);
                return builder.equal(userJoin.get(User_.username), createdByUserName);
            }
        };
    }

    public static SingleParamSpecification<BranchStatistic> deletedEquals(final Boolean deleted) {
        return new SingleParamSpecification<BranchStatistic>(deleted) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                Join<BranchStatistic, Branch> branchJoin = root.join(BranchStatistic_.branch, JoinType.LEFT);
                return builder.equal(branchJoin.get(Branch_.deleted), deleted);
            }
        };
    }

    public static SingleParamSpecification<BranchStatistic> createdBefore(final DateTime createdBefore) {
        return new SingleParamSpecification<BranchStatistic>(createdBefore) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                Join<BranchStatistic, Branch> branchJoin = root.join(BranchStatistic_.branch, JoinType.LEFT);
                return builder.lessThanOrEqualTo(branchJoin.get(Branch_.createdDate), createdBefore);
            }
        };
    }

    public static SingleParamSpecification<BranchStatistic> createdAfter(final DateTime createdAfter) {
        return new SingleParamSpecification<BranchStatistic>(createdAfter) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                Join<BranchStatistic, Branch> branchJoin = root.join(BranchStatistic_.branch, JoinType.LEFT);
                return builder.greaterThanOrEqualTo(branchJoin.get(Branch_.createdDate), createdAfter);
            }
        };
    }

    /**
     * @param empty if {@code null}, will behave as {@link Boolean#FALSE}
     * @return
     */
    public static SingleParamSpecification<BranchStatistic> empty(final Boolean empty) {

        return new SingleParamSpecification<BranchStatistic>(empty) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return empty != null && empty
                        ? builder.equal(root.get(BranchStatistic_.totalCount), 0L)
                        : builder.notEqual(root.get(BranchStatistic_.totalCount), 0L);
            }
        };
    }

    public static SingleParamSpecification<BranchStatistic> totalCountLessThanOrEqualsTo(final long count) {

        return new SingleParamSpecification<BranchStatistic>(count) {
            @Override
            public Predicate toPredicate(Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return builder.lessThanOrEqualTo(root.get(BranchStatistic_.totalCount), count);
            }
        };
    }
}
