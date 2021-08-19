package com.box.l10n.mojito.service.screenshot;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ScreenshotRun;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface ScreenshotRunRepository extends JpaRepository<ScreenshotRun, Long>, JpaSpecificationExecutor<ScreenshotRun> {

    @EntityGraph(value = "ScreenshotRunGraph", type = EntityGraph.EntityGraphType.LOAD)
    public ScreenshotRun findByRepositoryAndLastSuccessfulRunIsTrue(Repository repository);

    @EntityGraph(value = "ScreenshotRunGraph", type = EntityGraph.EntityGraphType.LOAD)
    public ScreenshotRun findByName(String name);

    @EntityGraph(value = "ScreenshotRunGraph", type = EntityGraph.EntityGraphType.LOAD)
    public Optional<ScreenshotRun> findById(Long id);
}
