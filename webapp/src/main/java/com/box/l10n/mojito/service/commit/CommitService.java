package com.box.l10n.mojito.service.commit;

import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.entity.CommitToPullRun;
import com.box.l10n.mojito.entity.CommitToPushRun;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.rest.commit.CommitWithNameNotFoundException;
import com.box.l10n.mojito.service.pullrun.PullRunRepository;
import com.box.l10n.mojito.service.pullrun.PullRunWithNameNotFoundException;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.pushrun.PushRunWithNameNotFoundException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.box.l10n.mojito.rest.commit.CommitSpecification.*;
import static com.box.l10n.mojito.specification.Specifications.distinct;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;

/**
 * @author garion
 */
@Service
public class CommitService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CommitService.class);

    final CommitRepository commitRepository;
    final CommitToPushRunRepository commitToPushRunRepository;
    final CommitToPullRunRepository commitToPullRunRepository;
    final PushRunRepository pushRunRepository;
    final PullRunRepository pullRunRepository;

    public CommitService(CommitRepository commitRepository,
                         CommitToPushRunRepository commitToPushRunRepository,
                         CommitToPullRunRepository commitToPullRunRepository,
                         PushRunRepository pushRunRepository,
                         PullRunRepository pullRunRepository) {
        this.commitRepository = commitRepository;
        this.commitToPushRunRepository = commitToPushRunRepository;
        this.commitToPullRunRepository = commitToPullRunRepository;
        this.pushRunRepository = pushRunRepository;
        this.pullRunRepository = pullRunRepository;
    }

    /**
     * Gets the last known commit that we have processed and recorded a
     * PushRun for from the list of commit names provided.
     */
    public Optional<Commit> getCommitWithNameAndRepository(String commitName, Long repositoryId) {
        return commitRepository.findByNameAndRepositoryId(commitName, repositoryId);
    }

    /**
     * Gets a list of {@link Commit} using pagination based on a set of search criteria.
     *
     * @return a list of {@link Commit}
     */
    public Page<Commit> getCommits(Long repositoryId,
                                   List<String> commitNames,
                                   String pushRunName,
                                   String pullRunName,
                                   Boolean hasPushRun,
                                   Boolean hasPullRun,
                                   Pageable pageable) {

        Specification<Commit> commitSpecification = distinct(ifParamNotNull(repositoryIdEquals(repositoryId)))
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
    public Commit saveCommit(Repository repository,
                             String commitName,
                             String authorEmail,
                             String authorName,
                             DateTime sourceCreationDate) {
        Commit commit = new Commit();

        commit.setRepository(repository);
        commit.setName(commitName);
        commit.setAuthorEmail(authorEmail);
        commit.setAuthorName(authorName);
        commit.setSourceCreationDate(sourceCreationDate);

        return commitRepository.save(commit);
    }

    /**
     * Gets the last known commit that we have processed and recorded a
     * PushRun for from the list of commit names provided.
     * Returns null if no commit is found for that repository with a
     * corresponding push run.
     */
    public Optional<Commit> getLastPushedCommit(List<String> commitNames, Long repositoryId) {
        return commitRepository.findLatestPushedCommits(commitNames, repositoryId, PageRequest.of(0, 1))
                .stream().findFirst();
    }

    /**
     * Gets the {@link PushRun} associated with the commit or null if none exists.
     */
    public Optional<PushRun> getPushRunForCommitId(Long commitId) {
        Optional<CommitToPushRun> commitToPushRun = commitToPushRunRepository.findByCommitId(commitId);
        return commitToPushRun.map(CommitToPushRun::getPushRun);
    }

    /**
     * Gets the {@link PullRun} associated with the commit or null if none exists.
     */
    public Optional<PullRun> getPullRunForCommitId(Long commitId) {
        Optional<CommitToPullRun> commitToPullRun = commitToPullRunRepository.findByCommitId(commitId);
        return commitToPullRun.map(CommitToPullRun::getPullRun);
    }


    /**
     * See {@link CommitService#associateCommitToPushRun(Commit, PushRun)}.
     */
    @Transactional
    public void associateCommitToPushRun(Long repositoryId, String commitName, String pushRunName)
            throws CommitWithNameNotFoundException, PushRunWithNameNotFoundException {
        Commit commit = commitRepository.findByNameAndRepositoryId(commitName, repositoryId)
                .orElseThrow(() -> new CommitWithNameNotFoundException(commitName));

        PushRun pushRun = pushRunRepository.findByName(pushRunName)
                .orElseThrow(() -> new PushRunWithNameNotFoundException(pushRunName));

        associateCommitToPushRun(commit, pushRun);
    }

    /**
     * Associates a commit with a specific push run ID. Any previous push run ID
     * association for the same commitID will get overwritten.
     * This API should only be called by the push command after all assets were
     * processed successfully.
     */
    @Transactional
    public void associateCommitToPushRun(Commit commit, PushRun pushRun) {
        CommitToPushRun commitToPushRun = commitToPushRunRepository.findByCommitId(commit.getId())
                .orElse(new CommitToPushRun());

        commitToPushRun.setCommit(commit);
        commitToPushRun.setPushRun(pushRun);

        commitToPushRunRepository.save(commitToPushRun);
    }

    /**
     * See {@link CommitService#associateCommitToPullRun(Commit, PullRun)}.
     */
    @Transactional
    public void associateCommitToPullRun(Long repositoryId, String commitName, String pullRunName)
            throws CommitWithNameNotFoundException, PullRunWithNameNotFoundException {
        Commit commit = commitRepository.findByNameAndRepositoryId(commitName, repositoryId)
                .orElseThrow(() -> new CommitWithNameNotFoundException(commitName));

        PullRun PullRun = pullRunRepository.findByName(pullRunName)
                .orElseThrow(() -> new PullRunWithNameNotFoundException(pullRunName));

        associateCommitToPullRun(commit, PullRun);
    }

    /**
     * Associates a commit with a specific pull run ID. Any previous pull run ID
     * association for the same commitID will get overwritten.
     * This API should only be called once the localized files generated by the
     * pull command are fully checked-in to the target repo.
     */
    @Transactional
    public void associateCommitToPullRun(Commit commit, PullRun pullRun) {
        CommitToPullRun commitToPullRun = commitToPullRunRepository.findByCommitId(commit.getId())
                .orElse(new CommitToPullRun());

        commitToPullRun.setCommit(commit);
        commitToPullRun.setPullRun(pullRun);

        commitToPullRunRepository.save(commitToPullRun);
    }
}
