package com.box.l10n.mojito.service.delta;

import com.box.l10n.mojito.service.assetExtraction.AssetExtractionCleanupJob;
import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author garion
 */
@Service
public class PushPullRunCleanupService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetExtractionCleanupJob.class);

  @Autowired PushRunService pushRunService;

  @Autowired PullRunService pullRunService;

  public void cleanOldPushPullData(Duration retentionDuration) {
    pushRunService.deleteAllPushEntitiesOlderThan(retentionDuration);
    pullRunService.deleteAllPullEntitiesOlderThan(retentionDuration);
  }
}
