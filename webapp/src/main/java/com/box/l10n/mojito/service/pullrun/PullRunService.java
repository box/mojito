package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.commit.CommitToPullRunRepository;
import java.time.Duration;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to manage PullRun data.
 *
 * @author garion
 */
@Service
public class PullRunService {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(PullRunService.class);

  static final int DELETE_BATCH_SIZE = 100000;

  @Autowired PullRunRepository pullRunRepository;

  @Autowired CommitToPullRunRepository commitToPullRunRepository;

  @Autowired PullRunAssetRepository pullRunAssetRepository;

  @Autowired PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

  public PullRun getOrCreate(String pullRunName, Repository repository) {
    return pullRunRepository
        .findByName(pullRunName)
        .orElseGet(
            () -> {
              PullRun pullRun = new PullRun();
              pullRun.setName(pullRunName);
              pullRun.setRepository(repository);
              pullRunRepository.save(pullRun);
              return pullRun;
            });
  }

  public void deleteAllPullEntitiesOlderThan(Duration retentionDuration) {
    ZonedDateTime beforeDate =
        ZonedDateTime.now().minusSeconds((int) retentionDuration.getSeconds());

    int batchNumber = 1;
    int deleteCount;
    do {
      deleteCount =
          pullRunTextUnitVariantRepository.deleteAllByPullRunWithCreatedDateBefore(
              beforeDate, DELETE_BATCH_SIZE);
      logger.debug(
          "Deleted {} pullRunTextUnitVariant rows in batch: {}", deleteCount, batchNumber++);
    } while (deleteCount == DELETE_BATCH_SIZE);

    pullRunAssetRepository.deleteAllByPullRunWithCreatedDateBefore(beforeDate);
    commitToPullRunRepository.deleteAllByPullRunWithCreatedDateBefore(beforeDate);
    pullRunRepository.deleteAllByCreatedDateBefore(beforeDate);
  }
}
