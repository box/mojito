package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface AssetExtractionByBranchRepository extends JpaRepository<AssetExtractionByBranch, Long> {

    List<AssetExtractionByBranch> findByAssetAndDeletedFalse(Asset asset);

    int countByAssetAndDeletedFalseAndBranchNot(Asset asset, Branch branch);

    AssetExtractionByBranch findByAssetAndBranch(Asset asset, Branch branch);

    @Modifying
    @Query("update AssetExtractionByBranch aea set aea.deleted = true where aea.asset= ?1")
    int setDeletedTrue(Asset asset);
}
