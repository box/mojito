package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** @author jaurambault */
@Component
public class DeleteAssetsOfBranchJob extends QuartzPollableJob<DeleteAssetsOfBranchJobInput, Void> {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DeleteAssetsOfBranchJob.class);

  @Autowired AssetService assetService;

  @Override
  public Void call(DeleteAssetsOfBranchJobInput input) throws Exception {
    logger.debug("DeleteAssetsOfBranchJob");
    assetService.deleteAssetsOfBranch(input.getAssetIds(), input.getBranchId());
    return null;
  }
}
