package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.Repository;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service to manage PullRunAsset entities.
 *
 * @author garion
 */
@Service
public class PullRunAssetService {

    static final int BATCH_SIZE = 1000;

    @Autowired
    PullRunAssetRepository pullRunAssetRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MeterRegistry meterRegistry;

    public PullRunAsset createPullRunAsset(PullRun pullRun, Asset asset) {
        PullRunAsset pullRunAsset = new PullRunAsset();
        pullRunAsset.setPullRun(pullRun);
        pullRunAsset.setAsset(asset);

        return pullRunAssetRepository.save(pullRunAsset);
    }

    public PullRunAsset getOrCreate(PullRun pullRun, Asset asset) {
        return pullRunAssetRepository.findByPullRunAndAsset(pullRun, asset).orElseGet(() -> createPullRunAsset(pullRun, asset));
    }

    @Transactional
    public void replaceTextUnitVariants(PullRunAsset pullRunAsset, Long localeId, List<Long> uniqueTmTextUnitVariantIds) {
        Repository repository = pullRunAsset.getPullRun().getRepository();
        meterRegistry.timer("PullRunAssetService.saveTextUnitVariantsMultiRow", Tags.of("repositoryId", Objects.toString(repository.getId())))
                .record(() -> {
                    Instant now = Instant.now();
                    jdbcTemplate.update("delete from pull_run_text_unit_variant where pull_run_asset_id = ? and locale_id = ?", pullRunAsset.getId(), localeId);
                    Lists.partition(uniqueTmTextUnitVariantIds, BATCH_SIZE).forEach(tuvIdsBatch -> saveTextUnitVariantsMultiRowBatch(pullRunAsset, localeId, tuvIdsBatch, now));
                });
    }

    void saveTextUnitVariantsMultiRowBatch(PullRunAsset pullRunAsset, Long localeId, List<Long> uniqueTmTextUnitVariantIds, Instant now) {
        String sql = "insert into pull_run_text_unit_variant(pull_run_asset_id, locale_id, tm_text_unit_variant_id, created_date) values "
                + uniqueTmTextUnitVariantIds.stream().map(tuvId -> String.format("(%s, %s, %s, '%s') ",
                pullRunAsset.getId(), localeId, tuvId, Timestamp.from(now))).collect(Collectors.joining(","));
        jdbcTemplate.update(sql);
    }
}
