package com.box.l10n.mojito.service.assetTextUnit;

import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.box.l10n.mojito.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetTextUnitRepository extends JpaRepository<AssetTextUnit, Long>, JpaSpecificationExecutor<AssetTextUnit> {

    List<AssetTextUnit> findByAssetExtraction(AssetExtraction assetExtraction);

    @Query("select atu.md5 from #{#entityName} atu where atu.assetExtraction = ?1 and not atu.branch = ?2")
    List<String> findMd5ByAssetExtractionAndBranch(AssetExtraction assetExtraction, Branch branch);

    List<AssetTextUnit> findByMd5NotInAndAssetExtraction(Collection<String> excludedMd5s, AssetExtraction assetExtraction);

    /**
     * Gets unmapped {@link AssetTextUnit}s
     *
     * @param assetExtractionId {@link AssetExtraction} id
     * @return the unmapped {@link AssetTextUnit}s
     */
    //TODO(P1) In fact, it should be possible to write that using HQL with modification to data model
    // Change like this in TMTextUnit
    //    @OneToMany(mappedBy = "tmTextUnit")
    //    private List<TMTextUnitVariant> tmTextUnitVariants;
    //
    //    @OneToMany(mappedBy = "tmTextUnit")
    //    private List<AssetTextUnitToTMTextUnit> assetTextUnitToTMTextUnits;
    //
    //    @OneToMany(mappedBy = "tmTextUnit")
    //    private List<TMTextUnitCurrentVariant> tmTextUnitCurrentVariants;
    @Query(nativeQuery = true, value
            = "select atu.* from asset_text_unit atu left join asset_text_unit_to_tm_text_unit map on map.asset_text_unit_id = atu.id "
            + "where map.asset_text_unit_id is NULL and atu.asset_extraction_id = ?1")
    List<AssetTextUnit> getUnmappedAssetTextUnits(Long assetExtractionId);

    @Transactional
    int deleteByAssetExtractionId(Long assetExtractionId);

    List<AssetTextUnit> findByAssetExtractionIdAndName(Long assetExtractionId, String name);

    List<AssetTextUnit> findByAssetExtractionIdOrderByNameAsc(Long assetExtractionId);

    List<AssetTextUnit> findByIdIn(List<Long> assetTextUnitIds);

}
