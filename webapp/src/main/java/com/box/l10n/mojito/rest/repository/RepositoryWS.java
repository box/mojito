package com.box.l10n.mojito.rest.repository;

import static com.box.l10n.mojito.rest.repository.BranchSpecification.branchStatisticTranslated;
import static com.box.l10n.mojito.rest.repository.BranchSpecification.createdBefore;
import static com.box.l10n.mojito.rest.repository.BranchSpecification.deletedEquals;
import static com.box.l10n.mojito.rest.repository.BranchSpecification.nameEquals;
import static com.box.l10n.mojito.rest.repository.BranchSpecification.repositoryEquals;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.data.jpa.domain.Specification.where;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMImportService;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

  /** logger */
  static Logger logger = getLogger(RepositoryWS.class);

  @Autowired TMImportService tmImportService;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired AssetRepository assetRepository;

  @Autowired RepositoryService repositoryService;

  @Autowired BranchRepository branchRepository;

  @Autowired BranchService branchService;

  @JsonView(View.Repository.class)
  @RequestMapping(
      value = "/api/repositories/{repositoryId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Repository getRepositoryById(@PathVariable Long repositoryId)
      throws RepositoryWithIdNotFoundException {
    Repository repository = repositoryRepository.findById(repositoryId).orElse(null);

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
  @RequestMapping(
      value = "/api/repositories",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public List<Repository> getRepositories() {
    return repositoryService.findRepositoriesIsNotDeletedOrderByName(null);
  }

  /**
   * Gets repository matching the given name
   *
   * @param repositoryName To filer on the name. Can be {@code null}
   * @return List of {@link Repository}s
   */
  @JsonView(View.Repository.class)
  @RequestMapping(
      value = "/api/repositories",
      params = "name",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public List<Repository> getRepositories(
      @RequestParam(value = "name", required = true) String repositoryName) {
    return repositoryService.findRepositoriesIsNotDeletedOrderByName(repositoryName);
  }

  /**
   * Creates a new {@link Repository}. Will return {@link HttpStatus#CREATED} if all is successful.
   * Will return {@link HttpStatus#CONFLICT} if there's already a repository with the same name.
   *
   * @param repository
   * @return
   */
  @JsonView(View.Repository.class)
  @RequestMapping(
      value = "/api/repositories",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Repository> createRepository(@RequestBody Repository repository) {
    logger.info("Creating repository");

    ResponseEntity<Repository> result;

    try {
      Repository createdRepo =
          repositoryService.createRepository(
              repository.getName(),
              repository.getDescription(),
              repository.getSourceLocale(),
              repository.getCheckSLA(),
              repository.getAssetIntegrityCheckers(),
              repository.getRepositoryLocales());
      result = new ResponseEntity<>(createdRepo, HttpStatus.CREATED);
    } catch (RepositoryNameAlreadyUsedException e) {
      logger.debug("Cannot create the repository", e);
      result =
          new ResponseEntity(
              "Repository with name [" + repository.getName() + "] already exists",
              HttpStatus.CONFLICT);
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
  @Operation(summary = "Import an entire Repository given an XLIFF")
  @RequestMapping(
      value = "/api/repositories/{repositoryId}/xliffImport",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity importRepository(
      @PathVariable Long repositoryId, @RequestBody ImportRepositoryBody importRepositoryBody) {

    logger.info("Importing for repo: [{}]", repositoryId);

    try {
      String normalizedContent =
          NormalizationUtils.normalize(importRepositoryBody.getXliffContent());
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
  public void deleteRepositoryById(@PathVariable Long repositoryId)
      throws RepositoryWithIdNotFoundException {
    logger.info("Deleting repository [{}]", repositoryId);
    Repository repository = repositoryRepository.findById(repositoryId).orElse(null);

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
  @RequestMapping(
      value = "/api/repositories/{repositoryId}",
      method = RequestMethod.PATCH,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity updateRepository(
      @PathVariable Long repositoryId, @RequestBody Repository repository)
      throws RepositoryWithIdNotFoundException {
    logger.info("Updating repository [{}]", repositoryId);
    ResponseEntity result;
    Repository repoToUpdate = repositoryRepository.findById(repositoryId).orElse(null);

    if (repoToUpdate == null) {
      throw new RepositoryWithIdNotFoundException(repositoryId);
    }

    try {
      repositoryService.updateRepository(
          repoToUpdate,
          repository.getName(),
          repository.getDescription(),
          repository.getCheckSLA(),
          repository.getRepositoryLocales(),
          repository.getAssetIntegrityCheckers());

      result = new ResponseEntity(HttpStatus.OK);

    } catch (RepositoryNameAlreadyUsedException e) {
      logger.debug("Cannot create the repository", e);
      result =
          new ResponseEntity(
              "Repository with name [" + repository.getName() + "] already exists",
              HttpStatus.CONFLICT);
    } catch (RepositoryLocaleCreationException e) {
      logger.debug("Cannot create the repository", e);
      result = new ResponseEntity(e.getMessage(), HttpStatus.CONFLICT);
    }

    return result;
  }

  @JsonView(View.BranchSummary.class)
  @RequestMapping(
      value = "/api/repositories/{repositoryId}/branches",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public List<Branch> getBranchesOfRepository(
      @PathVariable Long repositoryId,
      @RequestParam(value = "name", required = false) String branchName,
      @RequestParam(value = "deleted", required = false) Boolean deleted,
      @RequestParam(value = "translated", required = false) Boolean translated,
      @RequestParam(value = "createdBefore", required = false) ZonedDateTime createdBefore)
      throws RepositoryWithIdNotFoundException {

    Repository repository = repositoryRepository.findById(repositoryId).orElse(null);

    if (repository == null) {
      throw new RepositoryWithIdNotFoundException(repositoryId);
    }

    List<Branch> branches =
        branchRepository.findAll(
            where(ifParamNotNull(nameEquals(branchName)))
                .and(ifParamNotNull(repositoryEquals(repository)))
                .and(ifParamNotNull(deletedEquals(deleted)))
                .and(ifParamNotNull(branchStatisticTranslated(translated)))
                .and(ifParamNotNull((createdBefore(createdBefore)))));

    return branches;
  }

  @Operation(summary = "Delete a Branch asynchronously")
  @RequestMapping(
      value = "/api/repositories/{repositoryId}/branches",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public PollableTask deleteBranch(
      @PathVariable(value = "repositoryId") Long repositoryId,
      @RequestParam(value = "branchId") Long branchId) {

    logger.debug("Deleting branch {} in repository {}", repositoryId, branchId);
    PollableFuture pollableFuture = branchService.asyncDeleteBranch(repositoryId, branchId);
    return pollableFuture.getPollableTask();
  }
}
