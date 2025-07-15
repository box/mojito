package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.entity.TMTextUnitVariant.Status;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClient.CreateBatchResponse;
import com.box.l10n.mojito.openai.OpenAIClient.DownloadFileContentResponse;
import com.box.l10n.mojito.openai.OpenAIClient.RetrieveBatchResponse;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AiTranslateBatchesImportJob
    extends QuartzPollableJob<
        AiTranslateBatchesImportJob.AiTranslateBatchesImportInput,
        AiTranslateBatchesImportJob.AiTranslateBatchesImportOutput> {

  static Logger logger = LoggerFactory.getLogger(AiTranslateBatchesImportJob.class);

  @Autowired AiTranslateService aiTranslateService;

  @Qualifier("AiTranslate")
  @Autowired
  private OpenAIClient openAIClient;

  public record AiTranslateBatchesImportInput(
      List<CreateBatchResponse> createBatchResponses,
      List<String> skippedLocales,
      List<String> batchCreationErrors,
      List<String> processed,
      Map<String, String> failedImport,
      int attempt,
      String translateType,
      String importStatus) {}

  public record AiTranslateBatchesImportOutput(
      List<RetrieveBatchResponse> retrieveBatchResponses,
      List<String> processed,
      Map<String, String> failedToImport,
      Long nextJob,
      List<String> skippedLocales,
      List<String> batchCreationErrors) {}

  @Override
  public AiTranslateBatchesImportOutput call(
      AiTranslateBatchesImportInput aiTranslateBatchesImportInput) throws Exception {

    List<RetrieveBatchResponse> retrieveBatchResponses = new ArrayList<>();
    Set<String> processed = new HashSet<>(aiTranslateBatchesImportInput.processed());
    Map<String, String> failedImport = new HashMap<>(aiTranslateBatchesImportInput.failedImport());

    Long parentTaskId = getCurrentPollableTask().getParentTask().getId();

    logger.info(
        "[task id: {}] Batches already processed: {} and total: {}",
        parentTaskId,
        processed.size(),
        aiTranslateBatchesImportInput.createBatchResponses().size());

    for (CreateBatchResponse createBatchResponse :
        aiTranslateBatchesImportInput.createBatchResponses()) {

      logger.debug(
          "[task id: {}] Retrieve current status of batch, regardless if it was already processed: {}",
          parentTaskId,
          createBatchResponse.id());
      RetrieveBatchResponse retrieveBatchResponse =
          aiTranslateService.retrieveBatchWithRetry(createBatchResponse);
      retrieveBatchResponses.add(retrieveBatchResponse);

      if (!processed.contains(createBatchResponse.id())) {
        switch (retrieveBatchResponse.status()) {
          case "completed" -> {
            logger.info("[task id: {}] Completed batch: {}", parentTaskId, retrieveBatchResponse);

            try {
              List<String> errors =
                  aiTranslateService.importBatch(
                      retrieveBatchResponse,
                      AiTranslateType.fromString(aiTranslateBatchesImportInput.translateType()),
                      Status.valueOf(aiTranslateBatchesImportInput.importStatus()));

              if (!errors.isEmpty()) {
                failedImport.put(createBatchResponse.id(), String.join("\n", errors));
              }
            } catch (Throwable t) {
              logger.error(
                  "[task id: {}] Failed to import batch: {}, skip",
                  parentTaskId,
                  createBatchResponse.id(),
                  t);
              failedImport.put(createBatchResponse.id(), t.getMessage());
            }

            if (retrieveBatchResponse.errorFileId() != null) {
              logger.error(
                  "[task id: {}] {} completed but with an error file: {}",
                  parentTaskId,
                  createBatchResponse.id(),
                  retrieveBatchResponse.errorFileId());
              String message;
              try {
                DownloadFileContentResponse downloadFileContentResponse =
                    openAIClient.downloadFileContent(
                        new OpenAIClient.DownloadFileContentRequest(
                            retrieveBatchResponse.errorFileId()));
                message = downloadFileContentResponse.content();
              } catch (Exception e) {
                message = "Failed to read error file: " + retrieveBatchResponse.errorFileId();
              }
              String currentMessage = failedImport.get(createBatchResponse.id());
              failedImport.put(
                  createBatchResponse.id(),
                  currentMessage == null ? message : currentMessage + "\n\n" + message);
            }

            processed.add(createBatchResponse.id());
          }
          case "failed", "expired", "cancelled" -> {
            logger.error(
                "[task id: {}]  Batch has status: {}, skipping import. response: {}",
                parentTaskId,
                retrieveBatchResponse.status(),
                retrieveBatchResponse);
            processed.add(createBatchResponse.id());
          }
          case null, default ->
              logger.info(
                  "[task id: {}] Batch has status: {}, will process later: {}",
                  parentTaskId,
                  retrieveBatchResponse.status(),
                  retrieveBatchResponse);
        }
      }
    }

    PollableFuture<AiTranslateBatchesImportOutput> pollableFuture = null;

    if (processed.size() >= aiTranslateBatchesImportInput.createBatchResponses().size()) {
      logger.info(
          "[task id: {}]  Everything has been processed ({}/{}), don't reschedule",
          parentTaskId,
          processed.size(),
          aiTranslateBatchesImportInput.createBatchResponses().size());
    } else {
      logger.info(
          "[task id: {}] Schedule new job to process remaining batches, processed: {}",
          parentTaskId,
          processed);
      pollableFuture =
          aiTranslateService.aiTranslateBatchesImportAsync(
              new AiTranslateBatchesImportInput(
                  aiTranslateBatchesImportInput.createBatchResponses(),
                  aiTranslateBatchesImportInput.skippedLocales(),
                  aiTranslateBatchesImportInput.batchCreationErrors(),
                  processed.stream().toList(),
                  failedImport,
                  aiTranslateBatchesImportInput.attempt() + 1,
                  aiTranslateBatchesImportInput.translateType(),
                  aiTranslateBatchesImportInput.importStatus()),
              getCurrentPollableTask().getParentTask());
      logger.info(
          "[task id: {}] New job created with pollableTask id: {}",
          parentTaskId,
          pollableFuture.getPollableTask().getId());
    }

    return new AiTranslateBatchesImportOutput(
        retrieveBatchResponses,
        processed.stream().toList(),
        failedImport,
        pollableFuture != null ? pollableFuture.getPollableTask().getId() : null,
        aiTranslateBatchesImportInput.skippedLocales(),
        aiTranslateBatchesImportInput.batchCreationErrors());
  }
}
