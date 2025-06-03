package com.box.l10n.mojito.service.oaireview;

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
public class AiReviewBatchesImportJob
    extends QuartzPollableJob<
        AiReviewBatchesImportJob.AiReviewBatchesImportInput,
        AiReviewBatchesImportJob.AiReviewBatchesImportOutput> {

  static Logger logger = LoggerFactory.getLogger(AiReviewBatchesImportJob.class);

  @Autowired AiReviewService aiReviewService;

  @Qualifier("openAIClientReview")
  @Autowired
  private OpenAIClient openAIClient;

  public record AiReviewBatchesImportInput(
      List<CreateBatchResponse> createBatchResponses,
      List<String> processed,
      int attempt,
      String runName) {}

  public record AiReviewBatchesImportOutput(
      List<RetrieveBatchResponse> retrieveBatchResponses,
      List<String> processed,
      Map<String, String> failedToImport,
      Long nextJob,
      String runName) {}

  @Override
  public AiReviewBatchesImportOutput call(AiReviewBatchesImportInput aiReviewBatchesImportInput)
      throws Exception {

    List<RetrieveBatchResponse> retrieveBatchResponses = new ArrayList<>();
    Set<String> processed = new HashSet<>(aiReviewBatchesImportInput.processed());
    Map<String, String> failedImport = new HashMap<>();

    Long parentTaskId = getCurrentPollableTask().getParentTask().getId();

    logger.info(
        "[task id: {}] Batches already processed: {} and total: {}",
        parentTaskId,
        processed.size(),
        aiReviewBatchesImportInput.createBatchResponses.size());

    for (CreateBatchResponse createBatchResponse :
        aiReviewBatchesImportInput.createBatchResponses()) {

      logger.debug(
          "[task id: {}] Retrieve current status of batch, regardless if it was already processed: {}",
          parentTaskId,
          createBatchResponse.id());
      RetrieveBatchResponse retrieveBatchResponse =
          aiReviewService.retrieveBatchWithRetry(createBatchResponse);
      retrieveBatchResponses.add(retrieveBatchResponse);

      if (!processed.contains(createBatchResponse.id())) {
        switch (retrieveBatchResponse.status()) {
          case "completed" -> {
            logger.info("[task id: {}] Completed batch: {}", parentTaskId, retrieveBatchResponse);

            if (retrieveBatchResponse.errorFileId() != null) {
              logger.error(
                  "[task id: {}] Completed but with an error file: {}",
                  parentTaskId,
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
              failedImport.put(createBatchResponse.id(), message);
            } else {
              try {
                aiReviewService.importBatch(
                    retrieveBatchResponse, aiReviewBatchesImportInput.runName());
              } catch (Throwable t) {
                logger.error(
                    "[task id: {}] Failed to import batch: {}, skip",
                    parentTaskId,
                    createBatchResponse.id(),
                    t);
                failedImport.put(createBatchResponse.id(), t.getMessage());
              }
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

    PollableFuture<AiReviewBatchesImportOutput> pollableFuture = null;

    if (processed.size() >= aiReviewBatchesImportInput.createBatchResponses().size()) {
      logger.info(
          "[task id: {}]  Everything has been processed ({}/{}), don't reschedule",
          parentTaskId,
          processed.size(),
          aiReviewBatchesImportInput.createBatchResponses().size());
    } else {
      logger.info(
          "[task id: {}] Schedule new job to process remaining batches, processed: {}",
          parentTaskId,
          processed);
      pollableFuture =
          aiReviewService.aiReviewBatchesImportAsync(
              new AiReviewBatchesImportInput(
                  aiReviewBatchesImportInput.createBatchResponses(),
                  processed.stream().toList(),
                  aiReviewBatchesImportInput.attempt() + 1,
                  aiReviewBatchesImportInput.runName()),
              getCurrentPollableTask().getParentTask());
      logger.info(
          "[task id: {}] New job created with pollableTask id: {}",
          parentTaskId,
          pollableFuture.getPollableTask().getId());
    }

    return new AiReviewBatchesImportOutput(
        retrieveBatchResponses,
        processed.stream().toList(),
        failedImport,
        pollableFuture != null ? pollableFuture.getPollableTask().getId() : null,
        aiReviewBatchesImportInput.runName());
  }
}
