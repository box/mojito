package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException;
import com.box.l10n.mojito.rest.entity.Branch;
import com.box.l10n.mojito.rest.entity.ImportRepositoryBody;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author wyau
 */
@Component
public class RepositoryClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryClient.class);

    @Override
    public String getEntityName() {
        return "repositories";
    }

    /**
     * Get a {@link Repository} by the {@link Repository#id}
     *
     * @param repositoryId
     * @return
     */
    public Repository getRepositoryById(Long repositoryId) {
        logger.debug("Getting repository by id = [{}]", repositoryId);
        return authenticatedRestTemplate.getForObject(getBasePathForResource(repositoryId), Repository.class);
    }

    /**
     * Get a list of {@link Repository}s matching the given parameters
     *
     * @param repositoryName To filter by name. Can be {@code null}.
     * @return List of {@link Repository}s
     */
    public List<Repository> getRepositories(String repositoryName) {

        Map<String, String> filterParams = new HashMap<>();

        if (repositoryName != null) {
            filterParams.put("name", repositoryName);
        }

        return authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(
                getBasePathForEntity(),
                Repository[].class,
                filterParams);
    }

    /**
     * Gets a {@link Repository} by its name using the API
     *
     * @param repositoryName Name of the repository
     * @return
     * @throws RepositoryNotFoundException
     */
    public Repository getRepositoryByName(String repositoryName) throws RepositoryNotFoundException {
        logger.debug("Getting repo with name = {}", repositoryName);

        List<Repository> repositoryList = getRepositories(repositoryName);

        if (repositoryList.size() != 1) {
            throw new RepositoryNotFoundException("Repository with name [" + repositoryName + "] is not found");
        }

        return repositoryList.get(0);
    }

    /**
     * Create a {@link Repository} using the API
     *
     * @param name
     * @param description
     * @param sourceLocale
     * @param repositoryLocales With id, and repository id not set
     * @param integrityCheckers
     * @return
     * @throws com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException
     */
    public Repository createRepository(String name,
                                       String description,
                                       Locale sourceLocale,
                                       Set<RepositoryLocale> repositoryLocales,
                                       Set<IntegrityChecker> integrityCheckers,
                                       Boolean checkSLA) throws ResourceNotCreatedException {
        logger.debug("Creating repo with name = {}, and description = {}, and repositoryLocales = {}", name, description, repositoryLocales.toString());

        Repository repoToCreate = new Repository();
        repoToCreate.setName(name);
        repoToCreate.setDescription(description);
        repoToCreate.setSourceLocale(sourceLocale);
        repoToCreate.setRepositoryLocales(repositoryLocales);
        repoToCreate.setIntegrityCheckers(integrityCheckers);
        repoToCreate.setCheckSLA(checkSLA);

        try {
            return authenticatedRestTemplate.postForObject(getBasePathForEntity(), repoToCreate, Repository.class);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw new ResourceNotCreatedException(exception.getResponseBodyAsString());
            } else {
                throw exception;
            }
        }
    }

    /**
     *
     * @param repositoryId
     * @param exportedXliffContent
     * @param updateTM indicates if the TM should be updated or if the translation
     * can be imported assuming that there is no translation yet.
     * @return
     * @throws ResourceNotCreatedException
     */
    public void importRepository(Long repositoryId, String exportedXliffContent, boolean updateTM) throws ResourceNotCreatedException {

        String pathToImport = getBasePathForResource(repositoryId, "xliffImport");
        
        ImportRepositoryBody importRepositoryBody = new ImportRepositoryBody();
        importRepositoryBody.setXliffContent(exportedXliffContent);
        importRepositoryBody.setUpdateTM(updateTM);

        try {
            authenticatedRestTemplate.postForEntity(pathToImport, importRepositoryBody, Void.class);

        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw new ResourceNotCreatedException("Importing to repository [" + repositoryId + "] failed");
            } else {
                throw exception;
            }
        }
    }
    
    /**
     * Deletes a {@link Repository} by the {@link Repository#name}
     *
     * @param repositoryName
     * @throws com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException
     */
    public void deleteRepositoryByName(String repositoryName) throws RepositoryNotFoundException {
        logger.debug("Deleting repository by name = [{}]", repositoryName);
        Repository repository = getRepositoryByName(repositoryName);
        authenticatedRestTemplate.delete(getBasePathForResource(repository.getId()));
    }

    /**
     * Updates a {@link Repository}
     * 
     * @param name
     * @param newName
     * @param description
     * @param repositoryLocales With id, and repository id not set
     * @param integrityCheckers
     * @throws
     * com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException
     * @throws com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException
     */
    public void updateRepository(String name, String newName, String description, Boolean checkSLA, Set<RepositoryLocale> repositoryLocales, Set<IntegrityChecker> integrityCheckers) throws RepositoryNotFoundException, ResourceNotUpdatedException {
        
        logger.debug("Updating repository by name = [{}]", name);
        Repository repository = getRepositoryByName(name);

        repository.setDescription(description);
        repository.setName(newName);
        repository.setRepositoryLocales(repositoryLocales);
        repository.setCheckSLA(checkSLA);
        if (integrityCheckers != null) {
            repository.setIntegrityCheckers(integrityCheckers);
        }

        try {
            authenticatedRestTemplate.patch(getBasePathForResource(repository.getId()), repository);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw new ResourceNotUpdatedException(exception.getResponseBodyAsString());
            } else {
                throw exception;
            }
        }

    }

    public List<Branch> getBranches(Long repositoryId, String branchName) {
        Map<String, String> filterParams = new HashMap<>();

        if (branchName != null) {
            filterParams.put("name", branchName);
        }

        return authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(
                getBasePathForResource(repositoryId, "branches"),
                Branch[].class,
                filterParams);
    }

    public Branch getBranch(Long repositoryId, String branchName) {
        return getBranches(repositoryId, branchName).get(0);
    }

}
