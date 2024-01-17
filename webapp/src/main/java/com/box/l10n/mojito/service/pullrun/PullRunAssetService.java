package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.Repository;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage PullRunAsset entities.
 *
 * @author garion
 */
@Service
public class PullRunAssetService {

  static final int BATCH_SIZE = 1000;

  @Autowired PullRunAssetRepository pullRunAssetRepository;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired MeterRegistry meterRegistry;

  public PullRunAsset createPullRunAsset(PullRun pullRun, Asset asset) {
    PullRunAsset pullRunAsset = new PullRunAsset();
    pullRunAsset.setPullRun(pullRun);
    pullRunAsset.setAsset(asset);

    return pullRunAssetRepository.save(pullRunAsset);
  }

  public PullRunAsset getOrCreate(PullRun pullRun, Asset asset) {
    return pullRunAssetRepository
        .findByPullRunAndAsset(pullRun, asset)
        .orElseGet(() -> createPullRunAsset(pullRun, asset));
  }

  public void replaceTextUnitVariants(
      PullRunAsset pullRunAsset,
      Long localeId,
      List<Long> uniqueTmTextUnitVariantIds,
      String outputBcp47Tag) {
    Repository repository = pullRunAsset.getPullRun().getRepository();
    meterRegistry
        .timer(
            "PullRunAssetService.saveTextUnitVariantsMultiRow",
            Tags.of("repositoryId", Objects.toString(repository.getId())))
        .record(
            () -> {
              deleteExistingVariants(pullRunAsset, localeId, outputBcp47Tag);
              Lists.partition(uniqueTmTextUnitVariantIds, BATCH_SIZE)
                  .forEach(
                      tuvIdsBatch ->
                          saveTextUnitVariantsMultiRowBatch(
                              pullRunAsset, localeId, tuvIdsBatch, outputBcp47Tag));
            });
  }

  @Transactional
  void deleteExistingVariants(PullRunAsset pullRunAsset, Long localeId, String outputBcp47Tag) {
    // Delete and insert steps split into two transactions to avoid deadlocks occurring
    jdbcTemplate.update(
        "delete from pull_run_text_unit_variant where pull_run_asset_id = ? and locale_id = ? and output_bcp47_tag = ?",
        pullRunAsset.getId(),
        localeId,
        outputBcp47Tag);
  }

  @Transactional
  void saveTextUnitVariantsMultiRowBatch(
      PullRunAsset pullRunAsset,
      Long localeId,
      List<Long> uniqueTmTextUnitVariantIds,
      String outputBcp47Tag) {

    ZonedDateTime createdTime = ZonedDateTime.now();

    String sql =
        "insert into pull_run_text_unit_variant(pull_run_asset_id, locale_id, tm_text_unit_variant_id, created_date, output_bcp47_tag) values "
            + uniqueTmTextUnitVariantIds.stream()
                .map(
                    tuvId ->
                        String.format(
                            "(%s, %s, %s, '%s', '%s') ",
                            pullRunAsset.getId(),
                            localeId,
                            tuvId,
                            JSR310Migration.toRawSQL(createdTime),
                            outputBcp47Tag))
                .collect(Collectors.joining(","));
    jdbcTemplate.update(sql);
  }
}
