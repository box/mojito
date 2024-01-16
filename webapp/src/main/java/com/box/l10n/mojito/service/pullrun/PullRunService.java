package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.commit.CommitToPullRunRepository;
import java.time.Duration;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Autowired PullRunRepository pullRunRepository;

  @Autowired CommitToPullRunRepository commitToPullRunRepository;

  @Autowired PullRunAssetRepository pullRunAssetRepository;

  @Autowired PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

  @Value("${l10n.PullRunService.cleanup-job.batchsize:100000}")
  int deleteBatchSize;

  @Value("${l10n.PullRunService.cleanup-job.waitMs:2000}")
  int deleteWaitMs;

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
              beforeDate, deleteBatchSize);
      logger.debug(
          "Deleted {} pullRunTextUnitVariant rows in batch: {}", deleteCount, batchNumber++);
      waitForConfiguredTime();
    } while (deleteCount == deleteBatchSize);

    pullRunAssetRepository.deleteAllByPullRunWithCreatedDateBefore(beforeDate);
    commitToPullRunRepository.deleteAllByPullRunWithCreatedDateBefore(beforeDate);
    pullRunRepository.deleteAllByCreatedDateBefore(beforeDate);
  }

  private void waitForConfiguredTime() {
    try {
      Thread.sleep(deleteWaitMs);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
