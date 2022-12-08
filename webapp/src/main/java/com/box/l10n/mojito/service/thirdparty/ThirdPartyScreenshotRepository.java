package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.ThirdPartyScreenshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author jeanaurambault */
@RepositoryRestResource(exported = false)
public interface ThirdPartyScreenshotRepository
    extends JpaRepository<ThirdPartyScreenshot, Long>,
        JpaSpecificationExecutor<ThirdPartyScreenshot> {

    List<ThirdPartyScreenshot> findAllByScreenshotId(Long screenshotId);
}
