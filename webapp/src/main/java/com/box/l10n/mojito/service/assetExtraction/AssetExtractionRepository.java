package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author aloison */
@RepositoryRestResource(exported = false)
public interface AssetExtractionRepository extends JpaRepository<AssetExtraction, Long> {

  List<AssetExtraction> findByAsset(Asset asset);

  @Query(
      "select ae.id from #{#entityName} ae "
          + "inner join ae.asset a "
          + "inner join ae.pollableTask pt "
          + "left outer join ae.assetExtractionByBranches aea "
          + "where aea.id is null "
          + "and  ae != a.lastSuccessfulAssetExtraction "
          + "and pt.finishedDate is not null")
  List<Long> findFinishedAndOldAssetExtractions(Pageable pageable);
}
