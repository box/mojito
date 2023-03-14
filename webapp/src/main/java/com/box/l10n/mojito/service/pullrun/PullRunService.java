package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.commit.CommitToPullRunRepository;
import java.sql.Timestamp;
import java.time.Duration;
import org.joda.time.DateTime;
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
    DateTime beforeDate = DateTime.now().minusSeconds((int) retentionDuration.getSeconds());
    Timestamp sqlBeforeDate = new Timestamp(beforeDate.toDate().getTime());

    int batchNumber = 1;
    int deleteCount;
    do {
      deleteCount =
          pullRunTextUnitVariantRepository.deleteAllByPullRunWithCreatedDateBefore(
              sqlBeforeDate, DELETE_BATCH_SIZE);
      logger.debug(
          "Deleted {} pullRunTextUnitVariant rows in batch: {}", deleteCount, batchNumber++);
    } while (deleteCount == DELETE_BATCH_SIZE);

    pullRunAssetRepository.deleteAllByPullRunWithCreatedDateBefore(sqlBeforeDate);
    commitToPullRunRepository.deleteAllByPullRunWithCreatedDateBefore(sqlBeforeDate);
    pullRunRepository.deleteAllByCreatedDateBefore(sqlBeforeDate);
  }
}
