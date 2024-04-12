package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.AssetExtractionByBranch_;
import com.box.l10n.mojito.entity.Asset_;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Branch_;
import com.box.l10n.mojito.entity.Repository_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import java.util.ArrayList;
import java.util.List;
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
      public Predicate toPredicate(
          Root<Asset> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

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
      public Predicate toPredicate(
          Root<Asset> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.equal(root.get(Asset_.repository).get(Repository_.id), repositoryId);
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
      public Predicate toPredicate(
          Root<Asset> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.equal(root.get(Asset_.deleted), deleted);
      }
    };
  }

  /**
   * A {@link Specification} that checks if {@link Asset} has virtualContent
   *
   * @param virtual
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Asset> virtualEquals(final Boolean virtual) {
    return new SingleParamSpecification<Asset>(virtual) {
      @Override
      public Predicate toPredicate(
          Root<Asset> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.equal(root.get(Asset_.virtual), virtual);
      }
    };
  }

  /**
   * A {@link Specification} that checks if {@link Asset} has virtualContent
   *
   * @param virtual
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Asset> idsIn(final List<Long> ids) {
    return new SingleParamSpecification<Asset>(ids) {
      @Override
      public Predicate toPredicate(
          Root<Asset> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return root.get(Asset_.id).in(ids);
      }
    };
  }

  public static SingleParamSpecification<Asset> branchId(Long branchId, Boolean deleted) {
    return new SingleParamSpecification<Asset>(branchId) {
      @Override
      public Predicate toPredicate(
          Root<Asset> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        SetJoin<Asset, AssetExtractionByBranch> assetAssetExtractionByBranchSetJoin =
            root.join(Asset_.assetExtractionByBranches, JoinType.LEFT);
        Join<AssetExtractionByBranch, Branch> assetExtractionByBranchBranchJoin =
            assetAssetExtractionByBranchSetJoin.join(
                AssetExtractionByBranch_.branch, JoinType.LEFT);

        final List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(assetExtractionByBranchBranchJoin.get(Branch_.id), branchId));

        if (deleted != null) {
          predicates.add(
              builder.equal(
                  assetAssetExtractionByBranchSetJoin.get(AssetExtractionByBranch_.deleted),
                  deleted));
        }

        return builder.and(predicates.toArray(new Predicate[predicates.size()]));
      }
    };
  }
}
