package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.PushRunAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface PushRunAssetRepository extends JpaRepository<PushRunAsset, Long> {
}
