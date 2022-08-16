package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.aspect.StopWatch;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PullRunTextUnitVariant;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.slf4j.event.Level.INFO;

/**
 * Service to manage PullRunAsset entities.
 *
 * @author garion
 */
@Service
public class PullRunAssetService {
    public static final int BATCH_SIZE = 1000;
    @Autowired
    PullRunAssetRepository pullRunAssetRepository;

    @Autowired
    PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    DataSource dataSource;

    public PullRunAsset createPullRunAsset(PullRun pullRun, Asset asset) {
        PullRunAsset pullRunAsset = new PullRunAsset();
        pullRunAsset.setPullRun(pullRun);
        pullRunAsset.setAsset(asset);

        return pullRunAssetRepository.save(pullRunAsset);
    }

    @StopWatch(level = INFO)
    @Transactional
    public void saveTextUnitVariants(PullRunAsset pullRunAsset, List<Long> tuvIds) {
        Repository repository = pullRunAsset.getPullRun().getRepository();
        meterRegistry.timer("PullRunAssetService.saveTextUnitVariants", Tags.of("repositoryId", Objects.toString(repository.getId())))
                .record(() -> {
                    Lists.partition(tuvIds, BATCH_SIZE).forEach(tuvIdsBatch -> saveTextUnitVariantsBatch(pullRunAsset, tuvIdsBatch));
                });
    }

    @StopWatch(level = INFO)
    @Transactional
    public void saveTextUnitVariantsPreparedStatement(PullRunAsset pullRunAsset, List<Long> tuvIds) {
        Repository repository = pullRunAsset.getPullRun().getRepository();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Instant now = Instant.now();
        meterRegistry.timer("PullRunAssetService.saveTextUnitVariantsPreparedStatement", Tags.of("repositoryId", Objects.toString(repository.getId())))
                .record(() -> {
                    Lists.partition(tuvIds, BATCH_SIZE).forEach(tuvIdsBatch -> saveTextUnitVariantsPreparedStatementBatch(jdbcTemplate, pullRunAsset, tuvIdsBatch, now));
                });
    }

    @StopWatch(level = INFO)
    @Transactional
    public void saveTextUnitVariantsMultiRow(PullRunAsset pullRunAsset, List<Long> tuvIds) {
        Repository repository = pullRunAsset.getPullRun().getRepository();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Instant now = Instant.now();
        meterRegistry.timer("PullRunAssetService.saveTextUnitVariantsMultiRow", Tags.of("repositoryId", Objects.toString(repository.getId())))
                .record(() -> {
                    Lists.partition(tuvIds, BATCH_SIZE).forEach(tuvIdsBatch -> saveTextUnitVariantsMultiRowBatch(jdbcTemplate, pullRunAsset, tuvIdsBatch, now));
                });
    }

    /**
     * TODO(jean) Other implementation will track translation inheritance out of the box, but not this.
     * Though actually with current implementation the inheritance handling might be complicated. To review - how
     * do you know fr-CA delta needs to be changed when fr-FR is changed?
     * <p>
     * Ran locally this was very slow.. while we expected it to be the faster option
     */
    @StopWatch(level = INFO)
    @Transactional
    public void saveTextUnitVariantsServer(PullRunAsset pullRunAsset, List<Long> tuvIds, Long localeId) {
        Repository repository = pullRunAsset.getPullRun().getRepository();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        meterRegistry.timer("PullRunAssetService.saveTextUnitVariantsMultiRow", Tags.of("repositoryId", Objects.toString(repository.getId())))
                .record(() -> {
                    // TODO(jean) might be the fastest option (if data is clean on the server side),
                    //  inconvinient is that we over copy typically unused string
                    // we do need some of those unused strings since pull generate localized files regardless of the
                    // used/unused status. Maybe filter by timestamp?
                    jdbcTemplate.update("insert into pull_run_text_unit_variant (pull_run_asset_id, tm_text_unit_variant_id, created_date) " +
                                    "select ?, ttucv.tm_text_unit_variant_id, ? from tm_text_unit_current_variant ttucv " +
                                    "where ttucv.asset_id = ? and ttucv.locale_id = ? ",
                            pullRunAsset.getId(),
                            Instant.now(),
                            pullRunAsset.getAsset().getId(),
                            localeId);
                });
    }

    private void saveTextUnitVariantsBatch(PullRunAsset pullRunAsset, List<Long> tuvIds) {
        for (Long tuvId : tuvIds) {
            PullRunTextUnitVariant pullRunTextUnitVariant = new PullRunTextUnitVariant();
            pullRunTextUnitVariant.setPullRunAsset(pullRunAsset);
            pullRunTextUnitVariant.setTmTextUnitVariant(entityManager.getReference(TMTextUnitVariant.class, tuvId));
            pullRunTextUnitVariantRepository.save(pullRunTextUnitVariant);
        }
        entityManager.flush();
        entityManager.clear();
    }

    private void saveTextUnitVariantsPreparedStatementBatch(JdbcTemplate jdbcTemplate, PullRunAsset pullRunAsset, List<Long> tuvIds, Instant now) {
        // TODO(jean) is providing the date faster?
        jdbcTemplate.batchUpdate("insert into pull_run_text_unit_variant (pull_run_asset_id, tm_text_unit_variant_id, created_date) values (?, ?, ?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, pullRunAsset.getId());
                ps.setLong(2, tuvIds.get(i));
                ps.setTimestamp(3, Timestamp.from(now));
            }

            @Override
            public int getBatchSize() {
                return tuvIds.size();
            }
        });
    }

    private void saveTextUnitVariantsMultiRowBatch(JdbcTemplate jdbcTemplate, PullRunAsset pullRunAsset, List<Long> tuvIds, Instant now) {
        // TODO(jean) is providing the date faster?
        // TODO(jean) no protection from preparedstatement...
        String sql = "insert into pull_run_text_unit_variant(pull_run_asset_id, tm_text_unit_variant_id, created_date) values "
                + tuvIds.stream().map(id -> String.format("(%s, %s, '%s') ", pullRunAsset.getId(), id, Timestamp.from(now))).collect(Collectors.joining(","));
        jdbcTemplate.update(sql);
    }

    public PullRunAsset getOrCreate(PullRun pullRun, Asset asset) {
        return pullRunAssetRepository.findByPullRunAndAsset(pullRun, asset).orElseGet(() -> {
            PullRunAsset pullRunAsset = new PullRunAsset();
            pullRunAsset.setPullRun(pullRun);
            pullRunAsset.setAsset(asset);
            pullRunAssetRepository.saveAndFlush(pullRunAsset);
            return pullRunAsset;
        });
    }
}
