package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.commit.CommitToPullRunRepository;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;

/**
 * Service to manage PullRun data.
 * 
 * @author garion
 */
@Service
public class PullRunService {

    @Autowired
    PullRunRepository pullRunRepository;

    @Autowired
    CommitToPullRunRepository commitToPullRunRepository;

    @Autowired
    PullRunAssetRepository pullRunAssetRepository;

    @Autowired
    PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

    public PullRun getOrCreate(String pullRunName, Repository repository) {
        return pullRunRepository.findByName(pullRunName).orElseGet(() -> {
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

        pullRunTextUnitVariantRepository.deleteAllByPullRunWithCreatedDateBefore(sqlBeforeDate);
        pullRunAssetRepository.deleteAllByPullRunWithCreatedDateBefore(sqlBeforeDate);
        commitToPullRunRepository.deleteAllByPullRunWithCreatedDateBefore(sqlBeforeDate);
        pullRunRepository.deleteAllByCreatedDateBefore(sqlBeforeDate);
    }
}
