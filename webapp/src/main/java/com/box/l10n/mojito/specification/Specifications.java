package com.box.l10n.mojito.specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * Contains base {@link Specifications}.
 *
 * @author jaurambault
 */
public class Specifications {

    /**
     * Wraps a {@link SingleParamSpecification} to skip the
     * {@link Specification} if its param is {@code null}
     * ({@link SingleParamSpecification#isParamNull()}).
     *
     * <p>
     * This can be used to implement a filter pattern where the Specification is
     * applied only if the param is not null. This works with spring
     * specifications {@link Specifications#where(Specification) },
     * {@link Specifications#and(Specification) },
     * {@link Specifications#or(Specification)} and
     * {@link Specifications#not(Specification)}
     *
     * <pre>
     * {@code
     *
     * public static SingleParamSpecification<Asset> pathLike(final String path) {
     *
     *    return new SingleParamSpecification<Asset>(path) {
     *       public Predicate toPredicate(Root<Asset> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
     *          return builder.like(root.<String>get("path"), path);
     *       }
     *    };
     * }
     *
     * ...
     *
     * assetRepository.findAll(
     *    where(ifParamNotNull(pathLike(path1))).and(ifParamNotNull(pathLike(path2)))
     * );
     * }</pre>
     *
     * <p>
     * The resulting SQL would look like this:
     *
     * <pre>
     * {@code
     *
     * path1 = null;
     * path2 = null;
     * --> select * from asset;
     *
     * path1 = "path1";
     * path2 = null;
     * --> select * from asset where path like "path1";
     *
     * path1 = null;
     * path2 = "path2";
     * --> select * from asset where path like "path2";
     *
     * path1 = "path1;
     * path2 = "path2";
     * --> select * from asset where path like "path1" and path like "path2";
     *}</pre>
     *
     *
     * @param <T>
     * @param singleParamSpecification specification with one parameter, can be {@literal null}.
     * @return {@code null} if the specification parameter is {@code null} else the specification
     * predicate.
     */
    public static <T> Specification<T> ifParamNotNull(final SingleParamSpecification<T> singleParamSpecification) {
        return new Specification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return singleParamSpecification.isParamNull() ? null : singleParamSpecification.toPredicate(root, query, builder);
            }
        };
    }

}
