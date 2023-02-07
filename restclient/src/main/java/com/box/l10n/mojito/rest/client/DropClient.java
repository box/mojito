package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.CancelDropConfig;
import com.box.l10n.mojito.rest.entity.ExportDropConfig;
import com.box.l10n.mojito.rest.entity.ImportDropConfig;
import com.box.l10n.mojito.rest.entity.ImportXliffBody;
import com.box.l10n.mojito.rest.entity.Page;
import com.box.l10n.mojito.rest.entity.Repository;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author jaurambault
 */
@Component
public class DropClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropClient.class);

    @Override
    public String getEntityName() {
        return "drops";
    }

    /**
     * Gets a list of {@link Drop}s with pagination.
     *
     * @param repositoryId optionally filter by {@link Repository#id}
     * @param importedFilter optionally filter Drops that have been imported (
     * {@code null} no filter on imported status, {@code true} get only imported
     * {@code false} get only not imported)
     * @param page page to be retrieve (can be null, no page query string will
     * be set)
     * @param size size of the page to retrieve (can be null, no size query
     * string will be set)
     * @return list of {@link Drop}s
     */
    public Page<Drop> getDrops(Long repositoryId, Boolean importedFilter, Long page, Long size) {

        Map<String, String> params = new HashMap<>();

        if (repositoryId != null) {
            params.put("repositoryId", repositoryId.toString());
        }

        if (importedFilter != null) {
            params.put("imported", importedFilter.toString());
        }

        if (page != null) {
            params.put("page", page.toString());
        }

        if (size != null) {
            params.put("size", size.toString());
        }
        
        ResponseEntity<Page<Drop>> responseEntity = authenticatedRestTemplate.getForEntityWithQueryParams(
                getBasePathForEntity(),
                new ParameterizedTypeReference<Page<Drop>>(){},
                params);
        
        
        return responseEntity.getBody();
    }

    /**
     * Exports a drop given a config.
     *
     * @param exportDropConfig
     * @return {@link ExportDropConfig} that contains information about the drop
     * being created
     */
    public ExportDropConfig exportDrop(ExportDropConfig exportDropConfig) {

        String exportPath = UriComponentsBuilder
                .fromPath(getBasePathForEntity())
                .pathSegment("export")
                .toUriString();

        return authenticatedRestTemplate.postForObject(
                exportPath,
                exportDropConfig,
                ExportDropConfig.class);
    }

    /**
     * Imports a drop for a given {@link Repository} and Drop
     *
     * @param repository the repository the drop will be imported into
     * @param dropId the drop ID to be imported
     * @param status (optional) specific status to use when importing
     * translation
     * @return {@link ImportDropConfig} that contains information about the drop
     * being created
     */
    public ImportDropConfig importDrop(Repository repository, Long dropId, ImportDropConfig.Status status) {

        ImportDropConfig importDropConfig = new ImportDropConfig();
        importDropConfig.setRepositoryId(repository.getId());
        importDropConfig.setDropId(dropId);
        importDropConfig.setStatus(status);

        String importPath = UriComponentsBuilder
                .fromPath(getBasePathForEntity())
                .pathSegment("import")
                .toUriString();

        return authenticatedRestTemplate.postForObject(
                importPath,
                importDropConfig,
                ImportDropConfig.class);
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
     * maybe it should be refactor/merged into tm-import function.
     *
     * @param xliffContent
     * @param repositoryId
     * @param isTranslationKit if the XLIFF is linked to an existing Kit, in
     * which case the translation kit meta information will be updated as well
     * @param importStatus to specify a STATUS to be used when importing the
     * translations
     *
     * @return the Imported XLIFF
     */
    public String importXiff(String xliffContent, Long repositoryId, boolean isTranslationKit, ImportDropConfig.Status importStatus) {

        String importXliffPath = UriComponentsBuilder
                .fromPath(getBasePathForEntity())
                .pathSegment("importXliff")
                .toUriString();

        ImportXliffBody importXliffBody = new ImportXliffBody();

        importXliffBody.setRepositoryId(Preconditions.checkNotNull(repositoryId));
        importXliffBody.setTranslationKit(isTranslationKit);
        importXliffBody.setImportStatus(importStatus);
        importXliffBody.setXliffContent(xliffContent);

        return authenticatedRestTemplate.postForObject(
                importXliffPath,
                importXliffBody,
                ImportXliffBody.class).getXliffContent();
    }

    /**
     * Cancels a drop for a given drop ID
     *
     * @param dropId the drop ID to be cancelled
     * @return {@link CancelDropConfig} that contains information about the drop
     * being cancelled
     */
    public CancelDropConfig cancelDrop(Long dropId) {

        CancelDropConfig cancelDropConfig = new CancelDropConfig();
        cancelDropConfig.setDropId(dropId);

        String cancelPath = UriComponentsBuilder
                .fromPath(getBasePathForEntity())
                .pathSegment("cancel")
                .toUriString();

        return authenticatedRestTemplate.postForObject(
                cancelPath,
                cancelDropConfig,
                CancelDropConfig.class);
    }
}
