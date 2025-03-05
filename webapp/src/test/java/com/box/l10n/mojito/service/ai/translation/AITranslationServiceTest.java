package com.box.l10n.mojito.service.ai.translation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class AITranslationServiceTest {

  @Mock JdbcTemplate jdbcTemplate;

  @Mock TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Mock TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Mock RepositoryRepository repositoryRepository;

  @Mock RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  AITranslationService aiTranslationService;

  ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

  @Before
  public void before() {
    MockitoAnnotations.openMocks(this);
    aiTranslationService = new AITranslationService();
    aiTranslationService.jdbcTemplate = jdbcTemplate;
    aiTranslationService.tmTextUnitPendingMTRepository = tmTextUnitPendingMTRepository;
    aiTranslationService.repositoryRepository = repositoryRepository;
    aiTranslationService.tmTextUnitVariantRepository = tmTextUnitVariantRepository;
    aiTranslationService.repositoryLocaleAIPromptRepository = repositoryLocaleAIPromptRepository;
    aiTranslationService.batchSize = 5;
    aiTranslationService.timeout = Duration.ofSeconds(10);
    aiTranslationService.maxTextUnitsAIRequest = 10;

    when(repositoryLocaleAIPromptRepository.findCountOfActiveRepositoryPromptsByType(
            1L, PromptType.TRANSLATION.toString()))
        .thenReturn(1L);
  }

  @Test
  public void testSuccessfulCreatePendingMTEntitiesInBatches() {
    aiTranslationService.createPendingMTEntitiesInBatches(
        1L, Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));
    verify(jdbcTemplate, times(2)).update(stringArgumentCaptor.capture());
  }

  @Test
  public void testCreatePendingMTEntitiesInBatchesSkipsWhenTooManyTextUnits() {
    Set<Long> tmTextUnitIds = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
    aiTranslationService.maxTextUnitsAIRequest = 10;

    aiTranslationService.createPendingMTEntitiesInBatches(1L, tmTextUnitIds);

    verify(jdbcTemplate, times(0)).update(anyString());
  }

  @Test
  public void testCreatePendingMTEntitiesInBatchesCreatesWhenActivePromptsExist() {
    Set<Long> tmTextUnitIds = Set.of(1L, 2L, 3L);
    when(repositoryLocaleAIPromptRepository.findCountOfActiveRepositoryPromptsByType(
            1L, PromptType.TRANSLATION.toString()))
        .thenReturn(1L);

    aiTranslationService.createPendingMTEntitiesInBatches(1L, tmTextUnitIds);

    verify(jdbcTemplate, times(1)).update(anyString());
  }

  @Test
  public void testCreatePendingMTEntitiesInBatchesSkipsWhenNoActivePrompts() {
    Set<Long> tmTextUnitIds = Set.of(1L, 2L, 3L);
    when(repositoryLocaleAIPromptRepository.findCountOfActiveRepositoryPromptsByType(
            1L, PromptType.TRANSLATION.toString()))
        .thenReturn(0L);

    aiTranslationService.createPendingMTEntitiesInBatches(1L, tmTextUnitIds);

    verify(jdbcTemplate, times(0)).update(anyString());
  }

  @Test
  public void testUpdateVariantStatusToMTReviewNeeded() {
    aiTranslationService.updateVariantStatusToMTReviewNeeded(List.of(1L, 2L, 3L));
    verify(jdbcTemplate, times(1))
        .batchUpdate(
            eq(
                "UPDATE tm_text_unit_variant SET status = ? WHERE id IN (SELECT tucv.tm_text_unit_variant_id FROM tm_text_unit_current_variant tucv WHERE tucv.id = ?)"),
            any(BatchPreparedStatementSetter.class));
    verify(jdbcTemplate, times(1))
        .batchUpdate(
            eq(
                "INSERT INTO tm_text_unit_variant_comment (tm_text_unit_variant_id, severity, type, content, created_date, last_modified_date)  VALUES (?, ?, ?, ?, ?, ?)"),
            any(List.class));
  }
}
