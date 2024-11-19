package com.box.l10n.mojito.service.branch;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchSource;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.tm.BranchSourceRepository;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.text.MessageFormat;
import java.util.Set;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to manage {@link Branch}es.
 *
 * <p>When no branch name is specified, a branch with name: null will be used.
 *
 * @author jeanaurambault
 */
@Service
public class BranchService {

  /** logger */
  static Logger logger = getLogger(BranchService.class);

  @Autowired BranchRepository branchRepository;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired BranchSourceRepository branchSourceRepository;

  @Autowired BranchSourceConfig branchSourceConfig;

  @Value("${l10n.branchService.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String schedulerName;

  public Branch createBranch(
      Repository repository, String branchName, User createdByUser, Set<String> branchNotifierIds) {

    logger.debug("createBranch, name: {}, repository id: {}", branchName, repository.getId());

    Branch branch = new Branch();
    branch.setName(branchName);
    branch.setRepository(repository);
    branch.setCreatedByUser(createdByUser);
    branch.setNotifiers(branchNotifierIds);
    branch = branchRepository.save(branch);

    addBranchSource(branch);

    return branch;
  }

  public Branch getUndeletedOrCreateBranch(
      Repository repository, String branchName, User createdByUser, Set<String> branchNotifierIds) {

    logger.debug(
        "getUndeletedOrCreateBranch, name: {}, repository id: {}", branchName, repository.getId());

    Branch branch = branchRepository.findByNameAndRepository(branchName, repository);

    if (branch == null) {
      branch = createBranch(repository, branchName, createdByUser, branchNotifierIds);
    } else if (branch.getDeleted()) {
      undeleteBranch(branch);
    }

    return branch;
  }

  public void undeleteBranch(Branch branch) {
    branch.setDeleted(false);
    branchRepository.save(branch);
  }

  public PollableFuture<Void> asyncDeleteBranch(Long repositoryId, Long branchId) {
    DeleteBranchJobInput deleteBranchJobInput = new DeleteBranchJobInput();
    deleteBranchJobInput.setRepositoryId(repositoryId);
    deleteBranchJobInput.setBranchId(branchId);
    String pollableMessage =
        MessageFormat.format(" - Delete branch: {0} from repository: {1}", branchId, repositoryId);
    QuartzJobInfo<DeleteBranchJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(DeleteBranchJob.class)
            .withInput(deleteBranchJobInput)
            .withMessage(pollableMessage)
            .withScheduler(schedulerName)
            .build();
    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  public void addBranchSource(Branch branch) {
    // Mojito push links text unit extractions to empty branch, don't attempt to update the source
    if (branch.getName() == null) return;

    com.box.l10n.mojito.service.branch.BranchSource branchSource =
        branchSourceConfig.getRepoOverride().get(branch.getRepository().getName());

    String sourceUrl = (branchSource != null) ? branchSource.getUrl() : branchSourceConfig.getUrl();
    if (Strings.isNullOrEmpty(sourceUrl)) return;

    String url =
        StrSubstitutor.replace(
            sourceUrl, ImmutableMap.of("branchName", branch.getName()), "{", "}");

    BranchSource bSource = new BranchSource();
    bSource.setBranch(branch);
    bSource.setUrl(url);
    try {
      branchSourceRepository.save(bSource);
    } catch (Exception e) {
      logger.error(
          "Failed to save branch source for branch '{}' with url '{}'", branch.getName(), url, e);
    }
  }
}
