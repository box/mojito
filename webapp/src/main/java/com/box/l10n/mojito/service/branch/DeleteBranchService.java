package com.box.l10n.mojito.service.branch;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.service.asset.AssetService;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Service to manage deletion of {@link Branch}es.
 *
 * @author jeanaurambault
 */
@Service
public class DeleteBranchService {

  /** logger */
  static Logger logger = getLogger(DeleteBranchService.class);

  private final BranchRepository branchRepository;

  private final AssetService assetService;

  public DeleteBranchService(BranchRepository branchRepository, AssetService assetService) {
    this.branchRepository = branchRepository;
    this.assetService = assetService;
  }

  public void deleteBranch(Long repositoryId, Long branchId) {
    deleteBranchAsset(branchId, repositoryId);

    Branch branch = branchRepository.findById(branchId).orElse(null);
    logger.debug("Mark branch {} as deleted", branch.getName());
    branch.setDeleted(true);
    branchRepository.save(branch);
  }

  public void deleteBranchAsset(Long branchId, Long repositoryId) {
    Set<Long> assetIds = assetService.findAllAssetIds(repositoryId, null, false, false, branchId);
    assetService.deleteAssetsOfBranch(assetIds, branchId);
  }
}
