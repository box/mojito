package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.service.ai.LLMService;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.google.common.collect.Lists;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Quartz job that translates text units in batches via AI.
 *
 * @author maallen
 */
@Component
@Configuration
@ConditionalOnProperty(value = "l10n.ai.translation.enabled", havingValue = "true")
@DisallowConcurrentExecution
public class AITranslateCronJob implements Job {

  static Logger logger = LoggerFactory.getLogger(AITranslateCronJob.class);

  private static final String REPOSITORY_DEFAULT_PROMPT = "repository_default_prompt";

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired LLMService llmService;

  @Autowired MeterRegistry meterRegistry;

  @Autowired RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Autowired AITranslationTextUnitFilterService aiTranslationTextUnitFilterService;

  @Autowired AITranslationConfiguration aiTranslationConfiguration;

  @Autowired AITranslationService aiTranslationService;

  @Lazy @Autowired TMService tmService;

  @Autowired TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Value("${l10n.ai.translation.job.threads:1}")
  int threads;

  @Timed("AITranslateCronJob.translate")
  public void translate(Repository repository, TMTextUnit tmTextUnit, TmTextUnitPendingMT pendingMT)
      throws AIException {

    try {
      if (pendingMT != null) {
        if (!isExpired(pendingMT)) {
          if (!isUsed(tmTextUnit)) {
            logger.info(
                "Text unit with id: {} is not used, skipping AI translation.", tmTextUnit.getId());
            return;
          }
          if (aiTranslationTextUnitFilterService.isTranslatable(tmTextUnit, repository)) {
            translateLocales(tmTextUnit, repository, getLocalesForMT(repository, tmTextUnit));
            meterRegistry
                .timer("AITranslateCronJob.timeToMT", Tags.of("repository", repository.getName()))
                .record(
                    JSR310Migration.getMillis(JSR310Migration.dateTimeNow())
                        - JSR310Migration.getMillis(pendingMT.getCreatedDate()),
                    TimeUnit.MILLISECONDS);
          } else {
            logger.debug(
                "Text unit with name: {} should not be translated, skipping AI translation.",
                tmTextUnit.getName());
            meterRegistry
                .counter(
                    "AITranslateCronJob.translate.notTranslatable",
                    Tags.of("repository", repository.getName()))
                .increment();
          }
        } else {
          // If the pending MT is expired, log an error and delete it
          logger.error("Pending MT for tmTextUnitId: {} is expired", tmTextUnit.getId());
          meterRegistry
              .counter("AITranslateCronJob.expired", Tags.of("repository", repository.getName()))
              .increment();
        }
      }
    } catch (Exception e) {
      logger.error("Error running job for text unit id {}", tmTextUnit.getId(), e);
      meterRegistry
          .counter("AITranslateCronJob.error", Tags.of("repository", repository.getName()))
          .increment();
    }
  }

  private Set<Locale> getLocalesForMT(Repository repository, TMTextUnit tmTextUnit) {
    Set<Locale> localesWithVariants =
        tmTextUnitVariantRepository.findLocalesWithVariantByTmTextUnit_Id(tmTextUnit.getId());
    return repository.getRepositoryLocales().stream()
        .map(RepositoryLocale::getLocale)
        .filter(
            locale ->
                !localesWithVariants.contains(locale)
                    && !locale.equals(repository.getSourceLocale()))
        .collect(Collectors.toSet());
  }

  private void translateLocales(
      TMTextUnit tmTextUnit, Repository repository, Set<Locale> localesForMT) {

    Map<String, RepositoryLocaleAIPrompt> repositoryLocaleAIPrompts =
        repositoryLocaleAIPromptRepository
            .getActivePromptsByRepositoryAndPromptType(
                repository.getId(), PromptType.TRANSLATION.toString())
            .stream()
            .collect(
                Collectors.toMap(
                    rlap ->
                        rlap.getLocale() != null
                            ? rlap.getLocale().getBcp47Tag()
                            : REPOSITORY_DEFAULT_PROMPT,
                    Function.identity()));
    List<AITranslation> aiTranslations = Lists.newArrayList();
    localesForMT.forEach(
        targetLocale -> {
          try {
            String sourceLang = repository.getSourceLocale().getBcp47Tag().split("-")[0];
            if (aiTranslationConfiguration.getRepositorySettings(repository.getName()) != null
                && aiTranslationConfiguration
                    .getRepositorySettings(repository.getName())
                    .isReuseSourceOnLanguageMatch()
                && targetLocale.getBcp47Tag().startsWith(sourceLang)) {
              aiTranslations.add(
                  reuseSourceStringAsTranslation(tmTextUnit, repository, targetLocale, sourceLang));
              return;
            }
            // Get the prompt override for this locale if it exists, otherwise use the
            // repository default
            RepositoryLocaleAIPrompt repositoryLocaleAIPrompt =
                repositoryLocaleAIPrompts.get(targetLocale.getBcp47Tag()) != null
                    ? repositoryLocaleAIPrompts.get(targetLocale.getBcp47Tag())
                    : repositoryLocaleAIPrompts.get(REPOSITORY_DEFAULT_PROMPT);
            if (repositoryLocaleAIPrompt != null && !repositoryLocaleAIPrompt.isDisabled()) {
              logger.info(
                  "Translating text unit id {} for locale: {} using prompt: {}",
                  tmTextUnit.getId(),
                  targetLocale.getBcp47Tag(),
                  repositoryLocaleAIPrompt.getAiPrompt().getId());
              aiTranslations.add(
                  executeTranslationPrompt(
                      tmTextUnit, repository, targetLocale, repositoryLocaleAIPrompt));
            } else {
              if (repositoryLocaleAIPrompt != null && repositoryLocaleAIPrompt.isDisabled()) {
                logger.debug(
                    "AI translation is disabled for locale "
                        + repositoryLocaleAIPrompt.getLocale().getBcp47Tag()
                        + " in repository "
                        + repository.getName()
                        + ", skipping AI translation.");
              } else {
                logger.debug(
                    "No active translation prompt found for locale: {}, skipping AI translation.",
                    targetLocale.getBcp47Tag());
                meterRegistry
                    .counter(
                        "AITranslateCronJob.translate.noActivePrompt",
                        Tags.of(
                            "repository",
                            repository.getName(),
                            "locale",
                            targetLocale.getBcp47Tag()))
                    .increment();
              }
            }
          } catch (Exception e) {
            logger.error(
                "Error translating text unit id {} for locale: {}",
                tmTextUnit.getId(),
                targetLocale.getBcp47Tag(),
                e);
            meterRegistry
                .counter(
                    "AITranslateCronJob.translate.error",
                    Tags.of(
                        "repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
                .increment();
          }
        });
    for (AITranslation aiTranslation : aiTranslations) {
      tmService.addTMTextUnitCurrentVariantWithResult(
          aiTranslation.getTmTextUnit().getId(),
          aiTranslation.getLocaleId(),
          aiTranslation.getTranslation(),
          aiTranslation.getComment(),
          aiTranslation.getStatus(),
          aiTranslation.isIncludedInLocalizedFile(),
          aiTranslation.getCreatedDate());
    }
  }

  private AITranslation reuseSourceStringAsTranslation(
      TMTextUnit tmTextUnit, Repository repository, Locale targetLocale, String sourceLang) {
    logger.debug(
        "Target language {} matches source language {}, re-using source string as translation.",
        targetLocale.getBcp47Tag(),
        sourceLang);
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.reuseSourceAsTranslation",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment();

    return createAITranslationDTO(tmTextUnit, targetLocale, tmTextUnit.getContent());
  }

  private AITranslation executeTranslationPrompt(
      TMTextUnit tmTextUnit,
      Repository repository,
      Locale targetLocale,
      RepositoryLocaleAIPrompt repositoryLocaleAIPrompt) {
    String translation =
        llmService.translate(
            tmTextUnit,
            repository.getSourceLocale().getBcp47Tag(),
            targetLocale.getBcp47Tag(),
            repositoryLocaleAIPrompt.getAiPrompt());
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.success",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment();
    return createAITranslationDTO(tmTextUnit, targetLocale, translation);
  }

  private AITranslation createAITranslationDTO(
      TMTextUnit tmTextUnit, Locale locale, String translation) {
    AITranslation aiTranslation = new AITranslation();
    aiTranslation.setTmTextUnit(tmTextUnit);
    aiTranslation.setContentMd5(DigestUtils.md5Hex(translation));
    aiTranslation.setLocaleId(locale.getId());
    aiTranslation.setTranslation(translation);
    aiTranslation.setComment(tmTextUnit.getComment());
    aiTranslation.setIncludedInLocalizedFile(false);
    aiTranslation.setStatus(TMTextUnitVariant.Status.MT_TRANSLATED);
    aiTranslation.setCreatedDate(JSR310Migration.dateTimeNow());
    return aiTranslation;
  }

  private boolean isExpired(TmTextUnitPendingMT pendingMT) {
    return pendingMT
        .getCreatedDate()
        .isBefore(
            JSR310Migration.newDateTimeEmptyCtor()
                .minus(aiTranslationConfiguration.getExpiryDuration()));
  }

  private boolean isUsed(TMTextUnit textUnit) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setTmTextUnitIds(List.of(textUnit.getId()));
    params.setUsedFilter(UsedFilter.USED);
    List<TextUnitDTO> result = textUnitSearcher.search(params);
    return !result.isEmpty();
  }

  /**
   * Iterates over all pending MTs and translates them.
   *
   * <p>As each individual {@link TMTextUnit} is translated into all locales, the associated {@link
   * TmTextUnitPendingMT} is deleted.
   *
   * @param jobExecutionContext
   * @throws JobExecutionException
   */
  @Override
  @Timed("AITranslateCronJob.execute")
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Executing AITranslateCronJob");

    ExecutorService executorService = Executors.newFixedThreadPool(threads);

    List<TmTextUnitPendingMT> pendingMTs;
    try {
      do {
        meterRegistry
            .counter("AITranslateCronJob.pendingMT.queueSize")
            .increment(tmTextUnitPendingMTRepository.count());
        pendingMTs =
            tmTextUnitPendingMTRepository.findBatch(aiTranslationConfiguration.getBatchSize());
        logger.info("Processing {} pending MTs", pendingMTs.size());
        Queue<TmTextUnitPendingMT> textUnitsToClearPendingMT = new ConcurrentLinkedQueue<>();

        List<CompletableFuture<Void>> futures =
            pendingMTs.stream()
                .map(
                    pendingMT ->
                        CompletableFuture.runAsync(
                            () -> {
                              try {
                                TMTextUnit tmTextUnit = getTmTextUnit(pendingMT);
                                Repository repository = tmTextUnit.getAsset().getRepository();
                                translate(repository, tmTextUnit, pendingMT);
                              } catch (Exception e) {
                                logger.error(
                                    "Error processing pending MT for text unit id: {}",
                                    pendingMT.getTmTextUnitId(),
                                    e);
                                meterRegistry
                                    .counter("AITranslateCronJob.pendingMT.error")
                                    .increment();
                              } finally {
                                if (pendingMT != null) {
                                  logger.debug(
                                      "Sending pending MT for tmTextUnitId: {} for deletion",
                                      pendingMT.getTmTextUnitId());
                                  textUnitsToClearPendingMT.add(pendingMT);
                                }
                              }
                            },
                            executorService))
                .toList();

        // Wait for all tasks in this batch to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        aiTranslationService.deleteBatch(textUnitsToClearPendingMT);
      } while (!pendingMTs.isEmpty());
    } finally {
      shutdownExecutor(executorService);
    }

    logger.info("Finished executing AITranslateCronJob");
  }

  private static void shutdownExecutor(ExecutorService executorService) {
    try {
      executorService.shutdown();
      if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
        logger.error("Thread pool tasks didn't finish in the expected time.");
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
    }
  }

  private TMTextUnit getTmTextUnit(TmTextUnitPendingMT pendingMT) {
    return tmTextUnitRepository
        .findByIdWithAssetAndRepositoryAndTMFetched(pendingMT.getTmTextUnitId())
        .orElseThrow(
            () -> new AIException("TMTextUnit not found for id: " + pendingMT.getTmTextUnitId()));
  }

  @Bean(name = "aiTranslateCron")
  public JobDetailFactoryBean jobDetailAiTranslateCronJob() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(AITranslateCronJob.class);
    jobDetailFactory.setDescription("Translate text units in batches via AI");
    jobDetailFactory.setDurability(true);
    jobDetailFactory.setName("aiTranslateCron");
    return jobDetailFactory;
  }

  @Bean
  public CronTriggerFactoryBean triggerSlaCheckerCronJob(
      @Qualifier("aiTranslateCron") JobDetail job,
      AITranslationConfiguration aiTranslationConfiguration) {
    CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setCronExpression(aiTranslationConfiguration.getCron());
    return trigger;
  }
}
