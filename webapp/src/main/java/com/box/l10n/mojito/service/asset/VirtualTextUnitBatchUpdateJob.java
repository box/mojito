package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
public class VirtualTextUnitBatchUpdateJob
    extends QuartzPollableJob<VirtualTextUnitBatchUpdateJobInput, Void> {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(VirtualTextUnitBatchUpdateJob.class);

  @Autowired VirtualTextUnitBatchUpdaterService virtualTextUnitBatchUpdaterService;

  @Autowired VirtualAssetService virtualAssetService;

  @Override
  public Void call(VirtualTextUnitBatchUpdateJobInput input) throws Exception {
    logger.debug("ImportVirtualAssetJob");
    Asset asset = virtualAssetService.getVirtualAsset(input.getAssetId());
    virtualTextUnitBatchUpdaterService.updateTextUnits(
        asset, input.getVirtualAssetTextUnits(), input.isReplace());
    return null;
  }
}
