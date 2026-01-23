package com.box.l10n.mojito.service.assetintegritychecker;

import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.repository.AssetIntegrityCheckerRow;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetIntegrityCheckerRepository
    extends JpaRepository<AssetIntegrityChecker, Long> {

  Set<AssetIntegrityChecker> findByRepository(Repository repository);

  Set<AssetIntegrityChecker> findByRepositoryAndAssetExtension(
      Repository repository, String assetExtension);

  void deleteByRepository(Repository repository);

  @Query(
      """
      select new com.box.l10n.mojito.service.repository.AssetIntegrityCheckerRow(
        aic.id,
        aic.repository.id,
        aic.assetExtension,
        aic.integrityCheckerType
      )
      from AssetIntegrityChecker aic
      where aic.repository.id in :repositoryIds
      order by aic.id asc
      """)
  List<AssetIntegrityCheckerRow> findRowsByRepositoryIdIn(
      @Param("repositoryIds") List<Long> repositoryIds);
}
