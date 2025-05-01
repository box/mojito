package com.box.l10n.mojito.service.ai.translation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.service.ai.LLMService;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.GlossaryCacheService;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.GlossaryTerm;
import com.box.l10n.mojito.service.tm.AddTMTextUnitCurrentVariantResult;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.collect.Sets;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
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

  @Mock TextUnitSearcher textUnitSearcher;

  @Mock TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

  @Mock GlossaryCacheService glossaryCacheService;

  AITranslateCronJob aiTranslateCronJob;

  TMTextUnit tmTextUnit;

  TmTextUnitPendingMT tmTextUnitPendingMT;

  Locale german;

  AITranslationConfiguration aITranslationConfiguration;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    aiTranslateCronJob = new AITranslateCronJob();
    aiTranslateCronJob.meterRegistry = meterRegistry;
    aiTranslateCronJob.llmService = llmService;
    aiTranslateCronJob.tmService = tmService;
    aiTranslateCronJob.tmTextUnitRepository = tmTextUnitRepository;
    aiTranslateCronJob.tmTextUnitPendingMTRepository = tmTextUnitPendingMTRepository;
    aiTranslateCronJob.repositoryLocaleAIPromptRepository = repositoryLocaleAIPromptRepository;
    aiTranslateCronJob.aiTranslationTextUnitFilterService = aiTranslationTextUnitFilterService;
    aiTranslateCronJob.tmTextUnitVariantRepository = tmTextUnitVariantRepository;
    aiTranslateCronJob.aiTranslationService = aiTranslationService;
    aiTranslateCronJob.textUnitSearcher = textUnitSearcher;
    aiTranslateCronJob.glossaryCacheService = glossaryCacheService;
    aiTranslateCronJob.tmTextUnitVariantCommentService = tmTextUnitVariantCommentService;
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
    when(llmService.translate(
            isA(TMTextUnit.class),
            isA(String.class),
            isA(String.class),
            isA(AIPrompt.class),
            isA(List.class)))
        .thenReturn("translated with glossary terms");
    TMTextUnitCurrentVariant tmTextUnitCurrentVariant = new TMTextUnitCurrentVariant();
    tmTextUnitCurrentVariant.setLocale(english);
    tmTextUnitCurrentVariant.setId(1L);
    TMTextUnitVariant tmTextUnitVariant = new TMTextUnitVariant();
    tmTextUnitVariant.setLocale(english);
    tmTextUnitVariant.setId(1L);
    tmTextUnitCurrentVariant.setTmTextUnitVariant(tmTextUnitVariant);
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
    Counter mockCounter = mock(Counter.class);
    when(meterRegistry.counter(anyString())).thenReturn(mockCounter);
    when(meterRegistry.counter(anyString(), isA(Iterable.class))).thenReturn(mockCounter);
    when(textUnitSearcher.search(isA(TextUnitSearcherParameters.class)))
        .thenReturn(Collections.emptyList());
    when(tmService.addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            any(TMTextUnitVariant.Status.class),
            anyBoolean(),
            isA(ZonedDateTime.class)))
        .thenReturn(new AddTMTextUnitCurrentVariantResult(false, tmTextUnitCurrentVariant));
    GlossaryTerm glossaryTerm = new GlossaryTerm();
    glossaryTerm.setTmTextUnitId(1L);
    glossaryTerm.setText("Test text");
    glossaryTerm.setTranslations(
        Map.of("fr-FR", "Test text in French", "de-DE", "Test text in German"));
    List<GlossaryTerm> glossaryTerms = List.of(glossaryTerm);
    when(glossaryCacheService.getGlossaryTermsInText(isA(String.class))).thenReturn(glossaryTerms);
  }

  @Test
  public void testTranslateSuccess() throws Exception {
    aiTranslateCronJob.translate(repository, tmTextUnit, tmTextUnitPendingMT);
    verify(llmService, times(3))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, times(3))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, times(3))
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
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
    verify(tmService, times(3))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, times(3))
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
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
    verify(tmService, times(2))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, times(2))
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
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
    verify(tmTextUnitVariantCommentService, never())
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
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
    verify(tmTextUnitVariantCommentService, never())
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
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
    verify(tmService, times(30))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, times(30))
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
    verify(aiTranslationService, times(3)).deleteBatch(isA(Queue.class));
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
    verify(tmService, times(29))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, times(29))
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
    verify(aiTranslationService, times(3)).deleteBatch(isA(Queue.class));
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
    verify(tmService, times(12))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, times(12))
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
    verify(aiTranslationService, times(2)).deleteBatch(isA(Queue.class));
  }

  @Test
  public void skipTranslationIfTUNotUsed() throws JobExecutionException {
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
    TextUnitDTO dto = new TextUnitDTO();
    dto.setTmTextUnitId(tmTextUnitPendingMT.getId());
    when(textUnitSearcher.search(isA(TextUnitSearcherParameters.class))).thenReturn(List.of(dto));
    aiTranslateCronJob.execute(mock(JobExecutionContext.class));
    verify(llmService, times(0))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, times(0))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, never())
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
    verify(aiTranslationService, times(3)).deleteBatch(isA(Queue.class));
    verify(aiTranslationService, times(2)).deleteBatch(argThat(q -> !q.isEmpty()));
  }

  @Test
  public void testInjectGlossaryTermsEnabled() {
    AITranslationConfiguration.RepositorySettings repositorySettings =
        new AITranslationConfiguration.RepositorySettings();
    repositorySettings.setReuseSourceOnLanguageMatch(false);
    repositorySettings.setInjectGlossaryMatches(true);
    Map<String, AITranslationConfiguration.RepositorySettings> repositorySettingsMap =
        Collections.singletonMap("testRepo", repositorySettings);
    aITranslationConfiguration.setRepositorySettings(repositorySettingsMap);
    aiTranslateCronJob.translate(repository, tmTextUnit, tmTextUnitPendingMT);
    verify(glossaryCacheService, times(3)).getGlossaryTermsInText(tmTextUnit.getContent());
    verify(llmService, times(3))
        .translate(
            isA(TMTextUnit.class),
            isA(String.class),
            isA(String.class),
            isA(AIPrompt.class),
            isA(List.class));
    verify(tmService, times(3))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, times(3))
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
  }

  @Test
  public void testGlossaryMatchesWithNoRelevantTranslationFilteredOut() {
    AITranslationConfiguration.RepositorySettings repositorySettings =
        new AITranslationConfiguration.RepositorySettings();
    repositorySettings.setReuseSourceOnLanguageMatch(false);
    repositorySettings.setInjectGlossaryMatches(true);
    Map<String, AITranslationConfiguration.RepositorySettings> repositorySettingsMap =
        Collections.singletonMap("testRepo", repositorySettings);
    GlossaryTerm glossaryTerm = new GlossaryTerm();
    glossaryTerm.setTmTextUnitId(1L);
    glossaryTerm.setText("Test text");
    Map<String, String> translations = new HashMap<>();
    translations.put("fr-FR", "Test text in French");
    translations.put("de-DE", null);
    translations.put("en-IE", null);
    glossaryTerm.setTranslations(translations);

    GlossaryTerm glossaryTerm2 = new GlossaryTerm();
    glossaryTerm2.setTmTextUnitId(2L);
    glossaryTerm2.setText("More Test text");
    Map<String, String> translations2 = new HashMap<>();
    translations2.put("fr-FR", null);
    translations2.put("de-DE", null);
    translations2.put("en-IE", null);
    glossaryTerm2.setTranslations(translations2);
    // Do Not Translate matches should not be filtered out if translation is null
    glossaryTerm2.setDoNotTranslate(true);
    List<GlossaryTerm> glossaryTerms = List.of(glossaryTerm, glossaryTerm2);
    when(glossaryCacheService.getGlossaryTermsInText(isA(String.class))).thenReturn(glossaryTerms);
    aITranslationConfiguration.setRepositorySettings(repositorySettingsMap);
    aiTranslateCronJob.translate(repository, tmTextUnit, tmTextUnitPendingMT);

    verify(llmService)
        .translate(
            isA(TMTextUnit.class),
            eq("en-GB"),
            eq("fr-FR"),
            isA(AIPrompt.class),
            argThat(
                terms ->
                    terms.size() == 2
                        && terms.getFirst().getLocaleTranslation("fr-FR") != null
                        && terms.get(1).getLocaleTranslation("fr-FR") == null));

    verify(llmService)
        .translate(
            isA(TMTextUnit.class),
            eq("en-GB"),
            eq("de-DE"),
            isA(AIPrompt.class),
            argThat(
                terms ->
                    terms.size() == 1 && terms.getFirst().getLocaleTranslation("de-DE") == null));

    verify(llmService)
        .translate(
            isA(TMTextUnit.class),
            eq("en-GB"),
            eq("en-IE"),
            isA(AIPrompt.class),
            argThat(
                terms ->
                    terms.size() == 1 && terms.getFirst().getLocaleTranslation("en-IE") == null));

    verify(llmService, times(3))
        .translate(
            isA(TMTextUnit.class),
            isA(String.class),
            isA(String.class),
            isA(AIPrompt.class),
            isA(List.class));
    verify(tmService, times(3))
        .addTMTextUnitCurrentVariantWithResult(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitVariantCommentService, times(3))
        .addComment(
            anyLong(),
            any(TMTextUnitVariantComment.Type.class),
            any(TMTextUnitVariantComment.Severity.class),
            eq("Translated via AI translation job."));
  }
}
