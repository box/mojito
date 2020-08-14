package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface AssetExtractionByBranchRepository extends JpaRepository<AssetExtractionByBranch, Long> {

    List<AssetExtractionByBranch> findByAssetAndDeletedFalse(Asset asset);

    int countByAssetAndDeletedFalseAndBranchNot(Asset asset, Branch branch);

    Optional<AssetExtractionByBranch> findByAssetAndBranch(Asset asset, Branch branch);

//    @Lock()
//    AssetExtractionByBranch FindByIdWitLock(Long id);

    @Modifying
    @Query("update AssetExtractionByBranch aea set aea.deleted = true where aea.asset= ?1")
    int setDeletedTrue(Asset asset);
}
