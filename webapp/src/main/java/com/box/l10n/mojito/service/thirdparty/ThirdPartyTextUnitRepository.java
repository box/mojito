package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface ThirdPartyTextUnitRepository
    extends JpaRepository<ThirdPartyTextUnit, Long>, JpaSpecificationExecutor<ThirdPartyTextUnit> {

  @Query("select tptu.tmTextUnit.id from #{#entityName} tptu where tptu.asset = ?1")
  HashSet<Long> findTmTextUnitIdsByAsset(Asset asset);

  ThirdPartyTextUnit findByTmTextUnit(TMTextUnit tmTextUnit);

  List<ThirdPartyTextUnit> findByTmTextUnitIdIn(Collection<Long> TmTextUnitIdList);

  @VisibleForTesting
  @Override
  @EntityGraph(value = "ThirdPartyTextUnit.legacy", type = EntityGraphType.FETCH)
  List<ThirdPartyTextUnit> findAll();

  @Transactional
  int deleteByAssetId(@Param("assetId") long assetId);
}
