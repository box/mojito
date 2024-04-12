package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnit_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * @author jaurambault
 */
public class AssetTextUnitSpecification {

  public static SingleParamSpecification<AssetTextUnit> assetExtractionIdEquals(
      final Long assetExtractionId) {
    return new SingleParamSpecification<AssetTextUnit>(assetExtractionId) {
      @Override
      public Predicate toPredicate(
          Root<AssetTextUnit> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        return builder.equal(root.get(AssetTextUnit_.assetExtraction), assetExtractionId);
      }
    };
  }

  public static SingleParamSpecification<AssetTextUnit> doNotTranslateEquals(
      final Boolean doNotTranslate) {
    return new SingleParamSpecification<AssetTextUnit>(doNotTranslate) {
      @Override
      public Predicate toPredicate(
          Root<AssetTextUnit> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.equal(root.get(AssetTextUnit_.doNotTranslate), doNotTranslate);
      }
    };
  }
}
