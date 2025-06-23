package com.box.l10n.mojito.service.oaitranslate;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionResponseBatchFileLine;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.JsonFormat.JsonSchema.createJsonSchema;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.CreateBatchRequest.forChatCompletion;
import static com.box.l10n.mojito.openai.OpenAIClient.DownloadFileContentRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.DownloadFileContentResponse;
import static com.box.l10n.mojito.openai.OpenAIClient.RetrieveBatchRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.RetrieveBatchResponse;
import static com.box.l10n.mojito.openai.OpenAIClient.TemperatureHelper.getTemperatureForReasoningModels;
import static com.box.l10n.mojito.openai.OpenAIClient.UploadFileRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.UploadFileResponse;
import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.AI_TRANSLATE_WS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsResponse;
import com.box.l10n.mojito.openai.OpenAIClient.CreateBatchResponse;
import com.box.l10n.mojito.openai.OpenAIClient.RequestBatchFileLine;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateBatchesImportJob.AiTranslateBatchesImportInput;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateBatchesImportJob.AiTranslateBatchesImportOutput;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateType.CompletionInput;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateType.CompletionInput.ExistingTarget;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateType.CompletionOutput;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.box.l10n.mojito.service.pollableTask.MsgArg;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.pollableTask.PollableTaskBlobStorage;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryNameNotFoundException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.utils.FilePosition;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Service
public class AiTranslateService {

  static final String METADATA__TEXT_UNIT_DTOS__BLOB_ID = "textUnitDTOs";
  static final Integer MAX_COMPLETION_TOKENS = null;

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiTranslateService.class);

  AssetTextUnitRepository assetTextUnitRepository;

  PollableTaskService pollableTaskService;

  TextUnitSearcher textUnitSearcher;

  RepositoryRepository repositoryRepository;

  RepositoryService repositoryService;

  AiTranslateConfigurationProperties aiTranslateConfigurationProperties;

  OpenAIClient openAIClient;

  OpenAIClientPool openAIClientPool;

  TextUnitBatchImporterService textUnitBatchImporterService;

  StructuredBlobStorage structuredBlobStorage;

  ObjectMapper objectMapper;

  RetryBackoffSpec retryBackoffSpec;

  QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  PollableTaskBlobStorage pollableTaskBlobStorage;

  TextUnitDTOsCacheService textUnitDTOsCacheService;

  public AiTranslateService(
      TextUnitSearcher textUnitSearcher,
      RepositoryRepository repositoryRepository,
      RepositoryService repositoryService,
      TextUnitBatchImporterService textUnitBatchImporterService,
      StructuredBlobStorage structuredBlobStorage,
      AiTranslateConfigurationProperties aiTranslateConfigurationProperties,
      @Qualifier("AiTranslate") @Autowired(required = false) OpenAIClient openAIClient,
      @Qualifier("AiTranslate") @Autowired(required = false) OpenAIClientPool openAIClientPool,
      @Qualifier("AiTranslate") ObjectMapper objectMapper,
      @Qualifier("AiTranslate") RetryBackoffSpec retryBackoffSpec,
      QuartzPollableTaskScheduler quartzPollableTaskScheduler,
      PollableTaskBlobStorage pollableTaskBlobStorage,
      PollableTaskService pollableTaskService,
      TextUnitDTOsCacheService textUnitDTOsCacheService,
      AssetTextUnitRepository assetTextUnitRepository) {
    this.textUnitSearcher = textUnitSearcher;
    this.repositoryRepository = repositoryRepository;
    this.repositoryService = repositoryService;
    this.textUnitBatchImporterService = textUnitBatchImporterService;
    this.structuredBlobStorage = structuredBlobStorage;
    this.aiTranslateConfigurationProperties = aiTranslateConfigurationProperties;
    this.objectMapper = objectMapper;
    this.openAIClient = openAIClient;
    this.openAIClientPool = openAIClientPool;
    this.retryBackoffSpec = retryBackoffSpec;
    this.quartzPollableTaskScheduler = quartzPollableTaskScheduler;
    this.pollableTaskBlobStorage = pollableTaskBlobStorage;
    this.pollableTaskService = pollableTaskService;
    this.textUnitDTOsCacheService = textUnitDTOsCacheService;
    this.assetTextUnitRepository = assetTextUnitRepository;
  }

  public record AiTranslateInput(
      String repositoryName,
      List<String> targetBcp47tags,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      boolean useBatch,
      String useModel,
      String promptSuffix,
      String relatedStringsType,
      String translateType,
      String statusFilter) {}

  public PollableFuture<Void> aiTranslateAsync(AiTranslateInput aiTranslateInput) {

    QuartzJobInfo<AiTranslateInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(AiTranslateJob.class)
            .withInlineInput(false)
            .withInput(aiTranslateInput)
            .withScheduler(aiTranslateConfigurationProperties.getSchedulerName())
            .build();

    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  public PollableFuture<AiTranslateBatchesImportOutput> aiTranslateBatchesImportAsync(
      AiTranslateBatchesImportInput aiTranslateBatchesImportInput, PollableTask currentTask) {

    long backOffSeconds = Math.min(10 * (1L << aiTranslateBatchesImportInput.attempt()), 600);

    QuartzJobInfo<AiTranslateBatchesImportInput, AiTranslateBatchesImportOutput> quartzJobInfo =
        QuartzJobInfo.newBuilder(AiTranslateBatchesImportJob.class)
            .withInlineInput(false)
            .withInput(aiTranslateBatchesImportInput)
            .withScheduler(aiTranslateConfigurationProperties.getSchedulerName())
            .withTimeout(864000) // hardcoded 24h for now
            .withTriggerStartDate(
                JSR310Migration.dateTimeToDate(ZonedDateTime.now().plusSeconds(backOffSeconds)))
            .withParentId(
                currentTask
                    .getId()) // if running 24h, it will make record 144 sub-tasks. Might want to
            // reconsider
            .build();

    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  public void aiTranslate(AiTranslateInput aiTranslateInput, PollableTask currentTask)
      throws AiTranslateException {
    if (aiTranslateInput.useBatch()) {
      aiTranslateBatch(aiTranslateInput, currentTask);
    } else {
      aiTranslateNoBatch(aiTranslateInput);
    }
  }

  public void aiTranslateNoBatch(AiTranslateInput aiTranslateInput) {

    Repository repository = getRepository(aiTranslateInput);

    logger.info("Start AI Translation (no batch) for repository: {}", repository.getName());

    Set<RepositoryLocale> filteredRepositoryLocales =
        getFilteredRepositoryLocales(aiTranslateInput, repository);

    Flux.fromIterable(filteredRepositoryLocales)
        .flatMap(
            rl ->
                asyncProcessLocale(
                    rl,
                    aiTranslateInput.sourceTextMaxCountPerLocale(),
                    getModel(aiTranslateInput),
                    getPrompt(
                        AiTranslateType.fromString(aiTranslateInput.translateType()).getPrompt(),
                        aiTranslateInput.promptSuffix()),
                    aiTranslateInput.tmTextUnitIds(),
                    openAIClientPool,
                    RelatedStringsProvider.Type.fromString(aiTranslateInput.relatedStringsType()),
                    StatusFilter.valueOf(aiTranslateInput.statusFilter())),
            10)
        .then()
        .doOnTerminate(
            () ->
                logger.info(
                    "Done with AI Translation (no batch) for repository: {}", repository.getName()))
        .block();
  }

  Mono<Void> asyncProcessLocale(
      RepositoryLocale repositoryLocale,
      int sourceTextMaxCountPerLocale,
      String model,
      String prompt,
      List<Long> tmTextUnitIds,
      OpenAIClientPool openAIClientPool,
      RelatedStringsProvider.Type relatedStringsProviderType,
      StatusFilter statusFilter) {

    Repository repository = repositoryLocale.getRepository();
    List<TextUnitDTO> textUnitDTOS =
        getTextUnitDTOS(
            repository, sourceTextMaxCountPerLocale, tmTextUnitIds, repositoryLocale, statusFilter);

    if (textUnitDTOS.isEmpty()) {
      logger.debug(
          "Nothing to translate for locale: {}", repositoryLocale.getLocale().getBcp47Tag());
      return Mono.empty();
    }

    logger.info(
        "Starting parallel processing for each string in locale: {}, count: {}",
        repositoryLocale.getLocale().getBcp47Tag(),
        textUnitDTOS.size());

    return Flux.fromIterable(textUnitDTOS)
        .buffer(500)
        .concatMap(
            batch ->
                Flux.fromIterable(batch)
                    .flatMap(
                        textUnitDTO ->
                            getChatCompletionForTextUnitDTO(
                                    textUnitDTO,
                                    model,
                                    prompt,
                                    openAIClientPool,
                                    new RelatedStringsProvider(relatedStringsProviderType))
                                .retryWhen(
                                    Retry.backoff(5, Duration.ofSeconds(1))
                                        .filter(this::isRetryableException)
                                        .doBeforeRetry(
                                            retrySignal -> {
                                              logger.warn(
                                                  "Retrying request for TextUnitDTO {} due to exception of type {}",
                                                  textUnitDTO.getTmTextUnitId(),
                                                  retrySignal.failure().getMessage());
                                            }))
                                .onErrorResume(
                                    error -> {
                                      logger.error(
                                          "Request for TextUnitDTO {} failed after retries: {}",
                                          textUnitDTO.getTmTextUnitId(),
                                          error.getMessage());
                                      return Mono.empty();
                                    }))
                    .collectList()
                    .flatMap(this::submitForImport)
                    .doOnTerminate(() -> logger.info("Done submitting for processing")))
        .then();
  }

  record MyRecord(TextUnitDTO textUnitDTO, ChatCompletionsResponse chatCompletionsResponse) {}

  private Mono<Void> submitForImport(List<MyRecord> results) {
    logger.info("Submit for import for locale {}", results.get(0).textUnitDTO().getTargetLocale());
    List<TextUnitDTO> forImport =
        results.stream()
            .map(
                myRecord -> {
                  TextUnitDTO textUnitDTO = myRecord.textUnitDTO();
                  ChatCompletionsResponse chatCompletionsResponse =
                      myRecord.chatCompletionsResponse();

                  String completionOutputAsJson =
                      chatCompletionsResponse.choices().getFirst().message().content();

                  AiTranslateType aiTranslateType = AiTranslateType.WITH_REVIEW;

                  Object completionOutput =
                      objectMapper.readValueUnchecked(
                          completionOutputAsJson, aiTranslateType.getOutputJsonSchemaClass());

                  aiTranslateType.getTextUnitDTOUpdate().accept(textUnitDTO, completionOutput);

                  textUnitDTO.setTargetComment("ai-translate");
                  return textUnitDTO;
                })
            .collect(Collectors.toList());

    textUnitBatchImporterService.importTextUnits(
        forImport, TextUnitBatchImporterService.IntegrityChecksType.KEEP_STATUS_IF_SAME_TARGET);

    return Mono.empty();
  }

  private Mono<MyRecord> getChatCompletionForTextUnitDTO(
      TextUnitDTO textUnitDTO,
      String model,
      String prompt,
      OpenAIClientPool openAIClientPool,
      RelatedStringsProvider relatedStringsProvider) {

    CompletionInput completionInput =
        new CompletionInput(
            textUnitDTO.getTargetLocale(),
            textUnitDTO.getSource(),
            textUnitDTO.getComment(),
            textUnitDTO.getTarget() == null
                ? null
                : new ExistingTarget(
                    textUnitDTO.getTarget(), !textUnitDTO.isIncludedInLocalizedFile()),
            relatedStringsProvider.getRelatedStrings(textUnitDTO));

    ChatCompletionsRequest chatCompletionsRequest =
        getChatCompletionsRequest(model, prompt, completionInput);

    CompletableFuture<ChatCompletionsResponse> futureResult =
        openAIClientPool.submit(
            (openAIClient) -> openAIClient.getChatCompletions(chatCompletionsRequest));
    return Mono.fromFuture(futureResult)
        .map(chatCompletionsResponse -> new MyRecord(textUnitDTO, chatCompletionsResponse));
  }

  private ChatCompletionsRequest getChatCompletionsRequest(
      String model, String prompt, CompletionInput completionInput) {
    String inputAsJsonString = objectMapper.writeValueAsStringUnchecked(completionInput);
    ObjectNode jsonSchema = createJsonSchema(CompletionOutput.class);

    ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequest()
            .model(model)
            .maxCompletionTokens(MAX_COMPLETION_TOKENS)
            .temperature(getTemperatureForReasoningModels(model))
            .messages(
                List.of(
                    systemMessageBuilder().content(prompt).build(),
                    userMessageBuilder().content(inputAsJsonString).build()))
            .responseFormat(
                new ChatCompletionsRequest.JsonFormat(
                    "json_schema",
                    new ChatCompletionsRequest.JsonFormat.JsonSchema(
                        true, "request_json_format", jsonSchema)))
            .build();
    return chatCompletionsRequest;
  }

  private boolean isRetryableException(Throwable throwable) {
    Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
    return cause instanceof IOException || cause instanceof TimeoutException;
  }

  public void aiTranslateBatch(AiTranslateInput aiTranslateInput, PollableTask currentTask)
      throws AiTranslateException {

    Repository repository = getRepository(aiTranslateInput);

    logger.debug("Start AI Translation for repository: {}", repository.getName());

    try {

      Set<RepositoryLocale> repositoryLocalesWithoutRootLocale =
          getFilteredRepositoryLocales(aiTranslateInput, repository);

      logger.debug("Create batches for repository: {}", repository.getName());

      List<OpenAIClient.CreateBatchResponse> createdBatches = new ArrayList<>();
      List<String> batchCreationErrors = new ArrayList<>();
      List<String> skippedLocales = new ArrayList<>();

      for (RepositoryLocale repositoryLocale : repositoryLocalesWithoutRootLocale) {
        try {
          OpenAIClient.CreateBatchResponse createBatchResponse =
              createBatchForRepositoryLocale(
                  repositoryLocale,
                  repository,
                  aiTranslateInput.sourceTextMaxCountPerLocale(),
                  getModel(aiTranslateInput),
                  aiTranslateInput.tmTextUnitIds(),
                  aiTranslateInput.promptSuffix(),
                  RelatedStringsProvider.Type.fromString(aiTranslateInput.relatedStringsType()),
                  StatusFilter.valueOf(aiTranslateInput.statusFilter()));

          if (createBatchResponse != null) {
            createdBatches.add(createBatchResponse);
          } else {
            skippedLocales.add(repositoryLocale.getLocale().getBcp47Tag());
          }
        } catch (Throwable t) {
          String errorMessage =
              "Can't create batch for locale: %s. Error: %s"
                  .formatted(repositoryLocale.getLocale().getBcp47Tag(), t.getMessage());
          logger.error(errorMessage, t);
          batchCreationErrors.add(errorMessage);
        }
      }

      logger.debug("Start a job to import batches for repository: {}", repository.getName());
      PollableFuture<AiTranslateBatchesImportOutput> aiTranslateBatchesImportOutputPollableFuture =
          aiTranslateBatchesImportAsync(
              new AiTranslateBatchesImportInput(
                  createdBatches, skippedLocales, batchCreationErrors, List.of(), Map.of(), 0),
              currentTask);

      logger.info(
          "Schedule AiTranslateBatchesImportJob, id: {}",
          aiTranslateBatchesImportOutputPollableFuture.getPollableTask().getId());

    } catch (OpenAIClient.OpenAIClientResponseException openAIClientResponseException) {
      logger.error(
          "Failed to ai translate: %s".formatted(openAIClientResponseException),
          openAIClientResponseException);
      throw new AiTranslateException(openAIClientResponseException);
    }
  }

  private Set<RepositoryLocale> getFilteredRepositoryLocales(
      AiTranslateInput aiTranslateInput, Repository repository) {
    return repositoryService.getRepositoryLocalesWithoutRootLocale(repository).stream()
        .filter(
            rl ->
                aiTranslateInput.targetBcp47tags == null
                    || aiTranslateInput.targetBcp47tags.contains(rl.getLocale().getBcp47Tag()))
        .collect(Collectors.toSet());
  }

  void importBatch(RetrieveBatchResponse retrieveBatchResponse) {

    logger.info("Importing batch: {}", retrieveBatchResponse.id());

    String textUnitDTOsBlobId =
        retrieveBatchResponse.metadata().get(METADATA__TEXT_UNIT_DTOS__BLOB_ID);

    logger.info("Trying to load textUnitDTOs from blob: {}", textUnitDTOsBlobId);
    AiTranslateBlobStorage aiTranslateBlobStorage =
        structuredBlobStorage
            .getString(AI_TRANSLATE_WS, textUnitDTOsBlobId)
            .map(s -> objectMapper.readValueUnchecked(s, AiTranslateBlobStorage.class))
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "There must be an entry for textUnitDTOsBlobId: " + textUnitDTOsBlobId));

    Map<Long, TextUnitDTO> tmTextUnitIdToTextUnitDTOs =
        aiTranslateBlobStorage.textUnitDTOS().stream()
            .collect(toMap(TextUnitDTO::getTmTextUnitId, Function.identity()));

    DownloadFileContentResponse downloadFileContentResponse =
        getOpenAIClient()
            .downloadFileContent(
                new DownloadFileContentRequest(retrieveBatchResponse.outputFileId()));

    List<TextUnitDTO> forImport =
        downloadFileContentResponse
            .content()
            .lines()
            .map(
                line -> {
                  ChatCompletionResponseBatchFileLine chatCompletionResponseBatchFileLine =
                      objectMapper.readValueUnchecked(
                          line, ChatCompletionResponseBatchFileLine.class);

                  if (chatCompletionResponseBatchFileLine.response().statusCode() != 200) {
                    throw new RuntimeException(
                        "Response batch file line failed: " + chatCompletionResponseBatchFileLine);
                  }

                  String completionOutputAsJson =
                      chatCompletionResponseBatchFileLine
                          .response()
                          .chatCompletionsResponse()
                          .choices()
                          .getFirst()
                          .message()
                          .content();

                  AiTranslateType aiTranslateType = AiTranslateType.WITH_REVIEW;

                  Object completionOutput =
                      objectMapper.readValueUnchecked(
                          completionOutputAsJson, aiTranslateType.getOutputJsonSchemaClass());

                  TextUnitDTO textUnitDTO =
                      tmTextUnitIdToTextUnitDTOs.get(
                          Long.valueOf(chatCompletionResponseBatchFileLine.customId()));

                  aiTranslateType.getTextUnitDTOUpdate().accept(textUnitDTO, completionOutput);

                  textUnitDTO.setTargetComment("ai-translate");
                  textUnitDTO.setStatus(TMTextUnitVariant.Status.REVIEW_NEEDED);
                  return textUnitDTO;
                })
            .toList();

    textUnitBatchImporterService.importTextUnits(
        forImport, TextUnitBatchImporterService.IntegrityChecksType.KEEP_STATUS_IF_SAME_TARGET);
  }

  record RelatedString(String source, String description) {}

  @Pollable(message = "AiTranslateService Retry import for job id: {id}")
  public PollableFuture<Void> retryImport(
      @MsgArg(name = "id") long childPollableTaskId, @InjectCurrentTask PollableTask currentTask) {
    PollableTask childPollableTask = pollableTaskService.getPollableTask(childPollableTaskId);

    AiTranslateBatchesImportInput aiTranslateBatchesImportInput =
        pollableTaskBlobStorage.getInput(childPollableTaskId, AiTranslateBatchesImportInput.class);

    AiTranslateBatchesImportInput aiReviewBatchesImportInputAttempt0 =
        new AiTranslateBatchesImportInput(
            aiTranslateBatchesImportInput.createBatchResponses(),
            aiTranslateBatchesImportInput.skippedLocales(),
            aiTranslateBatchesImportInput.batchCreationErrors(),
            aiTranslateBatchesImportInput.processed(),
            aiTranslateBatchesImportInput.failedImport(),
            0);

    PollableFuture<AiTranslateBatchesImportOutput> aiTranslateBatchesImportOutputPollableFuture =
        aiTranslateBatchesImportAsync(aiReviewBatchesImportInputAttempt0, currentTask);
    logger.info(
        "[task id: {}] Retrying to import from child id: {}, new job created with pollable task id: {}",
        childPollableTask.getParentTask().getId(),
        childPollableTask.getId(),
        aiTranslateBatchesImportOutputPollableFuture.getPollableTask().getId());

    return new PollableFutureTaskResult<>();
  }

  CreateBatchResponse createBatchForRepositoryLocale(
      RepositoryLocale repositoryLocale,
      Repository repository,
      int sourceTextMaxCountPerLocale,
      String model,
      List<Long> tmTextUnitIds,
      String promptSuffix,
      RelatedStringsProvider.Type relatedStringsProviderType,
      StatusFilter statusFilter) {

    List<TextUnitDTO> textUnitDTOS =
        getTextUnitDTOS(
            repository, sourceTextMaxCountPerLocale, tmTextUnitIds, repositoryLocale, statusFilter);

    CreateBatchResponse createBatchResponse = null;
    if (textUnitDTOS.isEmpty()) {
      logger.debug("Nothing to translate, don't create a batch");
    } else {
      logger.debug("Save the TextUnitDTOs in blob storage for later batch import");
      String batchId =
          "%s_%s".formatted(repositoryLocale.getLocale().getBcp47Tag(), UUID.randomUUID());
      structuredBlobStorage.put(
          AI_TRANSLATE_WS,
          batchId,
          objectMapper.writeValueAsStringUnchecked(new AiTranslateBlobStorage(textUnitDTOS)),
          Retention.MIN_1_DAY);

      logger.debug("Generate the batch file content");
      String batchFileContent =
          generateBatchFileContent(textUnitDTOS, model, promptSuffix, relatedStringsProviderType);

      UploadFileResponse uploadFileResponse =
          getOpenAIClient()
              .uploadFile(
                  UploadFileRequest.forBatch("%s.jsonl".formatted(batchId), batchFileContent));

      logger.debug("Create the batch using file: {}", uploadFileResponse);
      createBatchResponse =
          getOpenAIClient()
              .createBatch(
                  forChatCompletion(
                      uploadFileResponse.id(), Map.of(METADATA__TEXT_UNIT_DTOS__BLOB_ID, batchId)));
    }

    logger.info(
        "Created batch for locale: {} with {} text units",
        repositoryLocale.getLocale().getBcp47Tag(),
        textUnitDTOS.size());

    return createBatchResponse;
  }

  private List<TextUnitDTO> getTextUnitDTOS(
      Repository repository,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      RepositoryLocale repositoryLocale,
      StatusFilter statusFilter) {
    logger.debug(
        "Get untranslated strings for locale: '{}' in repository: '{}'",
        repositoryLocale.getLocale().getBcp47Tag(),
        repository.getName());

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repository.getId());
    textUnitSearcherParameters.setStatusFilter(statusFilter);
    textUnitSearcherParameters.setLocaleId(repositoryLocale.getLocale().getId());
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);

    if (tmTextUnitIds != null) {
      logger.debug(
          "Using tmTextUnitIds: {} for ai translate repository: {}",
          tmTextUnitIds,
          repository.getName());
      textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitIds);
    } else {
      textUnitSearcherParameters.setLimit(sourceTextMaxCountPerLocale);
    }

    List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(textUnitSearcherParameters);
    return textUnitDTOS;
  }

  String generateBatchFileContent(
      List<TextUnitDTO> textUnitDTOS,
      String model,
      String promptSuffix,
      RelatedStringsProvider.Type relatedStringsProviderType) {

    RelatedStringsProvider relatedStringsProvider =
        new RelatedStringsProvider(relatedStringsProviderType);

    return textUnitDTOS.stream()
        .map(
            textUnitDTO -> {
              CompletionInput completionInput =
                  new CompletionInput(
                      textUnitDTO.getTargetLocale(),
                      textUnitDTO.getSource(),
                      textUnitDTO.getComment(),
                      new ExistingTarget(
                          textUnitDTO.getTarget(), !textUnitDTO.isIncludedInLocalizedFile()),
                      relatedStringsProvider.getRelatedStrings(textUnitDTO));

              ChatCompletionsRequest chatCompletionsRequest =
                  getChatCompletionsRequest(model, promptSuffix, completionInput);

              return RequestBatchFileLine.forChatCompletion(
                  textUnitDTO.getTmTextUnitId().toString(), chatCompletionsRequest);
            })
        .map(objectMapper::writeValueAsStringUnchecked)
        .collect(joining("\n"));
  }

  class RelatedStringsProvider {

    static int CHARACTER_LIMIT = 10000;

    Map<Long, List<AssetTextUnit>> cache = new HashMap<>();

    enum Type {
      USAGES,
      ID_PREFIX,
      NONE;

      public static Type fromString(String type) {
        Type result = NONE;
        if (type != null) {
          result = valueOf(type.toUpperCase());
        }
        return result;
      }
    }

    Type type;

    public RelatedStringsProvider(Type type) {
      this.type = type;
    }

    /**
     * Everything must be lazy in case we don't use the option. It is very heavy on memory usage but
     * relatively fast since all asset text units are load per asset, in most case we have one big
     * asset per repository.
     */
    public List<RelatedString> getRelatedStrings(TextUnitDTO textUnitDTO) {

      List<AssetTextUnit> assetTextUnits =
          getAssetTextUnitsOfAssetExtraction(textUnitDTO.getAssetExtractionId());

      Optional<AssetTextUnit> assetTextUnit =
          assetTextUnits.stream()
              .filter(atu -> atu.getId().equals(textUnitDTO.getAssetTextUnitId()))
              .findFirst();

      if (assetTextUnit.isPresent()) {
        List<RelatedString> relatedStrings =
            switch (type) {
              case USAGES -> getRelatedStringsByUsages(assetTextUnits, assetTextUnit.get());
              case ID_PREFIX -> getRelatedStringsByIdPrefix(assetTextUnits, assetTextUnit.get());
              case NONE -> ImmutableList.of();
            };
        List<RelatedString> filteredByCharLimit =
            filterByCharLimit(relatedStrings, CHARACTER_LIMIT);
        logger.debug(
            "Related strings (type: {}, count: {}, filtered: {}): {}",
            type,
            relatedStrings.size(),
            filteredByCharLimit.size(),
            relatedStrings);
        return filteredByCharLimit;
      } else {
        logger.warn(
            "The text unit dto does not have a matching asset text unit in the current asset extraction. This"
                + "is probably due to concurrent updates, return no related strings.");
        return ImmutableList.of();
      }
    }

    List<RelatedString> getRelatedStringsByIdPrefix(
        List<AssetTextUnit> assetTextUnits, AssetTextUnit assetTextUnit) {

      String prefix = getPrefix(assetTextUnit.getName());
      return assetTextUnits.stream()
          .filter(atu -> !atu.getId().equals(assetTextUnit.getId()))
          .filter(atu -> atu.getName().startsWith(prefix))
          .map(atu -> new RelatedString(atu.getContent(), atu.getComment()))
          .toList();
    }

    static String getPrefix(String id) {
      int dot = id.indexOf('.');
      return (dot == -1) ? id : id.substring(0, dot);
    }

    record RelatedStringWithPosition(RelatedString relatedString, Long position) {}

    static List<RelatedString> getRelatedStringsByUsages(
        List<AssetTextUnit> assetTextUnits, AssetTextUnit assetTextUnit) {
      Map<String, List<RelatedStringWithPosition>> byUsages =
          assetTextUnits.stream()
              .filter(atu -> !atu.getId().equals(assetTextUnit.getId()))
              .flatMap(
                  atu ->
                      atu.getUsages().stream()
                          .map(
                              usage -> {
                                FilePosition filePosition = FilePosition.from(usage);
                                return Map.entry(
                                    filePosition.path(),
                                    new RelatedStringWithPosition(
                                        new RelatedString(atu.getContent(), atu.getComment()),
                                        // in the same file order by line if available, else by
                                        // atu.id
                                        // to have at least creation order (not good for later
                                        // updates which put
                                        // newer strings at the end regardless the position in the
                                        // document)
                                        filePosition.line() == null
                                            ? atu.getId()
                                            : filePosition.line()));
                              }))
              .collect(
                  Collectors.groupingBy(
                      Map.Entry::getKey,
                      Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

      List<RelatedString> relatedStrings =
          assetTextUnit.getUsages().stream()
              .flatMap(
                  u -> {
                    FilePosition filePosition = FilePosition.from(u);
                    return byUsages.getOrDefault(filePosition.path(), List.of()).stream()
                        .sorted(Comparator.comparingLong(RelatedStringWithPosition::position))
                        .map(RelatedStringWithPosition::relatedString);
                  })
              .toList();

      return relatedStrings;
    }

    List<AssetTextUnit> getAssetTextUnitsOfAssetExtraction(Long assetExtractionId) {
      return cache.computeIfAbsent(
          assetExtractionId, assetTextUnitRepository::findByAssetExtractionId);
    }

    public static List<RelatedString> filterByCharLimit(
        List<RelatedString> relatedStrings, int charLimit) {

      final int JSON_OVERHEAD = 30;
      int i = 0;
      int totalCharCount = 0;

      for (RelatedString rs : relatedStrings) {

        int charCount =
            (rs.source() == null ? 0 : rs.source().length())
                + (rs.description() == null ? 0 : rs.description().length())
                + JSON_OVERHEAD;

        if (totalCharCount + charCount > charLimit) break;

        totalCharCount += charCount;
        i++;
      }

      return List.copyOf(relatedStrings.subList(0, i));
    }
  }

  // TODO(ja) duplicated
  RetrieveBatchResponse retrieveBatchWithRetry(CreateBatchResponse batch) {

    return Mono.fromCallable(
            () -> getOpenAIClient().retrieveBatch(new RetrieveBatchRequest(batch.id())))
        .retryWhen(
            retryBackoffSpec.doBeforeRetry(
                doBeforeRetry -> {
                  logger.info("Retrying retrieving batch: {}", batch.id());
                }))
        .doOnError(
            throwable -> new RuntimeException("Failed to retrieve batch: " + batch.id(), throwable))
        .block();
  }

  record AiTranslateBlobStorage(List<TextUnitDTO> textUnitDTOS) {}

  static String getPrompt(String prompt, String promptSuffix) {
    return promptSuffix == null ? prompt : "%s %s".formatted(prompt, promptSuffix);
  }

  private Repository getRepository(AiTranslateInput aiTranslateInput) {
    Repository repository = repositoryRepository.findByName(aiTranslateInput.repositoryName());

    if (repository == null) {
      throw new RepositoryNameNotFoundException(
          String.format(
              "Repository with name '%s' can not be found!", aiTranslateInput.repositoryName()));
    }
    return repository;
  }

  private String getModel(AiTranslateInput aiTranslateInput) {
    return aiTranslateInput.useModel() != null
        ? aiTranslateInput.useModel()
        : aiTranslateConfigurationProperties.getModelName();
  }

  /**
   * Typical configuration for the ObjectMapper needed by this class.
   *
   * <p>The ObjectMapper must not use indentation else Jsonl serialization will fail.
   */
  public static void configureObjectMapper(ObjectMapper objectMapper) {
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
    objectMapper.registerModule(new JavaTimeModule());
  }

  OpenAIClient getOpenAIClient() {
    if (openAIClient == null) {
      String msg =
          "OpenAI client is not configured for AiTranslateService. Ensure that the OpenAI API key is provided in the configuration (qualifier='aiTranslate').";
      logger.error(msg);
      throw new RuntimeException(msg);
    }
    return openAIClient;
  }

  public static class AiTranslateException extends Exception {
    public AiTranslateException(Throwable cause) {
      super(cause);
    }
  }
}
