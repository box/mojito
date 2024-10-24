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
import static com.box.l10n.mojito.openai.OpenAIClient.UploadFileRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.UploadFileResponse;
import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.AI_TRANSLATE_WS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClient.CreateBatchResponse;
import com.box.l10n.mojito.openai.OpenAIClient.RequestBatchFileLine;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryNameNotFoundException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@Service
public class AiTranslateService {

  static final String METADATA__TEXT_UNIT_DTOS__BLOB_ID = "textUnitDTOs";

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiTranslateService.class);

  TextUnitSearcher textUnitSearcher;

  RepositoryRepository repositoryRepository;

  RepositoryService repositoryService;

  AiTranslateConfigurationProperties aiTranslateConfigurationProperties;

  OpenAIClient openAIClient;

  TextUnitBatchImporterService textUnitBatchImporterService;

  StructuredBlobStorage structuredBlobStorage;

  ObjectMapper objectMapper;

  RetryBackoffSpec retryBackoffSpec;

  QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  public AiTranslateService(
      TextUnitSearcher textUnitSearcher,
      RepositoryRepository repositoryRepository,
      RepositoryService repositoryService,
      TextUnitBatchImporterService textUnitBatchImporterService,
      StructuredBlobStorage structuredBlobStorage,
      AiTranslateConfigurationProperties aiTranslateConfigurationProperties,
      @Qualifier("AiTranslate") @Autowired(required = false) OpenAIClient openAIClient,
      @Qualifier("AiTranslate") ObjectMapper objectMapper,
      @Qualifier("AiTranslate") RetryBackoffSpec retryBackoffSpec,
      QuartzPollableTaskScheduler quartzPollableTaskScheduler) {
    this.textUnitSearcher = textUnitSearcher;
    this.repositoryRepository = repositoryRepository;
    this.repositoryService = repositoryService;
    this.textUnitBatchImporterService = textUnitBatchImporterService;
    this.structuredBlobStorage = structuredBlobStorage;
    this.aiTranslateConfigurationProperties = aiTranslateConfigurationProperties;
    this.objectMapper = objectMapper;
    this.openAIClient = openAIClient;
    this.retryBackoffSpec = retryBackoffSpec;
    this.quartzPollableTaskScheduler = quartzPollableTaskScheduler;
  }

  public record AiTranslateInput(
      String repositoryName, List<String> targetBcp47tags, int sourceTextMaxCountPerLocale) {}

  public PollableFuture<Void> aiTranslateAsync(AiTranslateInput aiTranslateInput) {

    QuartzJobInfo<AiTranslateInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(AiTranslateJob.class)
            .withInlineInput(false)
            .withInput(aiTranslateInput)
            .withScheduler(aiTranslateConfigurationProperties.getSchedulerName())
            .build();

    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  public void aiTranslate(AiTranslateInput aiTranslateInput) throws AiTranslateException {

    Repository repository = repositoryRepository.findByName(aiTranslateInput.repositoryName());

    if (repository == null) {
      throw new RepositoryNameNotFoundException(
          String.format(
              "Repository with name '%s' can not be found!", aiTranslateInput.repositoryName()));
    }

    logger.debug("Start AI Translation for repository: {}", repository.getName());

    try {
      Set<RepositoryLocale> repositoryLocalesWithoutRootLocale =
          repositoryService.getRepositoryLocalesWithoutRootLocale(repository).stream()
              .filter(
                  rl ->
                      aiTranslateInput.targetBcp47tags == null
                          || aiTranslateInput.targetBcp47tags.contains(
                              rl.getLocale().getBcp47Tag()))
              .collect(Collectors.toSet());

      logger.debug("Create batches for repository: {}", repository.getName());
      ArrayDeque<CreateBatchResponse> batches =
          repositoryLocalesWithoutRootLocale.stream()
              .map(
                  createBatchForRepositoryLocale(
                      repository, aiTranslateInput.sourceTextMaxCountPerLocale()))
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(ArrayDeque::new));

      logger.debug("Import batches for repository: {}", repository.getName());
      while (!batches.isEmpty()) {
        RetrieveBatchResponse retrieveBatchResponse = getNextFinishedBatch(batches);
        importBatch(retrieveBatchResponse);
      }
    } catch (OpenAIClient.OpenAIClientResponseException openAIClientResponseException) {
      logger.error(
          "Failed to ai translate: %s".formatted(openAIClientResponseException),
          openAIClientResponseException);
      throw new AiTranslateException(openAIClientResponseException);
    }
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

                  String aiTranslateOutputAsJson =
                      chatCompletionResponseBatchFileLine
                          .response()
                          .chatCompletionsResponse()
                          .choices()
                          .getFirst()
                          .message()
                          .content();

                  AiTranslateOutput aiTranslateOutput =
                      objectMapper.readValueUnchecked(
                          aiTranslateOutputAsJson, AiTranslateOutput.class);

                  TextUnitDTO textUnitDTO =
                      tmTextUnitIdToTextUnitDTOs.get(
                          Long.valueOf(chatCompletionResponseBatchFileLine.customId()));
                  textUnitDTO.setTarget(aiTranslateOutput.target().content());
                  textUnitDTO.setTargetComment("ai-translate");
                  return textUnitDTO;
                })
            .toList();

    textUnitBatchImporterService.importTextUnits(
        forImport,
        TextUnitBatchImporterService.IntegrityChecksType.ALWAYS_USE_INTEGRITY_CHECKER_STATUS);
  }

  Function<RepositoryLocale, CreateBatchResponse> createBatchForRepositoryLocale(
      Repository repository, int sourceTextMaxCountPerLocale) {

    return repositoryLocale -> {
      logger.debug(
          "Get untranslated string for locale: '{}' in repository: '{}'",
          repositoryLocale.getLocale().getBcp47Tag(),
          repository.getName());
      TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
      textUnitSearcherParameters.setRepositoryIds(repository.getId());
      textUnitSearcherParameters.setStatusFilter(StatusFilter.UNTRANSLATED);
      textUnitSearcherParameters.setLocaleId(repositoryLocale.getLocale().getId());
      textUnitSearcherParameters.setLimit(sourceTextMaxCountPerLocale);
      List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(textUnitSearcherParameters);

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
        String batchFileContent = generateBatchFileContent(textUnitDTOS);

        logger.debug("Upload batch file content: {}", batchFileContent);
        UploadFileResponse uploadFileResponse =
            getOpenAIClient()
                .uploadFile(
                    UploadFileRequest.forBatch("%s.jsonl".formatted(batchId), batchFileContent));

        logger.debug("Create the batch using file: {}", uploadFileResponse);
        createBatchResponse =
            getOpenAIClient()
                .createBatch(
                    forChatCompletion(
                        uploadFileResponse.id(),
                        Map.of(METADATA__TEXT_UNIT_DTOS__BLOB_ID, batchId)));
      }

      logger.info(
          "Created batch for locale: {} with {} text units",
          repositoryLocale.getLocale().getBcp47Tag(),
          textUnitDTOS.size());
      return createBatchResponse;
    };
  }

  String generateBatchFileContent(List<TextUnitDTO> textUnitDTOS) {
    return textUnitDTOS.stream()
        .map(
            textUnitDTO -> {
              CompletionInput completionInput =
                  new CompletionInput(
                      textUnitDTO.getTargetLocale(),
                      textUnitDTO.getSource(),
                      textUnitDTO.getComment());

              String inputAsJsonString = objectMapper.writeValueAsStringUnchecked(completionInput);

              ObjectNode jsonSchema = createJsonSchema(AiTranslateOutput.class);

              ChatCompletionsRequest chatCompletionsRequest =
                  chatCompletionsRequest()
                      .model("gpt-4o-2024-08-06")
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

              return RequestBatchFileLine.forChatCompletion(
                  textUnitDTO.getTmTextUnitId().toString(), chatCompletionsRequest);
            })
        .map(objectMapper::writeValueAsStringUnchecked)
        .collect(joining("\n"));
  }

  /**
   * Use a queue to not stay stuck on a slow job, and try to import faster. Batch are imported
   * sequentially.
   *
   * <p>Note: This is an active blocking pooling which blocks the thread but is isolated in a thread
   * pool.
   */
  RetrieveBatchResponse getNextFinishedBatch(ArrayDeque<CreateBatchResponse> batches) {
    while (true) {
      int size = batches.size();

      for (int i = 0; i < size; i++) {
        CreateBatchResponse batch = batches.removeFirst();

        logger.debug("Retrieve current status of batch: {}", batch.id());
        RetrieveBatchResponse retrieveBatchResponse = retrieveBatchWithRetry(batch);

        if ("completed".equals(retrieveBatchResponse.status())) {
          logger.info("Next completed batch is:  {}", retrieveBatchResponse.id());
          return retrieveBatchResponse;
        } else if ("failed".equals(retrieveBatchResponse.status())) {
          logger.error("Batch failed, skipping it: {}", retrieveBatchResponse);
        } else {
          logger.debug(
              "Batch is still processing append to the end of the queue: {}",
              retrieveBatchResponse);
          batches.offerLast(batch);
        }
      }
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

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

  record CompletionInput(String locale, String source, String sourceDescription) {}

  record AiTranslateOutput(
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

  record AiTranslateBlobStorage(List<TextUnitDTO> textUnitDTOS) {}

  static final String PROMPT =
      """
      Your role is to act as a translator.
      You are tasked with translating provided source strings while preserving both the tone and the technical structure of the string. This includes protecting any tags, placeholders, or code elements that should not be translated.

      The input will be provided in JSON format with the following fields:

          •	"source": The source text to be translated.
          •	"locale": The target language locale, following the BCP47 standard (e.g., “fr”, “es-419”).
          •	"sourceDescription": A description providing context for the source text.
          •	"existingTarget" (optional): An existing translation to review.

      Instructions:

          •	If the source is colloquial, keep the translation colloquial; if it’s formal, maintain formality in the translation.
          •	Pay attention to regional variations specified in the "locale" field (e.g., “es” vs. “es-419”, “fr” vs. “fr-CA”, “zh” vs. “zh-Hant”), and ensure the translation length remains similar to the source text.

      Handling Tags and Code:

      Some strings contain code elements such as tags (e.g., {atag}, ICU message format, or HTML tags). You are provided with a inputs of tags that need to be protected. Ensure that:

          •	Tags like {atag} remain untouched.
          •	In cases of nested content (e.g., <a href={url}>text that needs translation</a>), only translate the inner text while preserving the outer structure.
          •	Complex structures like ICU message formats should have placeholders or variables left intact (e.g., {count, plural, one {# item} other {# items}}), but translate any inner translatable text.

      Ambiguity and Context:

      After translating, assess the usefulness of the "sourceDescription" field:

          •	Rate its usefulness on a scale of 0 to 2:
          •	0 – Not helpful at all; irrelevant or misleading.
          •	1 – Somewhat helpful; provides partial or unclear context but is useful to some extent.
          •	2 – Very helpful; provides clear and sufficient guidance for the translation.

      If the source is ambiguous—for example, if it could be interpreted as a noun or a verb—you must:

          •	Indicate the ambiguity in your explanation.
          •	Provide translations for all possible interpretations.
          •	Set "reviewRequired" to true, and explain the need for review due to the ambiguity.

      You will provide an output in JSON format with the following fields:

          •	"source": The original source text.
          •	"target": An object containing:
          •	"content": The best translation.
          •	"explanation": A brief explanation of your translation choices.
          •	"confidenceLevel": Your confidence level (0-100%) in the translation.
          •	"descriptionRating": An object containing:
          •	"explanation": An explanation of how the "sourceDescription" aided your translation.
          •	"score": The usefulness score (0-2).
          •	"altTarget": An object containing:
          •	"content": An alternative translation, if applicable. Focus on showcasing grammar differences,
          •	"explanation": Explanation for the alternative translation.
          •	"confidenceLevel": Your confidence level (0-100%) in the alternative translation.
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
          "OpenAI client is not configured for AiTranslateService. Ensure that the OpenAI API key is provided in the configuration (qualifier='aiTranslate').";
      logger.error(msg);
      throw new RuntimeException(msg);
    }
    return openAIClient;
  }

  public class AiTranslateException extends Exception {
    public AiTranslateException(Throwable cause) {
      super(cause);
    }
  }
}
