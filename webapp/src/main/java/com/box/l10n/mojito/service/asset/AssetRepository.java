package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author aloison */
@RepositoryRestResource(exported = false)
public interface AssetRepository
    extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

  Asset findByPathAndRepositoryId(String path, Long repositoryId);

  Asset findTopByRepositoryOrderByLastModifiedDateDesc(Repository repository);

  List<Asset> findByRepositoryIdOrderByLastModifiedDateDesc(Long repositoryId);

  @Query(value = "select a.id from Asset a where a.repository.id = :repositoryId")
  Set<Long> findIdByRepositoryId(@Param("repositoryId") Long repositoryId);

  @Query(
      value =
          "select a.id from Asset a where a.repository.id = :repositoryId and a.deleted = :deleted")
  Set<Long> findIdByRepositoryIdAndDeleted(
      @Param("repositoryId") Long repositoryId, @Param("deleted") Boolean deleted);

  @Query(value = "select a.id from Asset a where a.id IN ?1 and a.virtual = true")
  Set<Long> getVirtualAssetIds(@Param("assetId") Set<Long> assetIds);
}
