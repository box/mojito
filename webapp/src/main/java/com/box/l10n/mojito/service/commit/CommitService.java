package com.box.l10n.mojito.service.commit;

import static com.box.l10n.mojito.rest.commit.CommitSpecification.*;
import static com.box.l10n.mojito.specification.Specifications.distinct;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.entity.CommitToPullRun;
import com.box.l10n.mojito.entity.CommitToPushRun;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.rest.commit.CommitWithNameNotFoundException;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.service.pullrun.PullRunRepository;
import com.box.l10n.mojito.service.pullrun.PullRunWithNameNotFoundException;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.pushrun.PushRunWithNameNotFoundException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage commits.
 *
 * @author garion
 */
@Service
public class CommitService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(CommitService.class);

  final CommitRepository commitRepository;
  final CommitToPushRunRepository commitToPushRunRepository;
  final CommitToPullRunRepository commitToPullRunRepository;
  final PushRunRepository pushRunRepository;
  final PullRunRepository pullRunRepository;

  final RepositoryRepository repositoryRepository;

  public CommitService(
      CommitRepository commitRepository,
      CommitToPushRunRepository commitToPushRunRepository,
      CommitToPullRunRepository commitToPullRunRepository,
      PushRunRepository pushRunRepository,
      PullRunRepository pullRunRepository,
      RepositoryRepository repositoryRepository) {
    this.commitRepository = commitRepository;
    this.commitToPushRunRepository = commitToPushRunRepository;
    this.commitToPullRunRepository = commitToPullRunRepository;
    this.pushRunRepository = pushRunRepository;
    this.pullRunRepository = pullRunRepository;
    this.repositoryRepository = repositoryRepository;
  }

  /**
   * Gets the last known commit that we have processed and recorded a PushRun for from the list of
   * commit names provided.
   */
  public Optional<Commit> getCommitWithNameAndRepository(String commitName, Long repositoryId) {
    return commitRepository.findByNameAndRepositoryId(commitName, repositoryId);
  }

  /**
   * Gets a list of {@link Commit} using pagination based on a set of search criteria.
   *
   * @return a list of {@link Commit}
   */
  public Page<Commit> getCommits(
      Long repositoryId,
      List<String> commitNames,
      String pushRunName,
      String pullRunName,
      Boolean hasPushRun,
      Boolean hasPullRun,
      Pageable pageable) {

    Specification<Commit> commitSpecification =
        distinct(ifParamNotNull(repositoryIdEquals(repositoryId)))
            .and(ifParamNotNull(commitNamesIn(commitNames)))
            .and(ifParamNotNull(pushRunNameEquals(pushRunName)))
            .and(ifParamNotNull(pullRunNameEquals(pullRunName)))
            .and(ifParamNotNull(hasPushRun(hasPushRun)))
            .and(ifParamNotNull(hasPullRun(hasPullRun)));

    return commitRepository.findAll(commitSpecification, pageable);
  }

  /**
   * Creates a new commit in the database with the information provided.
   *
   * @return The newly created {@link View.Commit}.
   */
  @Transactional
  public Commit getOrCreateCommit(
      Repository repository,
      String commitName,
      String authorEmail,
      String authorName,
      ZonedDateTime sourceCreationDate)
      throws SaveCommitMismatchedExistingDataException {
    Commit commit = new Commit();

    Optional<Commit> existingCommit =
        commitRepository.findByNameAndRepositoryId(commitName, repository.getId());

    if (existingCommit.isPresent()) {
      commit = existingCommit.get();

      if (!commit.getAuthorEmail().equals(authorEmail)) {
        throw new SaveCommitMismatchedExistingDataException(
            "authorEmail", commit.getAuthorEmail(), authorEmail);
      }

      if (!commit.getAuthorName().equals(authorName)) {
        throw new SaveCommitMismatchedExistingDataException(
            "authorName", commit.getAuthorName(), authorName);
      }

      // Remove milliseconds when comparing as the dates are not stored with sub-second precision.
      ZonedDateTime existingCreationDateWithoutMs =
          JSR310Migration.dateTimeWithMillisOfSeconds(commit.getSourceCreationDate(), 0);
      ZonedDateTime sourceCreationDateWithoutMs =
          JSR310Migration.dateTimeWithMillisOfSeconds(sourceCreationDate, 0);
      if (JSR310Migration.getMillis(existingCreationDateWithoutMs)
          != JSR310Migration.getMillis(sourceCreationDateWithoutMs)) {
        throw new SaveCommitMismatchedExistingDataException(
            "sourceCreationDate",
            commit.getSourceCreationDate().toString(),
            sourceCreationDateWithoutMs.toString());
      }
    } else {
      commit.setRepository(repository);
      commit.setName(commitName);
      commit.setAuthorEmail(authorEmail);
      commit.setAuthorName(authorName);
      commit.setSourceCreationDate(sourceCreationDate);

      System.out.printf("setSourceCreationDate: %s%n", commit.getSourceCreationDate());

      commit = commitRepository.save(commit);
    }

    return commit;
  }

  /**
   * Gets the last known commit that we have processed and recorded a PushRun for from the list of
   * commit names provided.
   */
  public Optional<Commit> getLastPushedCommit(List<String> commitNames, Long repositoryId) {
    return commitRepository.findLatestPushedCommits(commitNames, repositoryId, PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  /**
   * Gets the last known PushRun that we have processed and recorded against a commit for from the
   * list of commit names provided.
   */
  public Optional<PushRun> getLastPushRun(List<String> commitNames, Long repositoryId) {
    return pushRunRepository
        .findLatestByCommitNames(commitNames, repositoryId, PageRequest.of(0, 1)).stream()
        .findFirst();
  }

  /**
   * Gets the last known commit that we have processed and recorded a PullRun for from the list of
   * commit names provided.
   */
  public Optional<Commit> getLastPulledCommit(List<String> commitNames, Long repositoryId) {
    return commitRepository.findLatestPulledCommits(commitNames, repositoryId, PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  /**
   * Gets the last known PullRun that we have processed and recorded against a commit for from the
   * list of commit names provided.
   */
  public Optional<PullRun> getLastPullRun(List<String> commitNames, Long repositoryId) {
    return pullRunRepository
        .findLatestByCommitNames(commitNames, repositoryId, PageRequest.of(0, 1)).stream()
        .findFirst();
  }

  /** Gets the {@link PushRun} associated with the commit or null if none exists. */
  public Optional<PushRun> getPushRunForCommitId(Long commitId) {
    Optional<CommitToPushRun> commitToPushRun = commitToPushRunRepository.findByCommitId(commitId);
    return commitToPushRun.map(CommitToPushRun::getPushRun);
  }

  /** Gets the {@link PullRun} associated with the commit or null if none exists. */
  public Optional<PullRun> getPullRunForCommitId(Long commitId) {
    Optional<CommitToPullRun> commitToPullRun = commitToPullRunRepository.findByCommitId(commitId);
    return commitToPullRun.map(CommitToPullRun::getPullRun);
  }

  /** See {@link CommitService#associateCommitToPushRun(Commit, PushRun)}. */
  @Transactional
  public void associateCommitToPushRun(Long repositoryId, String commitName, String pushRunName)
      throws CommitWithNameNotFoundException, RepositoryWithIdNotFoundException,
          PushRunWithNameNotFoundException {
    Commit commit =
        commitRepository
            .findByNameAndRepositoryId(commitName, repositoryId)
            .orElseThrow(() -> new CommitWithNameNotFoundException(commitName));

    Repository repository =
        repositoryRepository
            .findById(repositoryId)
            .orElseThrow(() -> new RepositoryWithIdNotFoundException(repositoryId));

    PushRun pushRun =
        pushRunRepository
            .findByNameAndRepository(pushRunName, repository)
            .orElseThrow(() -> new PushRunWithNameNotFoundException(pushRunName));

    associateCommitToPushRun(commit, pushRun);
  }

  /**
   * Associates a commit with a specific push run ID. Any previous push run ID association for the
   * same commitID will get overwritten. This API should only be called by the push command after
   * all assets were processed successfully.
   */
  @Transactional
  public void associateCommitToPushRun(Commit commit, PushRun pushRun) {
    CommitToPushRun commitToPushRun =
        commitToPushRunRepository.findByCommitId(commit.getId()).orElse(new CommitToPushRun());

    commitToPushRun.setCommit(commit);
    commitToPushRun.setPushRun(pushRun);

    commitToPushRunRepository.save(commitToPushRun);
  }

  /** See {@link CommitService#associateCommitToPullRun(Commit, PullRun)}. */
  @Transactional
  public void associateCommitToPullRun(Long repositoryId, String commitName, String pullRunName)
      throws CommitWithNameNotFoundException, PullRunWithNameNotFoundException {
    Commit commit =
        commitRepository
            .findByNameAndRepositoryId(commitName, repositoryId)
            .orElseThrow(() -> new CommitWithNameNotFoundException(commitName));

    PullRun PullRun =
        pullRunRepository
            .findByName(pullRunName)
            .orElseThrow(() -> new PullRunWithNameNotFoundException(pullRunName));

    associateCommitToPullRun(commit, PullRun);
  }

  /**
   * Associates a commit with a specific pull run ID. Any previous pull run ID association for the
   * same commitID will get overwritten. This API should only be called once the localized files
   * generated by the pull command are fully checked-in to the target repo.
   */
  @Transactional
  public void associateCommitToPullRun(Commit commit, PullRun pullRun) {
    CommitToPullRun commitToPullRun =
        commitToPullRunRepository.findByCommitId(commit.getId()).orElse(new CommitToPullRun());

    commitToPullRun.setCommit(commit);
    commitToPullRun.setPullRun(pullRun);

    commitToPullRunRepository.save(commitToPullRun);
  }
}
