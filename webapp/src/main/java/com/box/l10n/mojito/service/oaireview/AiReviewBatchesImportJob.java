package com.box.l10n.mojito.service.oaireview;

import com.box.l10n.mojito.openai.OpenAIClient.CreateBatchResponse;
import com.box.l10n.mojito.openai.OpenAIClient.RetrieveBatchResponse;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiReviewBatchesImportJob
    extends QuartzPollableJob<
        AiReviewBatchesImportJob.AiReviewBatchesImportInput,
        AiReviewBatchesImportJob.AiReviewBatchesImportOutput> {

  static Logger logger = LoggerFactory.getLogger(AiReviewBatchesImportJob.class);

  @Autowired AiReviewService aiReviewService;
  @Autowired private PollableTaskService pollableTaskService;

  public record AiReviewBatchesImportInput(
      List<CreateBatchResponse> createBatchResponses, List<String> processed, int attempt) {}

  public record AiReviewBatchesImportOutput(
      List<RetrieveBatchResponse> retrieveBatchResponses,
      List<String> processed,
      Map<String, String> failedToImport,
      Long nextJob) {}

  @Override
  public AiReviewBatchesImportOutput call(AiReviewBatchesImportInput aiReviewBatchesImportInput)
      throws Exception {

    List<RetrieveBatchResponse> retrieveBatchResponses = new ArrayList<>();
    Set<String> processed = new HashSet<>(aiReviewBatchesImportInput.processed());
    Map<String, String> failedImport = new HashMap<>();

    logger.info(
        "Batches already processed: {} and total: {}",
        processed.size(),
        aiReviewBatchesImportInput.createBatchResponses.size());

    for (CreateBatchResponse createBatchResponse :
        aiReviewBatchesImportInput.createBatchResponses()) {

      logger.debug(
          "Retrieve current status of batch, regardless if it was already processed: {}",
          createBatchResponse.id());
      RetrieveBatchResponse retrieveBatchResponse =
          aiReviewService.retrieveBatchWithRetry(createBatchResponse);
      retrieveBatchResponses.add(retrieveBatchResponse);

      if (!processed.contains(createBatchResponse.id())) {
        if ("completed".equals(retrieveBatchResponse.status())) {
          logger.info("Completed batch: {}", retrieveBatchResponse.id());
          try {
            aiReviewService.importBatch(retrieveBatchResponse);
          } catch (Throwable t) {
            logger.error("Failed to import batch: {}, skip", createBatchResponse.id(), t);
            failedImport.put(createBatchResponse.id(), t.getMessage());
            processed.add(createBatchResponse.id());
          }
          processed.add(createBatchResponse.id());
        } else if ("failed".equals(retrieveBatchResponse.status())) {
          logger.error("Batch failed, skipping it: {}", retrieveBatchResponse);
          processed.add(createBatchResponse.id());
        } else if ("expired".equals(retrieveBatchResponse.status())) {
          logger.info("Batch expired, skipping it: {}", retrieveBatchResponse);
          processed.add(createBatchResponse.id());
        } else if ("cancelled".equals(retrieveBatchResponse.status())) {
          logger.info("Batch cancelled, skipping it: {}", retrieveBatchResponse);
          processed.add(createBatchResponse.id());
        } else {
          logger.info("Batch is still processing will process later: {}", retrieveBatchResponse);
        }
      }
    }

    PollableFuture<AiReviewBatchesImportOutput> pollableFuture = null;

    if (processed.size() >= aiReviewBatchesImportInput.createBatchResponses().size()) {
      logger.info(
          "Everything has been processed ({}/{}), don't reschedule",
          processed.size(),
          aiReviewBatchesImportInput.createBatchResponses().size());
    } else {
      logger.info("Schedule new job to process remaining batches, processed: {}", processed);
      pollableFuture =
          aiReviewService.aiReviewBatchesImportAsync(
              new AiReviewBatchesImportInput(
                  aiReviewBatchesImportInput.createBatchResponses(),
                  processed.stream().toList(),
                  aiReviewBatchesImportInput.attempt() + 1),
              getCurrentPollableTask().getParentTask());
      logger.info(
          "New job created with pollableTask id: {}", pollableFuture.getPollableTask().getId());
    }

    return new AiReviewBatchesImportOutput(
        retrieveBatchResponses,
        processed.stream().toList(),
        failedImport,
        pollableFuture != null ? pollableFuture.getPollableTask().getId() : null);
  }
}
