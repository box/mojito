package com.box.l10n.mojito.service.branch;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import java.text.MessageFormat;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

  public Branch createBranch(
      Repository repository, String branchName, User createdByUser, Set<String> branchNotifierIds) {

    logger.debug("createBranch, name: {}, repository id: {}", branchName, repository.getId());

    Branch branch = new Branch();
    branch.setName(branchName);
    branch.setRepository(repository);
    branch.setCreatedByUser(createdByUser);
    branch.setNotifiers(branchNotifierIds);
    branch = branchRepository.save(branch);

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
            .build();
    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }
}
