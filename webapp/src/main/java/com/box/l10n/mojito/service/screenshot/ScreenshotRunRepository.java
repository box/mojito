package com.box.l10n.mojito.service.screenshot;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ScreenshotRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface ScreenshotRunRepository extends JpaRepository<ScreenshotRun, Long>, JpaSpecificationExecutor<ScreenshotRun> {

    public ScreenshotRun findByRepositoryAndLastSuccessfulRunIsTrue(Repository repository);
    
    public ScreenshotRun findByName(String name);

}
