package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.View;
import static com.box.l10n.mojito.rest.repository.RepositorySpecification.deletedEquals;
import static com.box.l10n.mojito.rest.repository.RepositorySpecification.nameEquals;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMImportService;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import static org.springframework.data.jpa.domain.Specifications.where;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wyau
 */
@RestController
public class RepositoryWS {

    /**
     * logger
     */
    static Logger logger = getLogger(RepositoryWS.class);

    @Autowired
    TMImportService tmImportService;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    RepositoryService repositoryService;

    @RequestMapping(value = "/api/repositories/{repositoryId}", method = RequestMethod.GET)
    public Repository getRepositoryById(@PathVariable Long repositoryId) throws RepositoryWithIdNotFoundException {
        ResponseEntity<Repository> result;
        Repository repository = repositoryRepository.findOne(repositoryId);

        if (repository == null) {
            throw new RepositoryWithIdNotFoundException(repositoryId);
        }

        return repository;
    }

    /**
     * Gets all undeleted repositories with @{link View.RepositorySummary}
     *
     * @return List of {@link Repository}s
     */
    @JsonView(View.RepositorySummary.class)
    @RequestMapping(value = "/api/repositories", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public List<Repository> getRepositories() {
        return repositoryRepository.findAll(
                where(deletedEquals(false)),
                new Sort(Sort.Direction.ASC, "name")
        );
    }

    /**
     * Gets repository matching the given name
     *
     * @param repositoryName To filer on the name. Can be {@code null}
     * @return List of {@link Repository}s
     */
    @RequestMapping(value = "/api/repositories", params = "name", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public List<Repository> getRepositories(@RequestParam(value = "name", required = true) String repositoryName) {
        return repositoryRepository.findAll(
                where(ifParamNotNull(nameEquals(repositoryName)))
                .and(deletedEquals(false)),
                new Sort(Sort.Direction.ASC, "name")
        );
    }

    /**
     * Creates a new {@link Repository}. Will return {@link HttpStatus#CREATED}
     * if all is successful. Will return {@link HttpStatus#CONFLICT} if there's
     * already a repository with the same name.
     *
     * @param repository
     * @return
     */
    @RequestMapping(value = "/api/repositories", method = RequestMethod.POST)
    public ResponseEntity<Repository> createRepository(@RequestBody Repository repository) {
        logger.info("Creating repository");

        ResponseEntity<Repository> result;

        try {
            Repository createdRepo = repositoryService.createRepository(
                    repository.getName(),
                    repository.getDescription(),
                    repository.getRepositoryLocales(),
                    repository.getAssetIntegrityCheckers()
            );
            result = new ResponseEntity<>(createdRepo, HttpStatus.CREATED);
        } catch (RepositoryNameAlreadyUsedException e) {
            logger.debug("Cannot create the repository", e);
            result = new ResponseEntity("Repository with name [" + repository.getName() + "] already exists", HttpStatus.CONFLICT);
        } catch (RepositoryLocaleCreationException e) {
            logger.debug("Cannot create the repository", e);
            result = new ResponseEntity(e.getMessage(), HttpStatus.CONFLICT);
        }

        return result;
    }

    /**
     * Imports an entire {@link Repository} given an XLIFF
     *
     * @param repositoryId
     * @param importRepositoryBody
     * @return
     */
    @RequestMapping(value = "/api/repositories/{repositoryId}/xliffImport", method = RequestMethod.POST)
    public ResponseEntity importRepository(
            @PathVariable Long repositoryId,
            @RequestBody ImportRepositoryBody importRepositoryBody) {

        logger.info("Importing for repo: [{}]", repositoryId);

        try {
            String normalizedContent = NormalizationUtils.normalize(importRepositoryBody.getXliffContent());
            tmImportService.importXLIFF(
                    repositoryRepository.getOne(repositoryId),
                    normalizedContent,
                    importRepositoryBody.isUpdateTM());

            return new ResponseEntity(HttpStatus.CREATED);
        } catch (RuntimeException exception) {
            String msg = "Unable to import: " + exception.getMessage();
            logger.debug(msg, exception);
            return new ResponseEntity(msg, HttpStatus.CONFLICT);
        }
    }


    /**
     * Deletes the repository by the {@link Repository#id}
     *
     * @param repositoryId
     * @return
     */
    @RequestMapping(value = "/api/repositories/{repositoryId}", method = RequestMethod.DELETE)
    public void deleteRepositoryById(@PathVariable Long repositoryId) throws RepositoryWithIdNotFoundException {
        logger.info("Deleting repository [{}]", repositoryId);
        Repository repository = repositoryRepository.findOne(repositoryId);

        if (repository == null) {
            throw new RepositoryWithIdNotFoundException(repositoryId);
        }

        repositoryService.deleteRepository(repository);
    }

    /**
     * Updates repository by the {@link Repository#id}
     *
     * @param repositoryId
     * @param repository
     * @return
     */
    @RequestMapping(value = "/api/repositories/{repositoryId}", method = RequestMethod.PATCH)
    public ResponseEntity updateRepository(@PathVariable Long repositoryId,
            @RequestBody Repository repository) throws RepositoryWithIdNotFoundException {
        logger.info("Updating repository [{}]", repositoryId);
        ResponseEntity result;
        Repository repoToUpdate = repositoryRepository.findOne(repositoryId);
        
        if (repoToUpdate == null) {
            throw new RepositoryWithIdNotFoundException(repositoryId);
        }

        try {
            repositoryService.updateRepository(repoToUpdate,
                        repository.getName(),
                        repository.getDescription(),
                        repository.getRepositoryLocales(),
                        repository.getAssetIntegrityCheckers());
            
            result = new ResponseEntity(HttpStatus.OK);
 
        } catch (RepositoryNameAlreadyUsedException e) {
            logger.debug("Cannot create the repository", e);
            result = new ResponseEntity("Repository with name [" + repository.getName() + "] already exists", HttpStatus.CONFLICT);
        } catch (RepositoryLocaleCreationException e) {
            logger.debug("Cannot create the repository", e);
            result = new ResponseEntity(e.getMessage(), HttpStatus.CONFLICT);
        }

        return result;
    }

}
