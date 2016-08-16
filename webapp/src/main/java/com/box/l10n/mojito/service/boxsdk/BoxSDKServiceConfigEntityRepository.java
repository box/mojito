package com.box.l10n.mojito.service.boxsdk;

import com.box.l10n.mojito.entity.BoxSDKServiceConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author wyau
 */
@RepositoryRestResource(exported = false)
public interface BoxSDKServiceConfigEntityRepository extends JpaRepository<BoxSDKServiceConfigEntity, Long>, JpaSpecificationExecutor<BoxSDKServiceConfigEntity> {

    BoxSDKServiceConfigEntity findFirstByOrderByIdAsc();

    Long deleteFirstByOrderByIdAsc();
}
