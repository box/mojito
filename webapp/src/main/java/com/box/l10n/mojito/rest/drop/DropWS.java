package com.box.l10n.mojito.rest.drop;

import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.PageView;
import com.box.l10n.mojito.rest.View;
import static com.box.l10n.mojito.rest.drop.DropSpecification.isCanceled;
import static com.box.l10n.mojito.rest.drop.DropSpecification.isImported;
import static com.box.l10n.mojito.rest.drop.DropSpecification.repositoryIdEquals;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.drop.DropRepository;
import com.box.l10n.mojito.service.drop.DropService;
import com.box.l10n.mojito.service.drop.ExportDropConfig;
import com.box.l10n.mojito.service.drop.exporter.DropExporterException;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.UpdateTMWithXLIFFResult;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import static org.springframework.data.jpa.domain.Specifications.where;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author aloison
 */
@RestController
public class DropWS {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropWS.class);

    @Autowired
    DropService dropService;

    @Autowired
    DropRepository dropRepository;

    @Autowired
    TMService tmService;

    @Autowired
    RepositoryRepository repositoryRepository;

    /**
     * Lists all the {@link Drop}s with pagination.
     * It uses View.DropSummary.class to return minimum fields 
     * that are annotated with @JsonView(View.DropSummary.class).
     *
     * @param repositoryId optionally filter by {@link Repository#id}
     * @param importedFilter optionally filter Drops that have been imported (
     * {@code null} no filter on imported status, {@code true} get only imported
     * {@code false} get only not imported)
     * @param canceledFilter optionally filter by Drops that have been canceled (
     * {@code null} no filter on canceled status, {@code true} get only canceled
     * {@code false} get only not canceled)
     * @param pageable pagination information
     * @return list of {@link Drop}
     * @throws Exception
     */
    @JsonView(View.DropSummary.class)
    @RequestMapping(method = RequestMethod.GET, value = "/api/drops")
    public Page<Drop> getDrops(
            @RequestParam(value = "repositoryId", required = false) Long repositoryId,
            @RequestParam(value = "imported", required = false) Boolean importedFilter,
            @RequestParam(value = "canceled", required = false) Boolean canceledFilter,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {

        Page<Drop> findAll = dropRepository.findAll(where(
                ifParamNotNull(repositoryIdEquals(repositoryId))).and(
                ifParamNotNull(isImported(importedFilter))).and(
                ifParamNotNull(isCanceled(canceledFilter))
        ),
                pageable
        );

        return new PageView<>(findAll);
    }

    /**
     * WS to start exporting a drop
     *
     * @param exportDropConfig contains information about the drop to be
     * exported
     * @return The information about the exported drop, updated with a pollable
     * task
     * @throws DropExporterException
     *
     */
    @RequestMapping(method = RequestMethod.POST, value = "/api/drops/export")
    public ExportDropConfig exportDrop(@RequestBody ExportDropConfig exportDropConfig) throws DropExporterException {

        // TODO(P1) Check here that the repo exists (and the user has access to it)?
        PollableFuture<Drop> exportDropFuture = dropService.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        exportDropConfig.setPollableTask(exportDropFuture.getPollableTask());

        try {
            exportDropConfig.setDropId(exportDropFuture.get().getId());
        } catch (ExecutionException | InterruptedException e) {
            //TODO(P1) Should this be a hard failure or be processed by the client through the pollable?
            //wondering how the FE will react currently
            logger.debug("Cannot get dropId from the pollable due to exception", e);
        }

        return exportDropConfig;
    }

    /**
     * WS to start re-importing a drop
     *
     * @param importDropConfig contains information about the drop to be
     * re-imported
     * @return The information about the re-imported drop, updated with a
     * pollable task
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST, value = "/api/drops/import")
    public ImportDropConfig importDrop(@RequestBody ImportDropConfig importDropConfig) throws Exception {

        // TODO(P1) Check here that the repo exists (and the user has access to it)?
        PollableFuture importDropFuture = dropService.importDrop(importDropConfig.getDropId(), importDropConfig.getStatus(), PollableTask.INJECT_CURRENT_TASK);

        importDropConfig.setPollableTask(importDropFuture.getPollableTask());

        return importDropConfig;
    }

    /**
     * WS to cancel a drop.
     *
     * @param cancelDropConfig contains information about the drop to be
     * re-imported
     * @return The information about the re-imported drop, updated with a
     * pollable task
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST, value = "/api/drops/cancel")
    public CancelDropConfig cancelDrop(@RequestBody CancelDropConfig cancelDropConfig) throws Exception {

        // TODO(P1) Check here that the repo exists (and the user has access to it)?
        PollableFuture cancelDropFuture = dropService.cancelDrop(cancelDropConfig.getDropId(), PollableTask.INJECT_CURRENT_TASK);

        cancelDropConfig.setPollableTask(cancelDropFuture.getPollableTask());

        return cancelDropConfig;
    }

    /**
     * WS to force complete a partially imported drop
     *
     * @param dropId
     * @throws DropWithIdNotFoundException
     */
    @RequestMapping(value = "/api/drops/complete/{dropId}", method = RequestMethod.POST)
    public void completeDropById(@PathVariable Long dropId) throws DropWithIdNotFoundException {
        Drop drop = dropRepository.findOne(dropId);

        if (drop == null) {
            throw new DropWithIdNotFoundException(dropId);
        }

        dropService.completeDrop(drop);

    }

    /**
     * Allows to import an XLIFF originating from a Drop but in an independent
     * way. It can be used to import modified XLIFF for drops that are not in
     * the system anymore or when the normal drop import logic is too heavy.
     *
     * This service doesn't need a drop id but instead needs a repository to
     * perform the operation. We keep the entry point here to express the
     * relationship with the drop.
     *
     * TODO(P1) we need this for now but not sure we should keep it later. or
     * maybe it should be refactor/merged into tm-import function. In any case
     * it should be pollable
     *
     * @param importXliffBody 
     * @return the imported XLIFF with information for each text unit about the
     * import process.
     *
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST, value = "/api/drops/importXliff")
    public ImportXliffBody importXliff(@RequestBody ImportXliffBody importXliffBody) throws Exception {

        Repository repository = repositoryRepository.findOne(importXliffBody.getRepositoryId());

        String normalizedContent = NormalizationUtils.normalize(importXliffBody.getXliffContent());
        
        UpdateTMWithXLIFFResult updateTMWithLocalizedXLIFF;

        if (importXliffBody.isTranslationKit()) {
            updateTMWithLocalizedXLIFF = tmService.updateTMWithTranslationKitXLIFF(normalizedContent, importXliffBody.getImportStatus());
        } else {
            updateTMWithLocalizedXLIFF = tmService.updateTMWithXLIFFByMd5(normalizedContent, importXliffBody.getImportStatus(), repository);
        }

        importXliffBody.setXliffContent(updateTMWithLocalizedXLIFF.getXliffContent());
        
        return importXliffBody; 
    }

}
