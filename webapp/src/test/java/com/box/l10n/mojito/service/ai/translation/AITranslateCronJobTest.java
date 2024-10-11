package com.box.l10n.mojito.service.ai.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.service.ai.LLMService;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.google.common.collect.Sets;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class AITranslateCronJobTest {

  @Mock TMService tmService;

  @Mock MeterRegistry meterRegistry;

  @Mock LLMService llmService;

  @Mock TMTextUnitRepository tmTextUnitRepository;

  @Mock TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Mock RepositoryRepository repositoryRepository;

  @Mock RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Mock AITranslationTextUnitFilterService aiTranslationTextUnitFilterService;

  @Mock AIPrompt aiPrompt;

  @Mock TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Mock Repository repository;

  @Mock AITranslationService aiTranslationService;

  AITranslateCronJob aiTranslateCronJob;

  TMTextUnit tmTextUnit;

  TmTextUnitPendingMT tmTextUnitPendingMT;

  Locale german;

  AITranslationConfiguration aITranslationConfiguration;

  ArgumentCaptor<List<AITranslation>> aiTranslationCaptor = ArgumentCaptor.forClass(List.class);

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    aiTranslateCronJob = new AITranslateCronJob();
    aiTranslateCronJob.meterRegistry = meterRegistry;
    aiTranslateCronJob.llmService = llmService;
    aiTranslateCronJob.tmTextUnitRepository = tmTextUnitRepository;
    aiTranslateCronJob.tmTextUnitPendingMTRepository = tmTextUnitPendingMTRepository;
    aiTranslateCronJob.repositoryLocaleAIPromptRepository = repositoryLocaleAIPromptRepository;
    aiTranslateCronJob.aiTranslationTextUnitFilterService = aiTranslationTextUnitFilterService;
    aiTranslateCronJob.tmTextUnitVariantRepository = tmTextUnitVariantRepository;
    aiTranslateCronJob.aiTranslationService = aiTranslationService;
    aITranslationConfiguration = new AITranslationConfiguration();
    aITranslationConfiguration.setEnabled(true);
    aITranslationConfiguration.setCron("0 0/10 * * * ?");
    aITranslationConfiguration.setBatchSize(5);
    aITranslationConfiguration.setExpiryDuration(Duration.ofHours(3));
    aiTranslateCronJob.threads = 1;
    AITranslationConfiguration.RepositorySettings repositorySettings =
        new AITranslationConfiguration.RepositorySettings();
    repositorySettings.setReuseSourceOnLanguageMatch(false);
    Map<String, AITranslationConfiguration.RepositorySettings> repositorySettingsMap =
        Collections.singletonMap("testRepo", repositorySettings);
    aITranslationConfiguration.setRepositorySettings(repositorySettingsMap);
    aiTranslateCronJob.aiTranslationConfiguration = aITranslationConfiguration;
    Repository testRepo = new Repository();
    testRepo.setId(1L);
    testRepo.setName("testRepo");
    Locale english = new Locale();
    english.setBcp47Tag("en-GB");
    english.setId(1L);
    RepositoryLocale englishRepoLocale = new RepositoryLocale(testRepo, english, true, null);
    Locale french = new Locale();
    french.setBcp47Tag("fr-FR");
    french.setId(2L);
    german = new Locale();
    german.setBcp47Tag("de-DE");
    german.setId(3L);
    Locale hibernoEnglish = new Locale();
    hibernoEnglish.setBcp47Tag("en-IE");
    hibernoEnglish.setId(4L);
    RepositoryLocale frenchRepoLocale =
        new RepositoryLocale(testRepo, french, true, englishRepoLocale);
    frenchRepoLocale.setId(2L);
    RepositoryLocale germanRepoLocale =
        new RepositoryLocale(testRepo, german, true, englishRepoLocale);
    germanRepoLocale.setId(3L);
    RepositoryLocale hibernoEnglishRepoLocale =
        new RepositoryLocale(testRepo, hibernoEnglish, true, englishRepoLocale);
    hibernoEnglishRepoLocale.setId(4L);
    testRepo.setRepositoryLocales(
        Sets.newHashSet(frenchRepoLocale, germanRepoLocale, hibernoEnglishRepoLocale));
    testRepo.setSourceLocale(english);

    when(repository.getSourceLocale()).thenReturn(english);
    when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepo));
    tmTextUnitPendingMT = new TmTextUnitPendingMT();
    tmTextUnitPendingMT.setTmTextUnitId(1L);
    tmTextUnitPendingMT.setId(1L);
    tmTextUnitPendingMT.setCreatedDate(JSR310Migration.dateTimeNow());
    when(tmTextUnitPendingMTRepository.findByTmTextUnitId(1L)).thenReturn(tmTextUnitPendingMT);
    when(aiTranslationTextUnitFilterService.isTranslatable(
            isA(TMTextUnit.class), isA(Repository.class)))
        .thenReturn(true);

    RepositoryLocaleAIPrompt testPrompt1 = new RepositoryLocaleAIPrompt();
    testPrompt1.setId(1L);
    testPrompt1.setRepository(testRepo);
    testPrompt1.setLocale(french);
    testPrompt1.setAiPrompt(aiPrompt);

    RepositoryLocaleAIPrompt testPrompt2 = new RepositoryLocaleAIPrompt();
    testPrompt2.setId(2L);
    testPrompt2.setRepository(testRepo);
    testPrompt2.setLocale(german);
    testPrompt2.setAiPrompt(aiPrompt);

    RepositoryLocaleAIPrompt testPrompt3 = new RepositoryLocaleAIPrompt();
    testPrompt3.setId(3L);
    testPrompt3.setRepository(testRepo);
    testPrompt3.setLocale(null);
    testPrompt3.setAiPrompt(aiPrompt);

    tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("content");
    tmTextUnit.setComment("comment");
    tmTextUnit.setName("name");
    Asset asset = new Asset();
    asset.setRepository(repository);
    tmTextUnit.setAsset(asset);
    when(tmTextUnitRepository.findById(1L)).thenReturn(Optional.of(tmTextUnit));
    when(repositoryLocaleAIPromptRepository.getActivePromptsByRepositoryAndPromptType(
            testRepo.getId(), PromptType.TRANSLATION.toString()))
        .thenReturn(Lists.list(testPrompt1, testPrompt2, testPrompt3));
    when(llmService.translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class)))
        .thenReturn("translated");
    TMTextUnitCurrentVariant tmTextUnitCurrentVariant = new TMTextUnitCurrentVariant();
    tmTextUnitCurrentVariant.setLocale(english);
    when(repository.getId()).thenReturn(1L);
    when(repository.getName()).thenReturn("testRepo");
    when(repository.getRepositoryLocales())
        .thenReturn(
            Sets.newHashSet(
                englishRepoLocale, frenchRepoLocale, germanRepoLocale, hibernoEnglishRepoLocale));
    when(repository.getSourceLocale()).thenReturn(english);
    when(meterRegistry.timer(anyString(), isA((Iterable.class))))
        .thenReturn(mock(io.micrometer.core.instrument.Timer.class));
    when(tmTextUnitRepository.findByIdWithAssetAndRepositoryAndTMFetched(1L))
        .thenReturn(Optional.of(tmTextUnit));
    when(meterRegistry.counter(anyString()))
        .thenReturn(mock(io.micrometer.core.instrument.Counter.class));
  }

  @Test
  public void testTranslateSuccess() throws Exception {
    aiTranslateCronJob.translate(repository, tmTextUnit, tmTextUnitPendingMT);
    verify(llmService, times(3))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(aiTranslationService, times(1))
        .insertMultiRowAITranslationVariant(anyLong(), aiTranslationCaptor.capture());
    List<AITranslation> aiTranslations = aiTranslationCaptor.getValue();
    assertEquals(3, aiTranslations.size());
    assertThat(aiTranslations).extracting("localeId").containsExactlyInAnyOrder(2L, 3L, 4L);
  }

  @Test
  public void testTranslateFailure() throws Exception {
    aiTranslateCronJob.translate(repository, tmTextUnit, tmTextUnitPendingMT);
    when(llmService.translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class)))
        .thenThrow(new RuntimeException("test"));
    verify(llmService, times(3))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
  }

  @Test
  public void testTranslateReuseSource() throws Exception {
    AITranslationConfiguration.RepositorySettings repositorySettings =
        new AITranslationConfiguration.RepositorySettings();
    repositorySettings.setReuseSourceOnLanguageMatch(true);
    aITranslationConfiguration.setRepositorySettings(
        Collections.singletonMap("testRepo", repositorySettings));
    aiTranslateCronJob.translate(repository, tmTextUnit, tmTextUnitPendingMT);
    verify(llmService, times(2))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(aiTranslationService, times(1))
        .insertMultiRowAITranslationVariant(anyLong(), aiTranslationCaptor.capture());
    List<AITranslation> aiTranslations = aiTranslationCaptor.getValue();
    assertEquals(3, aiTranslations.size());
    assertThat(aiTranslations).extracting("localeId").containsExactlyInAnyOrder(2L, 3L, 4L);
    assertThat(aiTranslations)
        .extracting("translation")
        .containsExactlyInAnyOrder("content", "translated", "translated");
  }

  @Test
  public void testNoTranslationIfLeveragedVariantExistsForLocale() {
    TMTextUnitCurrentVariant tmTextUnitCurrentVariant = new TMTextUnitCurrentVariant();
    tmTextUnitCurrentVariant.setLocale(german);
    Set<Locale> variants = Sets.newHashSet(tmTextUnitCurrentVariant.getLocale());
    when(tmTextUnitVariantRepository.findLocalesWithVariantByTmTextUnit_Id(1L))
        .thenReturn(variants);
    aiTranslateCronJob.translate(repository, tmTextUnit, tmTextUnitPendingMT);
    verify(llmService, times(2))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(aiTranslationService, times(1))
        .insertMultiRowAITranslationVariant(anyLong(), aiTranslationCaptor.capture());
    List<AITranslation> aiTranslations = aiTranslationCaptor.getValue();
    assertEquals(2, aiTranslations.size());
    assertThat(aiTranslations).extracting("localeId").containsExactlyInAnyOrder(2L, 4L);
  }

  @Test
  public void testPendingMTEntityIsExpired() throws Exception {
    TmTextUnitPendingMT expiredPendingMT = new TmTextUnitPendingMT();
    expiredPendingMT.setCreatedDate(ZonedDateTime.now().minusHours(4));
    when(tmTextUnitPendingMTRepository.findByTmTextUnitId(1L)).thenReturn(expiredPendingMT);
    aiTranslateCronJob.translate(repository, tmTextUnit, expiredPendingMT);
    verify(llmService, never())
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(2L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(3L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(4L),
            eq("content"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(meterRegistry, times(1)).counter(eq("AITranslateCronJob.expired"), any(Tags.class));
  }

  @Test
  public void testFilterMatchNoTranslation() throws Exception {
    when(aiTranslationTextUnitFilterService.isTranslatable(
            isA(TMTextUnit.class), isA(Repository.class)))
        .thenReturn(false);
    aiTranslateCronJob.translate(repository, tmTextUnit, tmTextUnitPendingMT);
    verify(llmService, never())
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(2L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(3L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(4L),
            eq("content"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
  }

  @Test
  public void testBatchLogic() throws JobExecutionException {
    List<TmTextUnitPendingMT> pendingMTList =
        Lists.list(
            tmTextUnitPendingMT,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT);
    when(tmTextUnitPendingMTRepository.findBatch(5))
        .thenReturn(pendingMTList)
        .thenReturn(pendingMTList)
        .thenReturn(Collections.emptyList());
    aiTranslateCronJob.execute(mock(JobExecutionContext.class));
    verify(llmService, times(30))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(aiTranslationService, times(10))
        .insertMultiRowAITranslationVariant(anyLong(), aiTranslationCaptor.capture());
    List<AITranslation> aiTranslations = aiTranslationCaptor.getValue();
    for (int i = 0; i < aiTranslationCaptor.getAllValues().size(); i++) {
      aiTranslations = aiTranslationCaptor.getAllValues().get(i);
      assertEquals(3, aiTranslations.size());
      assertThat(aiTranslations).extracting("localeId").containsExactlyInAnyOrder(2L, 3L, 4L);
    }
    verify(aiTranslationService, times(10)).sendForDeletion(isA(TmTextUnitPendingMT.class));
  }

  @Test
  public void testBatchRequestFailLogic() throws JobExecutionException {
    // Test verifies that if a single locale fails to translate, the rest of the batch is still
    // processed
    List<TmTextUnitPendingMT> pendingMTList =
        Lists.list(
            tmTextUnitPendingMT,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT);
    when(tmTextUnitPendingMTRepository.findBatch(5))
        .thenReturn(pendingMTList)
        .thenReturn(pendingMTList)
        .thenReturn(Collections.emptyList());
    when(llmService.translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class)))
        .thenThrow(new RuntimeException("test"))
        .thenReturn("translated");
    aiTranslateCronJob.execute(mock(JobExecutionContext.class));
    verify(llmService, times(30))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(aiTranslationService, times(10))
        .insertMultiRowAITranslationVariant(anyLong(), aiTranslationCaptor.capture());
    List<AITranslation> aiTranslations = aiTranslationCaptor.getAllValues().getFirst();
    assertEquals(2, aiTranslations.size());
    for (int i = 1; i < aiTranslationCaptor.getAllValues().size(); i++) {
      aiTranslations = aiTranslationCaptor.getAllValues().get(i);
      assertEquals(3, aiTranslations.size());
      assertThat(aiTranslations).extracting("localeId").containsExactlyInAnyOrder(2L, 3L, 4L);
    }
    verify(aiTranslationService, times(10)).sendForDeletion(isA(TmTextUnitPendingMT.class));
  }

  @Test
  public void testBatchLogicFailureToRetrieveTextUnit() throws JobExecutionException {
    // Test verifies that if an exception is thrown for a single text unit, the rest of the batch is
    // still processed
    TmTextUnitPendingMT tmTextUnitPendingMT2 = new TmTextUnitPendingMT();
    tmTextUnitPendingMT2.setTmTextUnitId(2L);
    List<TmTextUnitPendingMT> pendingMTList =
        Lists.list(
            tmTextUnitPendingMT2,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT,
            tmTextUnitPendingMT);
    when(tmTextUnitPendingMTRepository.findBatch(5))
        .thenReturn(pendingMTList)
        .thenReturn(Collections.emptyList());
    when(tmTextUnitRepository.findByIdWithAssetAndRepositoryAndTMFetched(2L))
        .thenReturn(Optional.empty());
    aiTranslateCronJob.execute(mock(JobExecutionContext.class));
    verify(llmService, times(12))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(aiTranslationService, times(4))
        .insertMultiRowAITranslationVariant(anyLong(), aiTranslationCaptor.capture());
    List<AITranslation> aiTranslations = aiTranslationCaptor.getAllValues().getFirst();
    assertEquals(3, aiTranslations.size());
    for (int i = 0; i < aiTranslationCaptor.getAllValues().size(); i++) {
      aiTranslations = aiTranslationCaptor.getAllValues().get(i);
      assertEquals(3, aiTranslations.size());
      assertThat(aiTranslations).extracting("localeId").containsExactlyInAnyOrder(2L, 3L, 4L);
    }
    verify(aiTranslationService, times(5)).sendForDeletion(isA(TmTextUnitPendingMT.class));
  }
}
