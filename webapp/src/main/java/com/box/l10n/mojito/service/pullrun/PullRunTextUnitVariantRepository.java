package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PullRunTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface PullRunTextUnitVariantRepository extends JpaRepository<PullRunTextUnitVariant, Long> {

    List<PullRunTextUnitVariant> findByPullRunAsset(PullRunAsset pullRunAsset, Pageable pageable);

    @Query("select prtuv.tmTextUnitVariant from PullRunTextUnitVariant prtuv " +
            "inner join prtuv.pullRunAsset pra " +
            "inner join pra.pullRun pr where pr = :pullRun")
    List<TMTextUnitVariant> findByPullRun(@Param("pullRun") PullRun pullRun, Pageable pageable);

    @Transactional
    void deleteByPullRunAsset(PullRunAsset pullRunAsset);

    @Modifying
    @Query(value = "insert into pull_run_text_unit_variant (pull_run_asset_id, tm_text_unit_variant_id) values (?1, ?2)", nativeQuery = true)
    void saveAllNative(List<Object> tuvIds);
}
