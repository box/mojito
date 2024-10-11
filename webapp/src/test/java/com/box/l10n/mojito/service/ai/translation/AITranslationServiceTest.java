package com.box.l10n.mojito.service.ai.translation;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
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
  public void testMultiRowAITranslationVariantInserts() {
    TM tm = new TM();
    tm.setId(1L);
    Asset asset = new Asset();
    asset.setId(1L);
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setAsset(asset);
    tmTextUnit.setTm(tm);
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("content");
    tmTextUnit.setContentMd5("md5");
    AITranslation aiTranslation = new AITranslation();
    aiTranslation.setTmTextUnit(tmTextUnit);
    aiTranslation.setLocaleId(1L);
    aiTranslation.setTranslation("translation");
    aiTranslation.setStatus(TMTextUnitVariant.Status.MT_TRANSLATED);
    aiTranslation.setCreatedDate(JSR310Migration.newDateTimeEmptyCtor());
    aiTranslation.setIncludedInLocalizedFile(false);

    AITranslation aiTranslation2 = new AITranslation();
    aiTranslation2.setTmTextUnit(tmTextUnit);
    aiTranslation2.setLocaleId(2L);
    aiTranslation2.setTranslation("translation2");
    aiTranslation2.setStatus(TMTextUnitVariant.Status.MT_TRANSLATED);
    aiTranslation2.setCreatedDate(JSR310Migration.newDateTimeEmptyCtor());
    aiTranslation2.setIncludedInLocalizedFile(false);

    AITranslation aiTranslation3 = new AITranslation();
    aiTranslation3.setTmTextUnit(tmTextUnit);
    aiTranslation3.setLocaleId(3L);
    aiTranslation3.setTranslation("translation3");
    aiTranslation3.setStatus(TMTextUnitVariant.Status.MT_TRANSLATED);
    aiTranslation3.setCreatedDate(JSR310Migration.newDateTimeEmptyCtor());
    aiTranslation3.setIncludedInLocalizedFile(false);
    List<AITranslation> translations = List.of(aiTranslation, aiTranslation2, aiTranslation3);

    aiTranslationService.insertMultiRowAITranslationVariant(1L, translations);

    verify(jdbcTemplate, times(2)).batchUpdate(anyString(), anyList());
  }
}
