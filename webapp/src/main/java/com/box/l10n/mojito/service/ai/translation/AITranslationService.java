package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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

  private final Sinks.Many<TmTextUnitPendingMT> pendingMTDeletionSink =
      Sinks.many().multicast().onBackpressureBuffer();

  @Transactional
  public void createPendingMTEntitiesInBatches(Long repositoryId, Set<Long> tmTextUnitIds) {
    if (tmTextUnitIds.size() > maxTextUnitsAIRequest) {
      logger.warn(
          "Number of text units ({}) exceeds the maximum number of text units that can be sent for AI translation per request ({}). AI translation will be skipped.",
          tmTextUnitIds.size(),
          maxTextUnitsAIRequest);
      return;
    }
    if (repositoryLocaleAIPromptRepository.findCountOfActiveRepositoryPromptsByType(
            repositoryId, PromptType.TRANSLATION.toString())
        > 0) {
      createPendingMTEntitiesInBatches(tmTextUnitIds);
    } else {
      logger.debug("No active prompts for repository: {}, no job scheduled", repositoryId);
    }
  }

  protected void sendForDeletion(TmTextUnitPendingMT pendingMT) {
    logger.debug("Sending pending MT for deletion: {}", pendingMT);
    pendingMTDeletionSink.tryEmitNext(pendingMT);
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

  /**
   * Inserts AI translations into the database.
   *
   * @param translationDTOs
   */
  @Transactional
  protected void insertMultiRowAITranslationVariant(
      Long tmTextUnitId, List<AITranslation> translationDTOs) {
    insertMultiRowTextUnitVariants(tmTextUnitId, translationDTOs);
    insertMultiRowTextUnitCurrentVariants(tmTextUnitId, translationDTOs);
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
  private void deleteBatch(List<TmTextUnitPendingMT> batch) {
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

  @PostConstruct
  public void init() {
    Flux<TmTextUnitPendingMT> flux = pendingMTDeletionSink.asFlux();

    flux.bufferTimeout(batchSize, timeout)
        .filter(batch -> !batch.isEmpty())
        .subscribe(this::deleteBatch);
  }

  @PreDestroy
  public void destroy() {
    pendingMTDeletionSink.tryEmitComplete();
  }
}
