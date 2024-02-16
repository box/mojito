package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PullRunTextUnitVariant;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.retry.DeadLockLoserExceptionRetryTemplate;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

  @Autowired PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired MeterRegistry meterRegistry;

  @Autowired DeadLockLoserExceptionRetryTemplate deadLockLoserExceptionRetryTemplate;

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
    try (var timer =
        Timer.resource(meterRegistry, "PullRunAssetService.saveTextUnitVariantsMultiRow")
            .tag("repositoryId", Objects.toString(repository.getId()))) {
      deadLockLoserExceptionRetryTemplate.execute(
          context -> {
            deleteExistingVariants(pullRunAsset, localeId, outputBcp47Tag);
            return null;
          });

      Lists.partition(uniqueTmTextUnitVariantIds, BATCH_SIZE)
          .forEach(
              tuvIdsBatch ->
                  deadLockLoserExceptionRetryTemplate.execute(
                      context -> {
                        saveTextUnitVariantsMultiRowBatch(
                            pullRunAsset, localeId, tuvIdsBatch, outputBcp47Tag);
                        return null;
                      }));
    }
  }

  @Transactional
  void deleteExistingVariants(PullRunAsset pullRunAsset, Long localeId, String outputBcp47Tag) {
    // Delete and insert steps split into two transactions to avoid deadlocks occurring

    List<PullRunTextUnitVariant> pullRunTextUnitVariants =
        pullRunTextUnitVariantRepository.findByPullRunAssetIdAndLocaleIdAndOutputBcp47Tag(
            pullRunAsset.getId(), localeId, outputBcp47Tag);

    if (!pullRunTextUnitVariants.isEmpty()) {
      // Delete the rows if ids are not empty
      NamedParameterJdbcTemplate namedParameterJdbcTemplate =
          new NamedParameterJdbcTemplate(jdbcTemplate);

      Lists.partition(
              pullRunTextUnitVariants.stream()
                  .map(PullRunTextUnitVariant::getId)
                  .collect(Collectors.toList()),
              BATCH_SIZE)
          .forEach(
              idsBatch -> {
                MapSqlParameterSource parameters = new MapSqlParameterSource();
                parameters.addValue("ids", idsBatch);
                namedParameterJdbcTemplate.update(
                    "delete from pull_run_text_unit_variant where id in (:ids)", parameters);
              });
    }
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
