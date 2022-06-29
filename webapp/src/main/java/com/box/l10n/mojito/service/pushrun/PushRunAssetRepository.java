package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRunAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

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
}
