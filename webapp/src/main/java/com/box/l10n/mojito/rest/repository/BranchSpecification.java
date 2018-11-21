package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Branch_;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.specification.SingleParamSpecification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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

    public static SingleParamSpecification<Branch> repositoryEquals(final Repository repository) {
        return new SingleParamSpecification<Branch>(repository) {
            @Override
            public Predicate toPredicate(Root<Branch> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return builder.equal(root.get(Branch_.repository), repository);
            }
        };
    }

}
