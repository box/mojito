package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchStatistic_;
import com.box.l10n.mojito.entity.Branch_;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import org.joda.time.DateTime;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Date;

/**
 * @author jeanaurambault
 */
public class BranchSpecification {

    public static SingleParamSpecification<Branch> nameEquals(final String name) {
        return new SingleParamSpecification<Branch>(name) {
            @Override
            public Predicate toPredicate(Root<Branch> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return builder.equal(root.get(Branch_.name), name);
            }
        };
    }

    public static SingleParamSpecification<Branch> deletedEquals(final Boolean deleted) {
        return new SingleParamSpecification<Branch>(deleted) {
            @Override
            public Predicate toPredicate(Root<Branch> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return builder.equal(root.get(Branch_.deleted), deleted);
            }
        };
    }

    public static SingleParamSpecification<Branch> repositoryEquals(final Repository repository) {
        return new SingleParamSpecification<Branch>(repository) {
            @Override
            public Predicate toPredicate(Root<Branch> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return builder.equal(root.get(Branch_.repository), repository);
            }
        };
    }

    public static SingleParamSpecification<Branch> createdBefore(final DateTime createdBefore) {
        return new SingleParamSpecification<Branch>(createdBefore) {
            @Override
            public Predicate toPredicate(Root<Branch> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return builder.lessThanOrEqualTo(root.get(Branch_.createdDate), createdBefore);
            }
        };
    }

    public static SingleParamSpecification<Branch> branchStatisticTranslated(final Boolean translated) {
        return new SingleParamSpecification<Branch>(translated) {
            @Override
            public Predicate toPredicate(Root<Branch> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                Join<Branch, BranchStatistic> branchStatisticJoin = root.join(Branch_.branchStatistic, JoinType.LEFT);
                Predicate predicate = builder.equal(branchStatisticJoin.get(BranchStatistic_.forTranslationCount), 0L);

                if (!translated) {
                    predicate = builder.not(predicate);
                }

                return predicate;
            }
        };
    }

}
