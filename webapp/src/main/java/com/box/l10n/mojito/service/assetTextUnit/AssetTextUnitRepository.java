package com.box.l10n.mojito.service.assetTextUnit;

import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetTextUnitRepository
    extends JpaRepository<AssetTextUnit, Long>, JpaSpecificationExecutor<AssetTextUnit> {

  List<AssetTextUnit> findByAssetExtraction(AssetExtraction assetExtraction);

  @EntityGraph(value = "AssetTextUnit.legacy", type = EntityGraphType.FETCH)
  List<AssetTextUnit> findByAssetExtractionId(long assetExtractionId);

  @Query(
      """
      select new com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitIdToMd5(atu.id, atu.md5)
      from #{#entityName} atu
      where atu.assetExtraction = ?1
      """)
  List<AssetTextUnitIdToMd5> findMd5ByAssetExtraction(AssetExtraction assetExtraction);

  /**
   * Gets unmapped {@link AssetTextUnit}s
   *
   * @param assetExtractionId {@link AssetExtraction} id
   * @return the unmapped {@link AssetTextUnit}s
   */
  // TODO(P1) In fact, it should be possible to write that using HQL with modification to data model
  // Change like this in TMTextUnit
  //    @OneToMany(mappedBy = "tmTextUnit")
  //    private List<TMTextUnitVariant> tmTextUnitVariants;
  //
  //    @OneToMany(mappedBy = "tmTextUnit")
  //    private List<AssetTextUnitToTMTextUnit> assetTextUnitToTMTextUnits;
  //
  //    @OneToMany(mappedBy = "tmTextUnit")
  //    private List<TMTextUnitCurrentVariant> tmTextUnitCurrentVariants;
  @Query(
      nativeQuery = true,
      value =
          """
          select atu.*
          from asset_text_unit atu
          left join asset_text_unit_to_tm_text_unit map on map.asset_text_unit_id = atu.id
          where map.asset_text_unit_id is NULL and atu.asset_extraction_id = ?1
          """)
  List<AssetTextUnit> getUnmappedAssetTextUnits(Long assetExtractionId);

  @Transactional
  int deleteByAssetExtractionId(Long assetExtractionId);

  List<AssetTextUnit> findByAssetExtractionIdAndName(Long assetExtractionId, String name);

  @EntityGraph(value = "AssetTextUnit.legacy", type = EntityGraphType.FETCH)
  List<AssetTextUnit> findByIdIn(List<Long> assetTextUnitIds);

  void deleteByIdIn(List<Long> assetTextUnitIds);
}
