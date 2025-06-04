package com.box.l10n.mojito.service.oaireview;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.JsonFormat.JsonSchema.createJsonSchema;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.CreateBatchRequest.forChatCompletion;
import static com.box.l10n.mojito.openai.OpenAIClient.TemperatureHelper.getTemperatureForReasoningModels;
import static java.util.stream.Collectors.joining;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AiReviewProto;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsResponse;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.oaireview.AiReviewBatchesImportJob.AiReviewBatchesImportInput;
import com.box.l10n.mojito.service.oaireview.AiReviewBatchesImportJob.AiReviewBatchesImportOutput;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryNameNotFoundException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.AiReviewProtoRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Service
public class AiReviewService {

  static final String METADATA__TEXT_UNIT_DTOS__BLOB_ID = "textUnitDTOs";

  static final int MAX_COMPLETION_TOKENS = 16384;

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiReviewService.class);

  TextUnitSearcher textUnitSearcher;

  RepositoryRepository repositoryRepository;

  TMTextUnitVariantRepository textUnitVariantRepository;

  AiReviewProtoRepository aiReviewProtoRepository;

  RepositoryService repositoryService;

  AiReviewConfigurationProperties aiReviewConfigurationProperties;

  OpenAIClient openAIClient;

  OpenAIClientPool openAIClientPool;

  ObjectMapper objectMapper;

  RetryBackoffSpec retryBackoffSpec;

  QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  /**
   * openAIClient and openAIClientPool are nullable. The public API will check for the client if
   * they are not configured will throw an exception (keeping code minimal for now, could split into
   * interface + 2 implementations: 1 for the non-configured case, and 1 for the configured)
   */
  public AiReviewService(
      TextUnitSearcher textUnitSearcher,
      RepositoryRepository repositoryRepository,
      TMTextUnitVariantRepository textUnitVariantRepository,
      AiReviewProtoRepository aiReviewProtoRepository,
      RepositoryService repositoryService,
      AiReviewConfigurationProperties aiReviewConfigurationProperties,
      @Autowired(required = false) @Qualifier("openAIClientReview") OpenAIClient openAIClient,
      @Autowired(required = false) @Qualifier("openAIClientPoolReview")
          OpenAIClientPool openAIClientPool,
      @Qualifier("objectMapperReview") ObjectMapper objectMapper,
      @Qualifier("retryBackoffSpecReview") RetryBackoffSpec retryBackoffSpec,
      QuartzPollableTaskScheduler quartzPollableTaskScheduler) {
    this.textUnitSearcher = Objects.requireNonNull(textUnitSearcher);
    this.repositoryRepository = Objects.requireNonNull(repositoryRepository);
    this.textUnitVariantRepository = Objects.requireNonNull(textUnitVariantRepository);
    this.aiReviewProtoRepository = Objects.requireNonNull(aiReviewProtoRepository);
    this.repositoryService = Objects.requireNonNull(repositoryService);
    this.aiReviewConfigurationProperties = Objects.requireNonNull(aiReviewConfigurationProperties);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.openAIClient = openAIClient; // nullable
    this.openAIClientPool = openAIClientPool; // nullable
    this.retryBackoffSpec = Objects.requireNonNull(retryBackoffSpec);
    this.quartzPollableTaskScheduler = Objects.requireNonNull(quartzPollableTaskScheduler);
  }

  public record AiReviewInput(
      String repositoryName,
      List<String> targetBcp47tags,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      boolean useBatch,
      String useModel,
      String runName) {}

  public record AiReviewSingleTextUnitOutput(
      String source,
      Target target,
      DescriptionRating descriptionRating,
      AltTarget altTarget,
      ExistingTargetRating existingTargetRating,
      ReviewRequired reviewRequired) {
    record Target(String content, String explanation, int confidenceLevel) {}

    record AltTarget(String content, String explanation, int confidenceLevel) {}

    record DescriptionRating(String explanation, int score) {}

    record ExistingTargetRating(String explanation, int score) {}

    record ReviewRequired(boolean required, String reason) {}
  }

  public record AiReviewSingleTextUnitInput(
      String locale, String source, String sourceDescription, ExistingTarget existingTarget) {
    public record ExistingTarget(String content, boolean hasBrokenPlaceholders) {}
  }

  public AiReviewSingleTextUnitOutput getAiReviewSingleTextUnit(AiReviewSingleTextUnitInput input) {

    ObjectMapper objectMapper = ObjectMapper.withIndentedOutput();
    String inputAsJsonString = objectMapper.writeValueAsStringUnchecked(input);

    ObjectNode jsonSchema = createJsonSchema(AiReviewSingleTextUnitOutput.class);

    OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequest()
            .model(aiReviewConfigurationProperties.getModelName())
            .temperature(
                getTemperatureForReasoningModels(aiReviewConfigurationProperties.getModelName()))
            .maxCompletionTokens(MAX_COMPLETION_TOKENS)
            .messages(
                List.of(
                    systemMessageBuilder().content(AiReviewService.PROMPT).build(),
                    userMessageBuilder().content(inputAsJsonString).build()))
            .responseFormat(
                new OpenAIClient.ChatCompletionsRequest.JsonFormat(
                    "json_schema",
                    new OpenAIClient.ChatCompletionsRequest.JsonFormat.JsonSchema(
                        true, "request_json_format", jsonSchema)))
            .build();

    logger.info(objectMapper.writeValueAsStringUnchecked(chatCompletionsRequest));

    OpenAIClient openAIClient =
        OpenAIClient.builder()
            .apiKey(aiReviewConfigurationProperties.getOpenaiClientToken())
            .build();

    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        openAIClient.getChatCompletions(chatCompletionsRequest).join();

    logger.info(objectMapper.writeValueAsStringUnchecked(chatCompletionsResponse));

    String jsonResponse = chatCompletionsResponse.choices().getFirst().message().content();
    AiReviewSingleTextUnitOutput aiReviewSingleTextUnitOutput =
        objectMapper.readValueUnchecked(jsonResponse, AiReviewSingleTextUnitOutput.class);
    return aiReviewSingleTextUnitOutput;
  }

  public PollableFuture<Void> aiReviewAsync(AiReviewInput aiReviewInput) {

    QuartzJobInfo<AiReviewInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(AiReviewJob.class)
            .withInlineInput(false)
            .withInput(aiReviewInput)
            .withScheduler(aiReviewConfigurationProperties.getSchedulerName())
            .build();

    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  /**
   * Appends 1 subtask to the main pollable task (which the CLI polls) for each scheduling. Since
   * the job reschedule itself until completion for 24h the number of tasks will grow. By checking
   * every 10 minutes, it is limited to 144 entries right now, but we might want to add logic to
   * limit the number of subtask and, check more often.
   */
  public PollableFuture<AiReviewBatchesImportOutput> aiReviewBatchesImportAsync(
      AiReviewBatchesImportInput aiReviewBatchesImportInput, PollableTask currentTask) {

    long backOffSeconds = Math.min(10 * (1L << aiReviewBatchesImportInput.attempt()), 600);

    QuartzJobInfo<AiReviewBatchesImportInput, AiReviewBatchesImportOutput> quartzJobInfo =
        QuartzJobInfo.newBuilder(AiReviewBatchesImportJob.class)
            .withInlineInput(false)
            .withInput(aiReviewBatchesImportInput)
            .withScheduler(aiReviewConfigurationProperties.getSchedulerName())
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

  public void aiReview(AiReviewInput aiReviewInput, PollableTask currentTask)
      throws AiReviewException {
    if (aiReviewInput.useBatch()) {
      aiReviewBatch(aiReviewInput, currentTask);
    } else {
      aiReviewNoBatch(aiReviewInput);
    }
  }

  public void aiReviewNoBatch(AiReviewInput aiReviewInput) {

    Repository repository = getRepository(aiReviewInput);

    logger.info("Start AI Review (no batch) for repository: {}", repository.getName());

    Set<RepositoryLocale> filteredRepositoryLocales =
        getFilteredRepositoryLocales(aiReviewInput, repository);

    Flux.fromIterable(filteredRepositoryLocales)
        .flatMap(
            rl ->
                asyncReviewNoBatchLocale(
                    rl,
                    aiReviewInput.sourceTextMaxCountPerLocale(),
                    aiReviewInput.tmTextUnitIds(),
                    getModel(aiReviewInput.useModel()),
                    aiReviewInput.runName(),
                    openAIClientPool),
            10)
        .then()
        .doOnTerminate(
            () ->
                logger.info(
                    "Done with AI Review (no batch) for repository: {}", repository.getName()))
        .block();
  }

  private String getModel(String useModel) {
    return useModel != null ? useModel : aiReviewConfigurationProperties.getModelName();
  }

  Mono<Void> asyncReviewNoBatchLocale(
      RepositoryLocale repositoryLocale,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      String model,
      String runName,
      OpenAIClientPool openAIClientPool) {

    List<TextUnitDTO> textUnitDTOS =
        getTextUnitDTOSForReview(
            repositoryLocale, sourceTextMaxCountPerLocale, tmTextUnitIds, runName);

    if (textUnitDTOS.isEmpty()) {
      logger.debug("Nothing to review for locale: {}", repositoryLocale.getLocale().getBcp47Tag());
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
                            getChatCompletionForTextUnitDTO(textUnitDTO, model, openAIClientPool)
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
                    .flatMap(results -> submitForSave(runName, results))
                    .doOnTerminate(
                        () ->
                            logger.info(
                                "Done submitting for saving: {}",
                                repositoryLocale.getLocale().getBcp47Tag())))
        .then();
  }

  private List<TextUnitDTO> getTextUnitDTOSForReview(
      RepositoryLocale repositoryLocale,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      String runName) {

    Repository repository = repositoryLocale.getRepository();

    logger.info("Get already reviewed tm text unit variants");
    Set<Long> alreadyReviewedTmTextUnitVariantIds =
        aiReviewProtoRepository.findTmTextUnitVariantIdsByLocaleIdAndRepositoryId(
            repositoryLocale.getLocale().getId(),
            repositoryLocale.getRepository().getId(),
            runName);

    logger.info(
        "Get translated strings for locale: '{}' in repository: '{}'",
        repositoryLocale.getLocale().getBcp47Tag(),
        repository.getName());

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repository.getId());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
    textUnitSearcherParameters.setLocaleId(repositoryLocale.getLocale().getId());
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
    if (tmTextUnitIds != null) {
      logger.debug(
          "Using tmTextUnitIds: {} for ai review repository: {}",
          tmTextUnitIds,
          repository.getName());
      textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitIds);
    } else {
      textUnitSearcherParameters.setLimit(sourceTextMaxCountPerLocale);
    }

    List<TextUnitDTO> allTextUnitDTOS = textUnitSearcher.search(textUnitSearcherParameters);
    List<TextUnitDTO> textUnitDTOS =
        allTextUnitDTOS.stream()
            .filter(t -> !alreadyReviewedTmTextUnitVariantIds.contains(t.getTmTextUnitVariantId()))
            .toList();
    logger.info(
        "All text unit dtos: {}, filtered: {}", allTextUnitDTOS.size(), textUnitDTOS.size());
    return textUnitDTOS;
  }

  record MyRecord(TextUnitDTO textUnitDTO, ChatCompletionsResponse chatCompletionsResponse) {}

  private Mono<Void> submitForSave(String runName, List<MyRecord> results) {
    String targetLocale = results.get(0).textUnitDTO().getTargetLocale();
    logger.info("Submit for save for locale {}", targetLocale);
    List<AiReviewProto> forSave =
        results.stream()
            .map(
                myRecord -> {
                  TextUnitDTO textUnitDTO = myRecord.textUnitDTO();
                  ChatCompletionsResponse chatCompletionsResponse =
                      myRecord.chatCompletionsResponse();

                  String jsonReview =
                      chatCompletionsResponse.choices().getFirst().message().content();
                  String completionOutputAsJson = jsonReview;

                  // this is just to check the format right now since we save the json anyway.
                  AiReviewSingleTextUnitOutput aiReviewSingleTextUnitOutput =
                      objectMapper.readValueUnchecked(
                          completionOutputAsJson, AiReviewSingleTextUnitOutput.class);

                  logger.debug(
                      "Review for text unit variant id: {}, is\n:{}",
                      textUnitDTO.getTmTextUnitVariantId(),
                      aiReviewSingleTextUnitOutput);

                  AiReviewProto aiReviewProto = new AiReviewProto();
                  aiReviewProto.setRunName(runName);
                  aiReviewProto.setTmTextUnitVariant(
                      textUnitVariantRepository.getReferenceById(
                          textUnitDTO.getTmTextUnitVariantId()));
                  aiReviewProto.setJsonReview(jsonReview);
                  return aiReviewProto;
                })
            .toList();

    trySaveAiReviewProtosInTx(forSave);
    logger.info("Done saving reviews for: {},  size: {}", targetLocale, forSave.size());
    return Mono.empty();
  }

  public void trySaveAiReviewProtosInTx(List<AiReviewProto> aiReviewProtos) {
    try {
      saveAiReviewProtosInTx(aiReviewProtos);
    } catch (Throwable t) {
      logger.error("Error while saving AiReviewProtos, swallow", t);
    }
  }

  @Transactional
  public void saveAiReviewProtosInTx(List<AiReviewProto> aiReviewProtos) {
    aiReviewProtoRepository.saveAll(aiReviewProtos);
  }

  private Mono<MyRecord> getChatCompletionForTextUnitDTO(
      TextUnitDTO textUnitDTO, String model, OpenAIClientPool openAIClientPool) {

    AiReviewSingleTextUnitInput aiReviewSingleTextUnitInput =
        new AiReviewService.AiReviewSingleTextUnitInput(
            textUnitDTO.getTargetLocale(),
            textUnitDTO.getSource(),
            textUnitDTO.getComment(),
            new AiReviewService.AiReviewSingleTextUnitInput.ExistingTarget(
                textUnitDTO.getTarget(), !textUnitDTO.isIncludedInLocalizedFile()));

    String inputAsJsonString =
        objectMapper.writeValueAsStringUnchecked(aiReviewSingleTextUnitInput);
    ObjectNode jsonSchema = createJsonSchema(AiReviewSingleTextUnitOutput.class);

    ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequest()
            .model(model)
            .temperature(getTemperatureForReasoningModels(model))
            .maxCompletionTokens(MAX_COMPLETION_TOKENS)
            .messages(
                List.of(
                    systemMessageBuilder().content(PROMPT).build(),
                    userMessageBuilder().content(inputAsJsonString).build()))
            .responseFormat(
                new ChatCompletionsRequest.JsonFormat(
                    "json_schema",
                    new ChatCompletionsRequest.JsonFormat.JsonSchema(
                        true, "request_json_format", jsonSchema)))
            .build();

    CompletableFuture<ChatCompletionsResponse> futureResult =
        openAIClientPool.submit(
            (openAIClient) -> openAIClient.getChatCompletions(chatCompletionsRequest));
    return Mono.fromFuture(futureResult)
        .handle(
            (chatCompletionsResponse, sink) -> {
              String jsonReview = chatCompletionsResponse.choices().getFirst().message().content();
              try {
                objectMapper.readValueUnchecked(jsonReview, AiReviewSingleTextUnitOutput.class);
              } catch (UncheckedIOException e) {
                logger.error(
                    "Can't deserialize the response: {}, content: {}", e.getMessage(), jsonReview);
                sink.error(e);
                return;
              }
              sink.next(new MyRecord(textUnitDTO, chatCompletionsResponse));
            });
  }

  private boolean isRetryableException(Throwable throwable) {
    Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
    return cause instanceof IOException || cause instanceof TimeoutException;
  }

  public void aiReviewBatch(AiReviewInput aiReviewInput, PollableTask currentTask)
      throws AiReviewException {

    Repository repository = getRepository(aiReviewInput);

    logger.debug("Start AI Review for repository: {}", repository.getName());

    try {
      Set<RepositoryLocale> repositoryLocalesWithoutRootLocale =
          getFilteredRepositoryLocales(aiReviewInput, repository);

      logger.debug("Create batches for repository: {}", repository.getName());
      List<OpenAIClient.CreateBatchResponse> batches =
          repositoryLocalesWithoutRootLocale.stream()
              .map(
                  createBatchForRepositoryLocale(
                      repository,
                      aiReviewInput.sourceTextMaxCountPerLocale(),
                      aiReviewInput.tmTextUnitIds(),
                      aiReviewInput.runName(),
                      getModel(aiReviewInput.useModel())))
              .filter(Objects::nonNull)
              .toList();

      logger.debug("Start a job to import batches for repository: {}", repository.getName());
      PollableFuture<AiReviewBatchesImportOutput> aiReviewBatchesImportOutputPollableFuture =
          aiReviewBatchesImportAsync(
              new AiReviewBatchesImportInput(batches, List.of(), 0, aiReviewInput.runName()),
              currentTask);

      logger.info(
          "Schedule AiReviewBatchesImportJob, id: {}",
          aiReviewBatchesImportOutputPollableFuture.getPollableTask().getId());

    } catch (OpenAIClient.OpenAIClientResponseException openAIClientResponseException) {
      logger.error(
          "Failed to ai review: %s".formatted(openAIClientResponseException),
          openAIClientResponseException);
      throw new AiReviewException(openAIClientResponseException);
    }
  }

  public void importBatch(
      OpenAIClient.RetrieveBatchResponse retrieveBatchResponse, String runName) {

    logger.info("Importing batch: {}", retrieveBatchResponse.id());

    logger.info("Download file content: {}", retrieveBatchResponse.outputFileId());
    OpenAIClient.DownloadFileContentResponse downloadFileContentResponse =
        getOpenAIClient()
            .downloadFileContent(
                new OpenAIClient.DownloadFileContentRequest(retrieveBatchResponse.outputFileId()));

    List<AiReviewProto> forSave =
        downloadFileContentResponse
            .content()
            .lines()
            .map(
                line -> {
                  OpenAIClient.ChatCompletionResponseBatchFileLine
                      chatCompletionResponseBatchFileLine =
                          objectMapper.readValueUnchecked(
                              line, OpenAIClient.ChatCompletionResponseBatchFileLine.class);

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

                  // check this was deserialzied
                  AiReviewSingleTextUnitOutput aiReviewSingleTextUnitOutput =
                      objectMapper.readValueUnchecked(
                          completionOutputAsJson, AiReviewSingleTextUnitOutput.class);

                  Long tmTextUnitVariantId =
                      Long.valueOf(chatCompletionResponseBatchFileLine.customId());
                  AiReviewProto aiReviewProto = new AiReviewProto();
                  aiReviewProto.setRunName(runName);
                  aiReviewProto.setTmTextUnitVariant(
                      textUnitVariantRepository.getReferenceById(tmTextUnitVariantId));
                  aiReviewProto.setJsonReview(completionOutputAsJson);
                  return aiReviewProto;
                })
            .toList();

    trySaveAiReviewProtosInTx(forSave);
  }

  /**
   * @return null if there is nothing to review
   */
  Function<RepositoryLocale, OpenAIClient.CreateBatchResponse> createBatchForRepositoryLocale(
      Repository repository,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      String runName,
      String model) {

    return repositoryLocale -> {
      logger.debug(
          "Get translated string for locale: '{}' in repository: '{}'",
          repositoryLocale.getLocale().getBcp47Tag(),
          repository.getName());

      List<TextUnitDTO> textUnitDTOS =
          getTextUnitDTOSForReview(
              repositoryLocale, sourceTextMaxCountPerLocale, tmTextUnitIds, runName);

      OpenAIClient.CreateBatchResponse createBatchResponse = null;
      if (textUnitDTOS.isEmpty()) {
        logger.info(
            "Nothing to review for locale: {}, don't create a batch",
            repositoryLocale.getLocale().getBcp47Tag());
      } else {
        String batchId =
            "%s_%s".formatted(repositoryLocale.getLocale().getBcp47Tag(), UUID.randomUUID());

        logger.debug("Generate the batch file content");
        String batchFileContent = generateBatchFileContent(textUnitDTOS, model);

        OpenAIClient.UploadFileResponse uploadFileResponse =
            getOpenAIClient()
                .uploadFile(
                    OpenAIClient.UploadFileRequest.forBatch(
                        "%s.jsonl".formatted(batchId), batchFileContent));

        logger.debug("Create the batch using file: {}", uploadFileResponse);
        createBatchResponse =
            getOpenAIClient()
                .createBatch(
                    forChatCompletion(
                        uploadFileResponse.id(),
                        Map.of(METADATA__TEXT_UNIT_DTOS__BLOB_ID, batchId)));

        logger.info("Create batch at {}: {}", ZonedDateTime.now(), createBatchResponse);

        logger.info(
            "Created batch for locale: {} with {} text units",
            repositoryLocale.getLocale().getBcp47Tag(),
            textUnitDTOS.size());
      }

      return createBatchResponse;
    };
  }

  String generateBatchFileContent(List<TextUnitDTO> textUnitDTOS, String model) {
    return textUnitDTOS.stream()
        .map(
            textUnitDTO -> {
              AiReviewSingleTextUnitInput aiReviewSingleTextUnitInput =
                  new AiReviewSingleTextUnitInput(
                      textUnitDTO.getTargetLocale(),
                      textUnitDTO.getSource(),
                      textUnitDTO.getComment(),
                      new AiReviewSingleTextUnitInput.ExistingTarget(
                          textUnitDTO.getTarget(), !textUnitDTO.isIncludedInLocalizedFile()));

              String inputAsJsonString =
                  objectMapper.writeValueAsStringUnchecked(aiReviewSingleTextUnitInput);

              ObjectNode jsonSchema = createJsonSchema(AiReviewSingleTextUnitOutput.class);

              ChatCompletionsRequest chatCompletionsRequest =
                  chatCompletionsRequest()
                      .model(getModel(model))
                      .maxCompletionTokens(MAX_COMPLETION_TOKENS)
                      .temperature(getTemperatureForReasoningModels(model))
                      .messages(
                          List.of(
                              systemMessageBuilder().content(PROMPT).build(),
                              userMessageBuilder().content(inputAsJsonString).build()))
                      .responseFormat(
                          new ChatCompletionsRequest.JsonFormat(
                              "json_schema",
                              new ChatCompletionsRequest.JsonFormat.JsonSchema(
                                  true, "request_json_format", jsonSchema)))
                      .build();

              return OpenAIClient.RequestBatchFileLine.forChatCompletion(
                  textUnitDTO.getTmTextUnitVariantId().toString(), chatCompletionsRequest);
            })
        .map(objectMapper::writeValueAsStringUnchecked)
        .collect(joining("\n"));
  }

  OpenAIClient.RetrieveBatchResponse retrieveBatchWithRetry(
      OpenAIClient.CreateBatchResponse batch) {

    return Mono.fromCallable(
            () ->
                getOpenAIClient().retrieveBatch(new OpenAIClient.RetrieveBatchRequest(batch.id())))
        .retryWhen(
            retryBackoffSpec.doBeforeRetry(
                doBeforeRetry -> {
                  logger.info("Retrying retrieving batch: {}", batch.id());
                }))
        .doOnError(
            throwable -> new RuntimeException("Failed to retrieve batch: " + batch.id(), throwable))
        .block();
  }

  private Set<RepositoryLocale> getFilteredRepositoryLocales(
      AiReviewInput aiReviewInput, Repository repository) {
    return repositoryService.getRepositoryLocalesWithoutRootLocale(repository).stream()
        .filter(
            rl ->
                aiReviewInput.targetBcp47tags == null
                    || aiReviewInput.targetBcp47tags.contains(rl.getLocale().getBcp47Tag()))
        .collect(Collectors.toSet());
  }

  private Repository getRepository(AiReviewInput aiReviewInput) {
    Repository repository = repositoryRepository.findByName(aiReviewInput.repositoryName());

    if (repository == null) {
      throw new RepositoryNameNotFoundException(
          String.format(
              "Repository with name '%s' can not be found!", aiReviewInput.repositoryName()));
    }
    return repository;
  }

  public static final String PROMPT =
      """
        Your role is to act as a translator.
        You are tasked with translating provided source strings while preserving both the tone and the technical structure of the string. This includes protecting any tags, placeholders, or code elements that should not be translated.

        The input will be provided in JSON format with the following fields:

            •	"source": The source text to be translated.
            •	"locale": The target language locale, following the BCP47 standard (e.g., “fr”, “es-419”).
            •	"sourceDescription": A description providing context for the source text.
            •	"existingTarget" (optional): An existing review to review.

        Instructions:

            •	If the source is colloquial, keep the review colloquial; if it’s formal, maintain formality in the review.
            •	Pay attention to regional variations specified in the "locale" field (e.g., “es” vs. “es-419”, “fr” vs. “fr-CA”, “zh” vs. “zh-Hant”), and ensure the review length remains similar to the source text.
            •	Aim to provide the best review, while compromising on length to ensure it remains close to the original text length

        Handling Tags and Code:

        Some strings contain code elements such as tags (e.g., {atag}, ICU message format, or HTML tags). You are provided with a inputs of tags that need to be protected. Ensure that:

            •	Tags like {atag} remain untouched.
            •	In cases of nested content (e.g., <a href={url}>text that needs review</a>), only translate the inner text while preserving the outer structure.
            •	Complex structures like ICU message formats should have placeholders or variables left intact (e.g., {count, plural, one {# item} other {# items}}), but translate any inner translatable text.

        Ambiguity and Context:

        After translating, assess the usefulness of the "sourceDescription" field:

            •	Rate its usefulness on a scale of 0 to 2:
            •	0 – Not helpful at all; irrelevant or misleading.
            •	1 – Somewhat helpful; provides partial or unclear context but is useful to some extent.
            •	2 – Very helpful; provides clear and sufficient guidance for the review.

        If the source is ambiguous—for example, if it could be interpreted as a noun or a verb—you must:

            •	Indicate the ambiguity in your explanation.
            •	Provide reviews for all possible interpretations.
            •	Set "reviewRequired" to true, and explain the need for review due to the ambiguity.

        You will provide an output in JSON format with the following fields:

            •	"source": The original source text.
            •	"target": An object containing:
            •	"content": The best review.
            •	"explanation": A brief explanation of your review choices.
            •	"confidenceLevel": Your confidence level (0-100%) in the review.
            •	"descriptionRating": An object containing:
            •	"explanation": An explanation of how the "sourceDescription" aided your review.
            •	"score": The usefulness score (0-2).
            •	"altTarget": An object containing:
            •	"content": An alternative review, if applicable. Focus on showcasing grammar differences,
            •	"explanation": Explanation for the alternative review.
            •	"confidenceLevel": Your confidence level (0-100%) in the alternative review.
            •	"existingTargetRating" (if "existingTarget" is provided): An object containing:
            •	"explanation": Feedback on the existing review’s accuracy and quality.
            •	"score": A rating score (0-2).
            •	"reviewRequired": An object containing:
            •	"required": true or false, indicating if review is needed.
            •	"reason": A detailed explanation of why review is or isn’t needed.
        """;

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
          "OpenAI client is not configured for AiReviewService. Ensure that the OpenAI API key is provided in the configuration (qualifier='aiReview').";
      logger.error(msg);
      throw new RuntimeException(msg);
    }
    return openAIClient;
  }

  public static class AiReviewException extends Exception {
    public AiReviewException(Throwable cause) {
      super(cause);
    }
  }
}
