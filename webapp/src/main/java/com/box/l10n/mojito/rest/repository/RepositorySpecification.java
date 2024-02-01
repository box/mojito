package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Repository_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author aloison
 */
public class RepositorySpecification {

  /**
   * A {@link Specification} that checks if {@link Repository#name} is equal
   *
   * @param name value to check
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Repository> nameEquals(final String name) {
    return new SingleParamSpecification<Repository>(name) {
      @Override
      public Predicate toPredicate(
          Root<Repository> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        return builder.equal(root.get(Repository_.name), name);
      }
    };
  }

  /**
   * A {@link Specification} that checks if {@link Repository} is deleted
   *
   * @param deleted
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Repository> deletedEquals(final Boolean deleted) {
    return new SingleParamSpecification<Repository>(deleted) {
      @Override
      public Predicate toPredicate(
          Root<Repository> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.equal(root.get(Repository_.deleted), deleted);
      }
    };
  }
}
