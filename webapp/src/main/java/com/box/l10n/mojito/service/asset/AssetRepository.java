package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetRepository
    extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {
  @EntityGraph(value = "Asset.legacy", type = EntityGraphType.FETCH)
  @Override
  Optional<Asset> findById(Long id);

  @EntityGraph(value = "Asset.legacy", type = EntityGraphType.FETCH)
  Asset findByPathAndRepositoryId(String path, Long repositoryId);

  @Query(value = "select a.id from Asset a where a.repository.id = :repositoryId")
  Set<Long> findIdByRepositoryId(@Param("repositoryId") Long repositoryId);

  @EntityGraph(value = "Asset.legacy", type = EntityGraphType.FETCH)
  @Query(
      value =
          "select a.id from Asset a where a.repository.id = :repositoryId and a.deleted = :deleted")
  Set<Long> findIdByRepositoryIdAndDeleted(
      @Param("repositoryId") Long repositoryId, @Param("deleted") Boolean deleted);

  @Query(value = "select a.id from Asset a where a.id IN ?1 and a.virtual = true")
  Set<Long> getVirtualAssetIds(@Param("assetId") Set<Long> assetIds);
}
