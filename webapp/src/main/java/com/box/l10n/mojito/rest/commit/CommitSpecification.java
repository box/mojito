package com.box.l10n.mojito.rest.commit;

import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.entity.CommitToPullRun;
import com.box.l10n.mojito.entity.CommitToPullRun_;
import com.box.l10n.mojito.entity.CommitToPushRun;
import com.box.l10n.mojito.entity.CommitToPushRun_;
import com.box.l10n.mojito.entity.Commit_;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRun_;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRun_;
import com.box.l10n.mojito.entity.Repository_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author garion
 */
public class CommitSpecification {

  /**
   * A {@link Specification} the check if {@link Commit#repository} is equal
   *
   * @param repositoryId value to check
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Commit> repositoryIdEquals(final Long repositoryId) {
    return new SingleParamSpecification<Commit>(repositoryId) {
      public Predicate toPredicate(
          Root<Commit> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.equal(root.get(Commit_.repository).get(Repository_.id), repositoryId);
      }
    };
  }

  /**
   * A {@link Specification} that checks if the {@link Commit} name is in the specified list
   *
   * @param commitNames The names of the commits to search for.
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Commit> commitNamesIn(final List<String> commitNames) {
    return new SingleParamSpecification<Commit>(commitNames) {
      @Override
      public Predicate toPredicate(
          Root<Commit> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return root.get(Commit_.name).in(commitNames);
      }
    };
  }

  /**
   * A {@link Specification} that checks if the related PushRun name equals the parameter
   *
   * @param pushRunName The name of the PushRun
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Commit> pushRunNameEquals(final String pushRunName) {
    return new SingleParamSpecification<Commit>(pushRunName) {
      @Override
      public Predicate toPredicate(
          Root<Commit> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        Join<Commit, CommitToPushRun> commitToPushRunJoin =
            root.join(Commit_.commitToPushRun, JoinType.LEFT);
        Join<CommitToPushRun, PushRun> pushRunJoin =
            commitToPushRunJoin.join(CommitToPushRun_.pushRun, JoinType.LEFT);

        return builder.equal(pushRunJoin.get(PushRun_.name), pushRunName);
      }
    };
  }

  /**
   * A {@link Specification} that checks if the related PullRun name equals the parameter
   *
   * @param pullRunName The name of the PullRun
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Commit> pullRunNameEquals(final String pullRunName) {
    return new SingleParamSpecification<Commit>(pullRunName) {
      @Override
      public Predicate toPredicate(
          Root<Commit> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        Join<Commit, CommitToPullRun> commitToPullRunJoin =
            root.join(Commit_.commitToPullRun, JoinType.LEFT);
        Join<CommitToPullRun, PullRun> pullRunJoin =
            commitToPullRunJoin.join(CommitToPullRun_.pullRun, JoinType.LEFT);

        return builder.equal(pullRunJoin.get(PullRun_.name), pullRunName);
      }
    };
  }

  /**
   * A {@link Specification} that checks if the Commit has an associated PushRun
   *
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Commit> hasPushRun(final Boolean hasPushRun) {
    return new SingleParamSpecification<Commit>(hasPushRun) {
      @Override
      public Predicate toPredicate(
          Root<Commit> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<Commit, CommitToPushRun> commitToPushRunJoin =
            root.join(Commit_.commitToPushRun, JoinType.LEFT);

        return hasPushRun != null && hasPushRun
            ? builder.isNotNull(commitToPushRunJoin.get(CommitToPushRun_.pushRun))
            : builder.isNull(commitToPushRunJoin.get(CommitToPushRun_.pushRun));
      }
    };
  }

  /**
   * A {@link Specification} that checks if the Commit has an associated PullRun
   *
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Commit> hasPullRun(final Boolean hasPullRun) {
    return new SingleParamSpecification<Commit>(hasPullRun) {
      @Override
      public Predicate toPredicate(
          Root<Commit> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Join<Commit, CommitToPullRun> commitToPullRunJoin =
            root.join(Commit_.commitToPullRun, JoinType.LEFT);

        return hasPullRun != null && hasPullRun
            ? builder.isNotNull(commitToPullRunJoin.get(CommitToPullRun_.pullRun))
            : builder.isNull(commitToPullRunJoin.get(CommitToPullRun_.pullRun));
      }
    };
  }
}
