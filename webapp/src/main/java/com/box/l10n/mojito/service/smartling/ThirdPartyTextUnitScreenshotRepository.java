package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.ThirdPartyTextUnitScreenshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ThirdPartyTextUnitScreenshotRepository extends JpaRepository<ThirdPartyTextUnitScreenshot, Long>, JpaSpecificationExecutor<ThirdPartyTextUnitScreenshot> {

    ThirdPartyTextUnitScreenshot findByThirdPartyTextUnitId(String thirdPartyTextUnitId);

}
