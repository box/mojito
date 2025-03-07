package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchStatistic_;
import com.box.l10n.mojito.entity.Branch_;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.entity.security.user.User_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.ZonedDateTime;

/**
 * @author jeanaurambault
 */
public class BranchStatisticSpecification {

  public static SingleParamSpecification<BranchStatistic> branchIdEquals(final Long branchId) {
    return new SingleParamSpecification<>(branchId) {
      @Override
      public Predicate toPredicate(
          Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<BranchStatistic, Branch> branchJoin =
            root.join(BranchStatistic_.branch, JoinType.LEFT);
        return builder.equal(branchJoin.get(Branch_.id), branchId);
      }
    };
  }

  public static SingleParamSpecification<BranchStatistic> branchNameEquals(
      final String branchName) {
    return new SingleParamSpecification<>(branchName) {
      @Override
      public Predicate toPredicate(
          Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<BranchStatistic, Branch> branchJoin =
            root.join(BranchStatistic_.branch, JoinType.LEFT);
        return builder.equal(branchJoin.get(Branch_.name), branchName);
      }
    };
  }

  public static SingleParamSpecification<BranchStatistic> search(final String search) {
    return new SingleParamSpecification<>(search) {
      @Override
      public Predicate toPredicate(
          Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<BranchStatistic, Branch> branchJoin =
            root.join(BranchStatistic_.branch, JoinType.LEFT);
        Join<Branch, User> userJoin = branchJoin.join(Branch_.createdByUser, JoinType.LEFT);

        String ilikeSearch = search.toLowerCase();

        return builder.or(
            builder.like(builder.lower(branchJoin.get(Branch_.name)), ilikeSearch),
            builder.like(builder.lower(userJoin.get(User_.username)), ilikeSearch));
      }
    };
  }

  public static SingleParamSpecification<BranchStatistic> createdByUserNameEquals(
      final String createdByUserName) {
    return new SingleParamSpecification<>(createdByUserName) {
      @Override
      public Predicate toPredicate(
          Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<BranchStatistic, Branch> branchJoin =
            root.join(BranchStatistic_.branch, JoinType.LEFT);
        Join<Branch, User> userJoin = branchJoin.join(Branch_.createdByUser, JoinType.LEFT);
        return builder.equal(userJoin.get(User_.username), createdByUserName);
      }
    };
  }

  public static SingleParamSpecification<BranchStatistic> deletedEquals(final Boolean deleted) {
    return new SingleParamSpecification<>(deleted) {
      @Override
      public Predicate toPredicate(
          Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<BranchStatistic, Branch> branchJoin =
            root.join(BranchStatistic_.branch, JoinType.LEFT);
        return builder.equal(branchJoin.get(Branch_.deleted), deleted);
      }
    };
  }

  public static SingleParamSpecification<BranchStatistic> createdBefore(
      final ZonedDateTime createdBefore) {
    return new SingleParamSpecification<>(createdBefore) {
      @Override
      public Predicate toPredicate(
          Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<BranchStatistic, Branch> branchJoin =
            root.join(BranchStatistic_.branch, JoinType.LEFT);
        return builder.lessThanOrEqualTo(branchJoin.get(Branch_.createdDate), createdBefore);
      }
    };
  }

  public static SingleParamSpecification<BranchStatistic> createdAfter(
      final ZonedDateTime createdAfter) {
    return new SingleParamSpecification<>(createdAfter) {
      @Override
      public Predicate toPredicate(
          Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<BranchStatistic, Branch> branchJoin =
            root.join(BranchStatistic_.branch, JoinType.LEFT);
        return builder.greaterThanOrEqualTo(branchJoin.get(Branch_.createdDate), createdAfter);
      }
    };
  }

  /**
   * @param empty if {@code null}, will behave as {@link Boolean#FALSE}
   * @return
   */
  public static SingleParamSpecification<BranchStatistic> empty(final Boolean empty) {

    return new SingleParamSpecification<>(empty) {
      @Override
      public Predicate toPredicate(
          Root<BranchStatistic> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return empty != null && empty
            ? builder.equal(root.get(BranchStatistic_.totalCount), 0L)
            : builder.notEqual(root.get(BranchStatistic_.totalCount), 0L);
      }
    };
  }
}
