package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.AssetExtractionByBranch_;
import com.box.l10n.mojito.entity.Asset_;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Branch_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
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

        Predicate conjunction = builder.conjunction();
        conjunction
            .getExpressions()
            .add(builder.equal(assetExtractionByBranchBranchJoin.get(Branch_.id), branchId));

        if (deleted != null) {
          conjunction
              .getExpressions()
              .add(
                  builder.equal(
                      assetAssetExtractionByBranchSetJoin.get(AssetExtractionByBranch_.deleted),
                      deleted));
        }

        return conjunction;
      }
    };
  }
}
