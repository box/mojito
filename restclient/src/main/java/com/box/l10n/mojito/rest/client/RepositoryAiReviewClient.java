package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.PollableTask;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
public class RepositoryAiReviewClient extends BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryAiReviewClient.class);

  @Override
  public String getEntityName() {
    return "proto-ai-review";
  }

  /** Ai review strings in a repository for a given list of locales */
  public ProtoAiReviewResponse reviewRepository(ProtoAiReviewRequest protoAiReviewRequest) {

    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity(), protoAiReviewRequest, ProtoAiReviewResponse.class);
  }

  public long retryImport(Long childPollableTaskId) {
    ProtoAiReviewRetryImportResponse protoAiReviewRetryImportResponse =
        authenticatedRestTemplate.postForObject(
            getBasePathForEntity() + "/retry-import",
            new ProtoAiReviewRetryImportRequest(childPollableTaskId),
            ProtoAiReviewRetryImportResponse.class);
    return protoAiReviewRetryImportResponse.pollableTaskId();
  }

  public record ProtoAiReviewRequest(
      String repositoryName,
      List<String> targetBcp47tags,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      boolean useBatch,
      String useModel,
      String runName,
      String reviewType) {}

  public record ProtoAiReviewResponse(PollableTask pollableTask) {}

  public record ProtoAiReviewRetryImportRequest(long childPollableTaskId) {}

  public record ProtoAiReviewRetryImportResponse(long pollableTaskId) {}
}
