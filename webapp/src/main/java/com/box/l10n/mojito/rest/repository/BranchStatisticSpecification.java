package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchStatistic_;
import com.box.l10n.mojito.entity.Branch_;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.entity.security.user.User_;
import com.box.l10n.mojito.specification.SingleParamSpecification;

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

}
