package com.box.l10n.mojito.rest.commit;

import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.PageView;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.service.commit.CommitService;
import com.box.l10n.mojito.service.commit.SaveCommitMismatchedExistingDataException;
import com.box.l10n.mojito.service.pullrun.PullRunWithNameNotFoundException;
import com.box.l10n.mojito.service.pushrun.PushRunWithNameNotFoundException;
import com.box.l10n.mojito.service.repository.RepositoryNameNotFoundException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author garion
 */
@RestController
public class CommitWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(CommitWS.class);

  RepositoryRepository repositoryRepository;

  CommitService commitService;

  public CommitWS(RepositoryRepository repositoryRepository, CommitService commitService) {
    this.repositoryRepository = repositoryRepository;
    this.commitService = commitService;
  }

  /**
   * Gets information about commits based on the specified search criteria and also includes data
   * about their associated PushRun and PullRun runs.
   *
   * @return {@link com.box.l10n.mojito.rest.View.Commit}
   */
  @JsonView(View.CommitDetailed.class)
  @RequestMapping(value = "/api/commits/detailed", method = RequestMethod.GET)
  public Page<Commit> getCommitsDetailed(
      @RequestParam(value = "repositoryId") Long repositoryId,
      @RequestParam(value = "commitNames", required = false) List<String> commitNames,
      @RequestParam(value = "pushRunName", required = false) String pushRunName,
      @RequestParam(value = "pullRunName", required = false) String pullRunName,
      @RequestParam(value = "hasPushRun", required = false) Boolean hasPushRun,
      @RequestParam(value = "hasPullRun", required = false) Boolean hasPullRun,
      @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

    return getCommits(
        repositoryId, commitNames, pushRunName, pullRunName, hasPushRun, hasPullRun, pageable);
  }

  /**
   * Gets information about commits based on the specified search criteria.
   *
   * @return {@link Commit}
   */
  @JsonView(View.Commit.class)
  @RequestMapping(value = "/api/commits", method = RequestMethod.GET)
  public Page<Commit> getCommits(
      @RequestParam(value = "repositoryId") Long repositoryId,
      @RequestParam(value = "commitNames", required = false) List<String> commitNames,
      @RequestParam(value = "pushRunName", required = false) String pushRunName,
      @RequestParam(value = "pullRunName", required = false) String pullRunName,
      @RequestParam(value = "hasPushRun", required = false) Boolean hasPushRun,
      @RequestParam(value = "hasPullRun", required = false) Boolean hasPullRun,
      @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<Commit> commits =
        commitService.getCommits(
            repositoryId, commitNames, pushRunName, pullRunName, hasPushRun, hasPullRun, pageable);

    return new PageView<>(commits);
  }

  /**
   * Gets the last known commit that we have processed and recorded a PushRun for from the list of
   * commit names provided.
   *
   * <p>Returns null if no commit is found for that repository with a corresponding push run.
   *
   * @param commitListWithRepositoryIdBody The commit names to search for together with the
   *     repositoryID.
   * @return {@link View.Commit}
   */
  @JsonView(View.Commit.class)
  @RequestMapping(value = "/api/commits/lastPushed/", method = RequestMethod.POST)
  public Commit getLastPushedCommit(
      @RequestBody CommitListWithRepositoryIdBody commitListWithRepositoryIdBody) {
    return commitService
        .getLastPushedCommit(
            commitListWithRepositoryIdBody.getCommitNames(),
            commitListWithRepositoryIdBody.getRepositoryId())
        .orElse(null);
  }

  /**
   * Gets the last known PushRun that we have processed and recorded against a commit from the list
   * of commit names provided.
   *
   * <p>Returns null if no commit is found for that repository with a corresponding push run.
   *
   * @param commitListWithRepositoryIdBody The commit names to search for together with the
   *     repositoryID.
   * @return {@link View.Commit}
   */
  @JsonView(View.CommitDetailed.class)
  @RequestMapping(value = "/api/commits/lastPushRun/", method = RequestMethod.POST)
  public PushRun getLastPushRun(
      @RequestBody CommitListWithRepositoryIdBody commitListWithRepositoryIdBody) {
    return commitService
        .getLastPushRun(
            commitListWithRepositoryIdBody.getCommitNames(),
            commitListWithRepositoryIdBody.getRepositoryId())
        .orElse(null);
  }

  /**
   * Gets the last known commit that we have processed and recorded a PullRun for from the list of
   * commit names provided.
   *
   * <p>Returns null if no commit is found for that repository with a corresponding pull run.
   *
   * @param commitListWithRepositoryIdBody The commit names to search for together with the
   *     repositoryID.
   * @return {@link View.Commit}
   */
  @JsonView(View.Commit.class)
  @RequestMapping(value = "/api/commits/lastPulled/", method = RequestMethod.POST)
  public Commit getLastPulledCommit(
      @RequestBody CommitListWithRepositoryIdBody commitListWithRepositoryIdBody) {
    return commitService
        .getLastPulledCommit(
            commitListWithRepositoryIdBody.getCommitNames(),
            commitListWithRepositoryIdBody.getRepositoryId())
        .orElse(null);
  }

  /**
   * Gets the last known PullRun that we have processed and recorded against a commit from the list
   * of commit names provided.
   *
   * <p>Returns null if no commit is found for that repository with a corresponding pull run.
   *
   * @param commitListWithRepositoryIdBody The commit names to search for together with the
   *     repositoryID.
   * @return {@link View.Commit}
   */
  @JsonView(View.CommitDetailed.class)
  @RequestMapping(value = "/api/commits/lastPullRun/", method = RequestMethod.POST)
  public PullRun getLastPullRun(
      @RequestBody CommitListWithRepositoryIdBody commitListWithRepositoryIdBody) {
    return commitService
        .getLastPullRun(
            commitListWithRepositoryIdBody.getCommitNames(),
            commitListWithRepositoryIdBody.getRepositoryId())
        .orElse(null);
  }

  /**
   * Creates a new commit in the database with the information provided.
   *
   * @param commitBody The data for the commit to be created in the DB.
   * @return The newly created {@link View.Commit}.
   * @throws RepositoryWithIdNotFoundException if the repository ID is not found.
   */
  @JsonView(View.Commit.class)
  @RequestMapping(value = "/api/commits", method = RequestMethod.POST)
  public Commit createCommit(@RequestBody CommitBody commitBody)
      throws RepositoryWithIdNotFoundException, SaveCommitMismatchedExistingDataException {
    Repository repository =
        repositoryRepository
            .findById(commitBody.getRepositoryId())
            .orElseThrow(() -> new RepositoryWithIdNotFoundException(commitBody.getRepositoryId()));

    return commitService.getOrCreateCommit(
        repository,
        commitBody.getCommitName(),
        commitBody.getAuthorEmail(),
        commitBody.getAuthorName(),
        commitBody.getSourceCreationDate());
  }

  /**
   * Associates a commit with a specific push run ID. Any previous push run ID association for the
   * same commitID will get overwritten. This API should only be called by the push command after
   * all assets were processed successfully.
   *
   * @param commitToPushRunBody The identifying data for the Commit and PushRun to associate.
   * @throws RepositoryNameNotFoundException if the repository name can not be found.
   * @throws PushRunWithNameNotFoundException if the push run can not be found.
   * @throws CommitWithNameNotFoundException if the commit name can not be found.
   */
  @RequestMapping(value = "/api/commits/pushRun", method = RequestMethod.POST)
  public void associateCommitToPushRun(@RequestBody CommitToPushRunBody commitToPushRunBody)
      throws RepositoryWithIdNotFoundException,
          PushRunWithNameNotFoundException,
          CommitWithNameNotFoundException {
    commitService.associateCommitToPushRun(
        commitToPushRunBody.getRepositoryId(),
        commitToPushRunBody.getCommitName(),
        commitToPushRunBody.getPushRunName());
  }

  /**
   * Associates a commit with a specific pull run ID. Any previous pull run ID association for the
   * same commitID will get overwritten. This API should only be called once the localized files
   * generated by the pull command are fully checked-in to the target repo.
   *
   * @param commitToPullRunBody The identifying data for the Commit and PullRun to associate.
   * @throws RepositoryNameNotFoundException if the repository name can not be found.
   * @throws PullRunWithNameNotFoundException if the pull run can not be found.
   * @throws CommitWithNameNotFoundException if the commit name can not be found.
   */
  @RequestMapping(value = "/api/commits/pullRun", method = RequestMethod.POST)
  public void associateCommitToPullRun(@RequestBody CommitToPullRunBody commitToPullRunBody)
      throws RepositoryNameNotFoundException,
          CommitWithNameNotFoundException,
          PullRunWithNameNotFoundException {
    commitService.associateCommitToPullRun(
        commitToPullRunBody.getRepositoryId(),
        commitToPullRunBody.getCommitName(),
        commitToPullRunBody.getPullRunName());
  }
}
