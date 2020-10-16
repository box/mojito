package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetTextUnitToTMTextUnitRepository extends JpaRepository<AssetTextUnitToTMTextUnit, Long> {

    @Transactional
    int deleteByAssetExtractionId(Long assetExtractionId);

    void deleteByAssetTextUnitId(Long assetTextUnitId);

    void deleteByAssetTextUnitIdIn(List<Long> assetTextUnitIds);

    @Query("select atuttu.tmTextUnit.id from AssetTextUnitToTMTextUnit atuttu "
            + "inner join atuttu.assetExtraction ae "
            + "inner join ae.assetExtractionByBranches aec "
            + "where aec.branch = ?1")
    List<Long> findByBranch(Branch branch);


    @Query("select atuttu.tmTextUnit.id " +
            "from AssetTextUnitToTMTextUnit atuttu " +
            "where atuttu.assetExtraction.id = ?1")
    Set<Long> findTmTextUnitIdsByAssetExtractionId(Long assetExtractionId);

    @Query("select atuttu.tmTextUnit.id " +
            "from AssetTextUnitToTMTextUnit atuttu " +
            "where atuttu.assetExtraction.id = ?1 and atuttu.assetTextUnit.id = ?2")
    Optional<Long> findTmTextUnitId(long assetExtractionId, long assetTextUnitId);

}
