package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface PullRunAssetRepository extends JpaRepository<PullRunAsset, Long> {
    /**
     * TODO(jean) should this be rename as findAllBy
     */
    List<PullRunAsset> findByPullRun(PullRun pullRun);


    Optional<PullRunAsset> findByPullRunAndAsset(PullRun pullRun, Asset asset);

    @Transactional
    void deleteByPullRun(PullRun pullRun);
}
