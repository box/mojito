package com.box.l10n.mojito.service.screenshot;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ScreenshotRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Set;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface ScreenshotRepository extends JpaRepository<Screenshot, Long>, JpaSpecificationExecutor<Screenshot> {

    Screenshot findByScreenshotRunAndNameAndLocale(ScreenshotRun screenshotRun, String name, Locale locale);
}
