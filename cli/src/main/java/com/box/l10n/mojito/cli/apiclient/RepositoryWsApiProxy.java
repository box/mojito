package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.model.BranchBranchSummary;
import com.box.l10n.mojito.cli.model.ImportRepositoryBody;
import com.box.l10n.mojito.cli.model.PollableTask;
import com.box.l10n.mojito.cli.model.Repository;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class RepositoryWsApiProxy {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryWsApiProxy.class);

  @Autowired private RepositoryWsApi repositoryClient;

  public List<BranchBranchSummary> getBranchesOfRepository(
      Long repositoryId,
      String branchName,
      String branchNameRegex,
      Boolean deleted,
      Boolean translated,
      boolean includeNullBranch,
      OffsetDateTime createdBefore)
      throws CommandException {
    List<BranchBranchSummary> branches =
        this.repositoryClient.getBranchesOfRepository(
            repositoryId, branchName, deleted, translated, createdBefore);
    if (branchNameRegex != null) {
      Pattern branchNamePattern = Pattern.compile(branchNameRegex);
      branches =
          branches.stream()
              .filter(
                  b -> {
                    if (b.getName() == null) {
                      return includeNullBranch;
                    } else {
                      return branchNamePattern.matcher(b.getName()).matches();
                    }
                  })
              .toList();
    }
    return branches;
  }

  public RepositoryRepository getRepositoryByName(String repositoryName)
      throws RepositoryNotFoundException {
    logger.debug("Getting repo with name = {}", repositoryName);

    List<RepositoryRepository> repositoryList =
        this.repositoryClient.getRepositories(repositoryName);

    if (repositoryList.size() != 1) {
      throw new RepositoryNotFoundException(
          "Repository with name [" + repositoryName + "] is not found");
    }

    return repositoryList.getFirst();
  }

  /**
   * Deletes a {@link RepositoryRepository} by the {@link RepositoryRepository#getName()}
   *
   * @param repositoryName
   */
  public void deleteRepositoryByName(String repositoryName) throws RepositoryNotFoundException {
    logger.debug("Deleting repository by name = [{}]", repositoryName);
    RepositoryRepository repository = this.getRepositoryByName(repositoryName);
    this.repositoryClient.deleteRepositoryById(repository.getId());
  }

  public BranchBranchSummary getBranch(Long repositoryId, String branchName) {
    List<BranchBranchSummary> branches =
        this.getBranchesOfRepository(repositoryId, branchName, null, null, null, false, null);
    logger.debug("Support the \"null\" branch (name is null and param filtering doesn't work)");
    return branches.stream()
        .filter((b) -> Objects.equals(b.getName(), branchName))
        .findFirst()
        .orElse(null);
  }

  public RepositoryRepository createRepository(Repository body) throws ResourceNotCreatedException {
    logger.debug(
        "Creating repo with name = {}, and description = {}, and repositoryLocales = {}",
        body.getName(),
        body.getDescription(),
        body.getRepositoryLocales().toString());
    try {
      return this.repositoryClient.createRepository(body);
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new ResourceNotCreatedException(exception.getResponseBodyAsString());
      } else {
        throw exception;
      }
    }
  }

  public String importRepository(ImportRepositoryBody body, Long repositoryId)
      throws ResourceNotCreatedException {
    try {
      return this.repositoryClient.importRepository(body, repositoryId);
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new ResourceNotCreatedException(
            "Importing to repository [" + repositoryId + "] failed");
      } else {
        throw exception;
      }
    }
  }

  public void updateRepository(String name, Repository repositoryBody)
      throws ResourceNotUpdatedException, RepositoryNotFoundException {
    logger.debug("Updating repository by name = [{}]", name);
    RepositoryRepository repository = this.getRepositoryByName(name);
    Repository updatedRepository = RepositoryMapper.mapToRepository(repository);
    updatedRepository.setDescription(repositoryBody.getDescription());
    updatedRepository.setName(repositoryBody.getName());
    updatedRepository.setRepositoryLocales(repositoryBody.getRepositoryLocales());
    updatedRepository.setCheckSLA(repositoryBody.isCheckSLA());
    if (repositoryBody.getAssetIntegrityCheckers() != null) {
      updatedRepository.setAssetIntegrityCheckers(repositoryBody.getAssetIntegrityCheckers());
    }
    try {
      this.repositoryClient.updateRepository(updatedRepository, repository.getId());
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new ResourceNotUpdatedException(exception.getResponseBodyAsString());
      } else {
        throw exception;
      }
    }
  }

  public PollableTask deleteBranch(Long repositoryId, Long branchId) {
    return this.repositoryClient.deleteBranch(repositoryId, branchId);
  }

  public List<RepositoryRepository> getRepositories(String name) {
    return this.repositoryClient.getRepositories(name);
  }

  public RepositoryRepository getRepositoryById(Long repositoryId) {
    logger.debug("Getting repository by id = [{}]", repositoryId);
    return this.repositoryClient.getRepositoryById(repositoryId);
  }
}
