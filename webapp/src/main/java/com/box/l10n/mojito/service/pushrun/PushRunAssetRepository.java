package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRunAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface PushRunAssetRepository extends JpaRepository<PushRunAsset, Long> {
    List<PushRunAsset> findByPushRun(PushRun pushRun);

    Optional<PushRunAsset> findByPushRunAndAsset(PushRun pushRun, Asset asset);

    @Transactional
    void deleteByPushRun(PushRun pushRun);

    @Transactional
    @Modifying
    @Query(nativeQuery = true,
            value = "delete pra " +
                    "from push_run pr " +
                    "join push_run_asset pra on pra.push_run_id = pr.id " +
                    "where pr.created_date < :beforeDate ")
    void deleteAllByPushRunWithCreatedDateBefore(@Param("beforeDate") Timestamp beforeDate);
}
