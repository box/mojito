package com.box.l10n.mojito.service.oaireview;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.JsonFormat.JsonSchema.createJsonSchema;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;

import com.box.l10n.mojito.entity.AiReviewProto;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsResponse;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryNameNotFoundException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.AiReviewProtoRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
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
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
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

  TextUnitBatchImporterService textUnitBatchImporterService;

  StructuredBlobStorage structuredBlobStorage;

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
      TextUnitBatchImporterService textUnitBatchImporterService,
      StructuredBlobStorage structuredBlobStorage,
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
    this.textUnitBatchImporterService = Objects.requireNonNull(textUnitBatchImporterService);
    this.structuredBlobStorage = Objects.requireNonNull(structuredBlobStorage);
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
      boolean useBatch) {}

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
            .maxTokens(16384)
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

  public void aiReview(AiReviewInput aiReviewInput) throws AiReviewException {
    if (aiReviewInput.useBatch()) {
      throw new UnsupportedOperationException("Only non batch for review");
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
                    openAIClientPool),
            10)
        .then()
        .doOnTerminate(
            () ->
                logger.info(
                    "Done with AI Review (no batch) for repository: {}", repository.getName()))
        .block();
  }

  Mono<Void> asyncReviewNoBatchLocale(
      RepositoryLocale repositoryLocale,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      OpenAIClientPool openAIClientPool) {

    Repository repository = repositoryLocale.getRepository();

    logger.info("Get already reviewed tm text unit variants");
    Set<Long> alreadyReviewedTmTextUnitVariantIds =
        aiReviewProtoRepository.findTmTextUnitVariantIdsByLocaleIdAndRepositoryId(
            repositoryLocale.getLocale().getId(), repositoryLocale.getRepository().getId());

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
                            getChatCompletionForTextUnitDTO(textUnitDTO, openAIClientPool)
                                .retryWhen(
                                    Retry.backoff(5, Duration.ofSeconds(1))
                                        .filter(this::isRetriableException)
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
                    .flatMap(this::submitForSave)
                    .doOnTerminate(() -> logger.info("Done submitting for processing")))
        .then();
  }

  record MyRecord(TextUnitDTO textUnitDTO, ChatCompletionsResponse chatCompletionsResponse) {}

  private Mono<Void> submitForSave(List<MyRecord> results) {
    logger.info("Submit for save for locale {}", results.get(0).textUnitDTO().getTargetLocale());
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
                  aiReviewProto.setTmTextUnitVariant(
                      textUnitVariantRepository.getReferenceById(
                          textUnitDTO.getTmTextUnitVariantId()));
                  aiReviewProto.setJsonReview(jsonReview);
                  return aiReviewProto;
                })
            .toList();

    saveAiReviewProtosInTx(forSave);
    return Mono.empty();
  }

  @Transactional
  public void saveAiReviewProtosInTx(List<AiReviewProto> aiReviewProtos) {
    aiReviewProtoRepository.saveAll(aiReviewProtos);
  }

  private Mono<MyRecord> getChatCompletionForTextUnitDTO(
      TextUnitDTO textUnitDTO, OpenAIClientPool openAIClientPool) {

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
            .model(aiReviewConfigurationProperties.getModelName())
            .maxTokens(16384)
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
        .map(chatCompletionsResponse -> new MyRecord(textUnitDTO, chatCompletionsResponse));
  }

  private boolean isRetriableException(Throwable throwable) {
    Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
    return cause instanceof IOException || cause instanceof TimeoutException;
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

  public class AiReviewException extends Exception {
    public AiReviewException(Throwable cause) {
      super(cause);
    }
  }
}
