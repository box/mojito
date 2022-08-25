package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRunAsset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.commit.CommitToPushRunRepository;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service that manages PushRuns. Allows creation of PushRuns,
 * association and retrieval of connected TextUnit data.
 *
 * @author garion
 */
@Service
public class PushRunService {

    private static final int BATCH_SIZE = 1000;

    final EntityManager entityManager;

    final JdbcTemplate jdbcTemplate;

    final CommitToPushRunRepository commitToPushRunRepository;

    final PushRunRepository pushRunRepository;

    final PushRunAssetRepository pushRunAssetRepository;

    final PushRunAssetTmTextUnitRepository pushRunAssetTmTextUnitRepository;

    public PushRunService(EntityManager entityManager,
                          JdbcTemplate jdbcTemplate,
                          CommitToPushRunRepository commitToPushRunRepository, PushRunRepository pushRunRepository,
                          PushRunAssetRepository pushRunAssetRepository,
                          PushRunAssetTmTextUnitRepository pushRunAssetTmTextUnitRepository) {
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
        this.commitToPushRunRepository = commitToPushRunRepository;
        this.pushRunRepository = pushRunRepository;
        this.pushRunAssetRepository = pushRunAssetRepository;
        this.pushRunAssetTmTextUnitRepository = pushRunAssetTmTextUnitRepository;
    }

    /**
     * Creates a new PushRun entry with a new UUID as the logical name.
     */
    public PushRun createPushRun(Repository repository) {
        return createPushRun(repository, null);
    }

    /**
     * Creates a new PushRun entry with the specified name.
     * Note: if the name is null/blank, a UUID will be generated and used instead.
     */
    public PushRun createPushRun(Repository repository, String pushRunName) {
        if (StringUtils.isBlank(pushRunName)) {
            pushRunName = UUID.randomUUID().toString();
        }

        PushRun newPushRun = new PushRun();

        newPushRun.setRepository(repository);
        newPushRun.setName(pushRunName);

        return pushRunRepository.save(newPushRun);
    }

    /**
     * Removes the linked PushRunAssets and pushRunAssetTmTextUnits from the PushRun.
     */
    @Transactional
    public void clearPushRunLinkedData(PushRun pushRun) {
        List<PushRunAsset> existingPushRunAssets = pushRunAssetRepository.findByPushRun(pushRun);
        existingPushRunAssets.forEach(pushRunAssetTmTextUnitRepository::deleteByPushRunAsset);
        pushRunAssetRepository.deleteByPushRun(pushRun);
    }

    /**
     * Associates a set of TextUnits to a PushRunAsset and a PushRun.
     */
    @Transactional
    public void associatePushRunToTextUnitIds(PushRun pushRun, Asset asset, List<Long> textUnitIds) {
        PushRunAsset pushRunAsset = pushRunAssetRepository.findByPushRunAndAsset(pushRun, asset).orElse(null);

        if (pushRunAsset == null) {
            pushRunAsset = new PushRunAsset();

            pushRunAsset.setPushRun(pushRun);
            pushRunAsset.setAsset(asset);

            pushRunAssetRepository.save(pushRunAsset);
        } else {
            pushRunAssetTmTextUnitRepository.deleteByPushRunAsset(pushRunAsset);
        }

        Instant now = Instant.now();
        PushRunAsset finalPushRunAsset = pushRunAsset;
        Lists.partition(textUnitIds, BATCH_SIZE)
                .forEach(textUnitIdsBatch -> saveTextUnits(finalPushRunAsset, textUnitIdsBatch, now));
    }

    /**
     * Retrieves the list of TextUnits associated with a PushRun.
     */
    public List<TMTextUnit> getPushRunTextUnits(PushRun pushRun, Pageable pageable) {
        return pushRunAssetTmTextUnitRepository.findByPushRun(pushRun, pageable);
    }

    void saveTextUnits(PushRunAsset pushRunAsset, List<Long> textUnitIds, Instant now) {
        String createdTime = Timestamp.valueOf(LocalDateTime.ofInstant(now, ZoneOffset.UTC)).toString();
        String sql = "insert into push_run_asset_tm_text_unit(push_run_asset_id, tm_text_unit_id, created_date) values" +
                textUnitIds.stream()
                        .map(tuId -> String.format("(%s, %s, '%s') ", pushRunAsset.getId(), tuId, createdTime))
                        .collect(Collectors.joining(","));

        jdbcTemplate.update(sql);
    }

    public PushRun getPushRunById(long id) {
        return pushRunRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(String.format(
                        "Could not find a PushRun for id: %s", id)));
    }

    public void deleteAllPushEntitiesOlderThan(Duration retentionDuration) {
        DateTime beforeDate = DateTime.now().minusSeconds((int) retentionDuration.getSeconds());
        Timestamp sqlBeforeDate = new Timestamp(beforeDate.toDate().getTime());

        pushRunAssetTmTextUnitRepository.deleteAllByPushRunWithCreatedDateBefore(sqlBeforeDate);
        pushRunAssetRepository.deleteAllByPushRunWithCreatedDateBefore(sqlBeforeDate);
        commitToPushRunRepository.deleteAllByPushRunWithCreatedDateBefore(sqlBeforeDate);
        pushRunRepository.deleteAllByCreatedDateBefore(sqlBeforeDate);
    }
}
