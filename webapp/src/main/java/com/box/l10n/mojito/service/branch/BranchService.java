package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service to manage {@link Branch}es.
 * <p>
 * When no branch name is specified, a branch with name: null will be used.
 *
 * @author jeanaurambault
 */
@Service
public class BranchService {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchService.class);

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    AssetService assetService;

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    public Branch createBranch(Repository repository, String branchName, User createdByUser) {

        logger.debug("createBranch, name: {}, repository id: {}", branchName, repository.getId());

        Branch branch = new Branch();
        branch.setName(branchName);
        branch.setRepository(repository);
        branch.setCreatedByUser(createdByUser);

        branch = branchRepository.save(branch);

        return branch;
    }

    public Branch getUndeletedOrCreateBranch(Repository repository, String branchName, User createdByUser) {

        logger.debug("getUndeletedOrCreateBranch, name: {}, repository id: {}", branchName, repository.getId());

        Branch branch = branchRepository.findByNameAndRepository(branchName, repository);

        if (branch == null) {
            branch = createBranch(repository, branchName, createdByUser);
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
        String pollableMessage = MessageFormat.format(" - Delete branch: {0} from repository: {1}", branchId, repositoryId);
        QuartzJobInfo quartzJobInfo = QuartzJobInfo.newBuilder(DeleteBranchJob.class).withInput(deleteBranchJobInput).withMessage(pollableMessage).build();
        return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
    }

    @Transactional
    public void deleteBranch(Long repositoryId, Long branchId) {
        deleteBranchAsset(branchId, repositoryId);

        Branch branch = branchRepository.findOne(branchId);
        logger.debug("Mark branch {} as deleted", branch.getName());
        branch.setDeleted(true);
        branchRepository.save(branch);
    }

    public void deleteBranchAsset(Long branchId, Long repositoryId) {
        Set<Long> assetIds = assetService.findAllAssetIds(repositoryId, null, false, false, branchId);
        assetService.deleteAssetsOfBranch(assetIds, branchId);
    }
}