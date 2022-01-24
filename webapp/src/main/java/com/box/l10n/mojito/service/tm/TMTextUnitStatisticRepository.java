package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitStatisticRepository  extends JpaRepository<TMTextUnitStatistic, Long> {
    @Query("select tus from TMTextUnitStatistic tus join fetch tus.tmTextUnit tmTextUnit join fetch tmTextUnit.asset asset where asset.id = :assetId")
    List<TMTextUnitStatistic> findByAsset(@Param("assetId") Long assetId);
}
