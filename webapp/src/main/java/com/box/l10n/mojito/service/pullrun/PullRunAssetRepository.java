package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRunAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface PullRunAssetRepository extends JpaRepository<PullRunAsset, Long> {
}
