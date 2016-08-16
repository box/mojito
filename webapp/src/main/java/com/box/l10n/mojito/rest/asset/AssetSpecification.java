package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Asset_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author wyau
 */
public class AssetSpecification {

    /**
     * A {@link Specification} the check if {@link Asset#path} is equal
     *
     * @param path value to check
     * @return {@link Specification}
     */
    public static SingleParamSpecification<Asset> pathEquals(final String path) {
        return new SingleParamSpecification<Asset>(path) {
            public Predicate toPredicate(Root<Asset> root, CriteriaQuery<?> query,
                                         CriteriaBuilder builder) {

                return builder.equal(root.get(Asset_.path), path);
            }
        };
    }

    /**
     * A {@link Specification} the check if {@link Asset#repository} is equal
     *
     * @param repositoryId value to check
     * @return {@link Specification}
     */
    public static SingleParamSpecification<Asset> repositoryIdEquals(final Long repositoryId) {
        return new SingleParamSpecification<Asset>(repositoryId) {
            public Predicate toPredicate(Root<Asset> root, CriteriaQuery<?> query,
                                         CriteriaBuilder builder) {
                return builder.equal(root.get(Asset_.repository), repositoryId);
            }
        };
    }
    
    /**
     * A {@link Specification} that checks if {@link Asset} is deleted
     * 
     * @param deleted
     * @return {@link Specification}
     */
    public static SingleParamSpecification<Asset> deletedEquals(final Boolean deleted) {
        return new SingleParamSpecification<Asset>(deleted) {
            @Override
            public Predicate toPredicate(Root<Asset> root, 
                                         CriteriaQuery<?> query, 
                                         CriteriaBuilder builder) {
                return builder.equal(root.get(Asset_.deleted), deleted);
            }   
        };
    }
}
