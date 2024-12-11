package com.box.l10n.mojito.service.ai.translation;

import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.MT_REVIEW_NEEDED;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.google.common.collect.Lists;
import jakarta.transaction.Transactional;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "l10n.ai.translation.enabled", havingValue = "true")
public class AITranslationService {

  private static final Logger logger = LoggerFactory.getLogger(AITranslationService.class);

  @Autowired RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Autowired TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired JdbcTemplate jdbcTemplate;

  @Value("${l10n.ai.translation.pendingMT.batchSize:1000}")
  int batchSize;

  @Value("${l10n.ai.translation.pendingMT.timeout:PT10S}")
  Duration timeout;

  @Value("${l10n.ai.translation.maxTextUnitsAIRequest:1000}")
  int maxTextUnitsAIRequest;

  @Transactional
  public void createPendingMTEntitiesInBatches(Long repositoryId, Set<Long> tmTextUnitIds) {
    if (repositoryLocaleAIPromptRepository.findCountOfActiveRepositoryPromptsByType(
            repositoryId, PromptType.TRANSLATION.toString())
        > 0) {
      if (tmTextUnitIds.size() > maxTextUnitsAIRequest) {
        logger.warn(
            "Number of text units ({}) exceeds the maximum number of text units that can be sent for AI translation per request ({}). AI translation will be skipped.",
            tmTextUnitIds.size(),
            maxTextUnitsAIRequest);
        return;
      }
      createPendingMTEntitiesInBatches(tmTextUnitIds);
    } else {
      logger.debug("No active prompts for repository: {}, no job scheduled", repositoryId);
    }
  }

  @Transactional
  public void updateVariantStatusToMTReviewNeeded(List<Long> currentVariantIds) {

    for (int i = 0; i < currentVariantIds.size(); i += batchSize) {
      logger.debug("Updating variant statuses to MT_REVIEW in batches of {}", batchSize);
      int end = Math.min(i + batchSize, currentVariantIds.size());
      List<Long> updateBatch = currentVariantIds.subList(i, end);
      executeVariantStatusUpdatesToMTReview(updateBatch);
    }
  }

  private void executeVariantStatusUpdatesToMTReview(List<Long> updateBatch) {
    String sql =
        "UPDATE tm_text_unit_variant "
            + "SET status = ? "
            + "WHERE id IN ("
            + "SELECT tucv.tm_text_unit_variant_id "
            + "FROM tm_text_unit_current_variant tucv "
            + "WHERE tucv.id = ?)";

    jdbcTemplate.batchUpdate(
        sql,
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            ps.setString(1, MT_REVIEW_NEEDED.name());
            ps.setLong(2, updateBatch.get(i));
          }

          @Override
          public int getBatchSize() {
            return updateBatch.size();
          }
        });
  }

  private void createPendingMTEntitiesInBatches(Set<Long> tmTextUnitIds) {
    List<TmTextUnitPendingMT> pendingMTs =
        tmTextUnitIds.stream()
            .map(AITranslationService::createTmTextUnitPendingMT)
            .collect(Collectors.toList());
    logger.debug("Persisting {} pending MTs", pendingMTs.size());
    Lists.partition(pendingMTs, batchSize).forEach(this::savePendingMTsMultiRowBatch);
  }

  private void savePendingMTsMultiRowBatch(List<TmTextUnitPendingMT> pendingMTs) {
    String sql =
        "INSERT INTO tm_text_unit_pending_mt(tm_text_unit_id, created_date) VALUES"
            + pendingMTs.stream()
                .map(
                    tmTextUnitPendingMT ->
                        String.format(
                            "(%d, '%s')",
                            tmTextUnitPendingMT.getTmTextUnitId(),
                            Timestamp.from(tmTextUnitPendingMT.getCreatedDate().toInstant())))
                .collect(Collectors.joining(","));
    logger.debug("Executing batch insert for {} pending MTs", pendingMTs.size());
    jdbcTemplate.update(sql);
  }

  private void insertMultiRowTextUnitVariants(
      Long textUnitId, List<AITranslation> translationDTOs) {
    logger.debug(
        "Inserting {} translation variants for text unit ID: {}",
        translationDTOs.size(),
        textUnitId);

    String sql =
        "INSERT INTO tm_text_unit_variant (tm_text_unit_id, locale_id, content, content_md5, status, included_in_localized_file, created_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
    List<Object[]> batchArgs =
        translationDTOs.stream()
            .map(
                translationDTO ->
                    new Object[] {
                      translationDTO.getTmTextUnit().getId(),
                      translationDTO.getLocaleId(),
                      translationDTO.getTranslation(),
                      translationDTO.getContentMd5(),
                      translationDTO.getStatus().toString(),
                      translationDTO.isIncludedInLocalizedFile(),
                      Timestamp.from(translationDTO.getCreatedDate().toInstant())
                    })
            .collect(Collectors.toList());

    logger.debug("Executing batch insert for {} translation variants", translationDTOs.size());

    jdbcTemplate.batchUpdate(sql, batchArgs);
  }

  private void insertMultiRowTextUnitCurrentVariants(
      Long textUnitId, List<AITranslation> translationDTOs) {
    logger.debug(
        "Inserting {} current variants for text unit ID: {}", translationDTOs.size(), textUnitId);

    String sql =
        "INSERT INTO tm_text_unit_current_variant (tm_id, asset_id, tm_text_unit_id, tm_text_unit_variant_id, locale_id, created_date, last_modified_date) VALUES (?, ?, ?, ?, ?, ?, ?)";

    Map<Long, Long> localeToVariantIds =
        tmTextUnitVariantRepository.findLocaleVariantDTOsByTmTextUnitId(textUnitId).stream()
            .collect(
                Collectors.toMap(LocaleVariantDTO::getLocaleId, LocaleVariantDTO::getVariantId));

    List<Object[]> batchArgs =
        translationDTOs.stream()
            .map(
                translationDTO ->
                    new Object[] {
                      translationDTO.getTmTextUnit().getTm().getId(),
                      translationDTO.getTmTextUnit().getAsset().getId(),
                      translationDTO.getTmTextUnit().getId(),
                      localeToVariantIds.get(translationDTO.getLocaleId()),
                      translationDTO.getLocaleId(),
                      Timestamp.from(translationDTO.getCreatedDate().toInstant()),
                      Timestamp.from(translationDTO.getCreatedDate().toInstant())
                    })
            .toList();

    logger.debug(
        "Executing batch insert for {} translation current variants", translationDTOs.size());

    jdbcTemplate.batchUpdate(sql, batchArgs);
  }

  @Transactional
  protected void deleteBatch(Queue<TmTextUnitPendingMT> batch) {
    if (batch.isEmpty()) {
      logger.debug("No pending MTs to delete");
      return;
    }

    String sql =
        "DELETE FROM tm_text_unit_pending_mt WHERE id IN ("
            + batch.stream()
                .map(tmTextUnitPendingMT -> tmTextUnitPendingMT.getId().toString())
                .collect(Collectors.joining(","))
            + ")";
    logger.debug(
        "Executing batch delete for IDs: {}",
        batch.stream().map(TmTextUnitPendingMT::getId).collect(Collectors.toList()));
    jdbcTemplate.update(sql);
  }

  private static TmTextUnitPendingMT createTmTextUnitPendingMT(Long tmTextUnitId) {
    TmTextUnitPendingMT tmTextUnitPendingMT = new TmTextUnitPendingMT();
    tmTextUnitPendingMT.setTmTextUnitId(tmTextUnitId);
    tmTextUnitPendingMT.setCreatedDate(JSR310Migration.newDateTimeEmptyCtor());
    return tmTextUnitPendingMT;
  }
}
