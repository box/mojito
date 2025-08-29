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
public class RepositoryAiTranslateClient extends BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryAiTranslateClient.class);

  @Override
  public String getEntityName() {
    return "proto-ai-translate";
  }

  /** Ai translate untranslated and rejected strings in a repository for a given list of locales */
  public ProtoAiTranslateResponse translateRepository(
      ProtoAiTranslateRequest protoAiTranslateRequest) {

    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity(), protoAiTranslateRequest, ProtoAiTranslateResponse.class);
  }

  public long retryImport(Long childPollableTaskId, boolean resume) {
    ProtoAiTranslateRetryImportResponse protoAiReviewRetryImportResponse =
        authenticatedRestTemplate.postForObject(
            getBasePathForEntity() + "/retry-import",
            new ProtoAiTranslateRetryImportRequest(childPollableTaskId, resume),
            ProtoAiTranslateRetryImportResponse.class);
    return protoAiReviewRetryImportResponse.pollableTaskId();
  }

  public ProtoAiTranslateGetReportResponse getReport(long pollableTaskId) {
    return authenticatedRestTemplate.getForObject(
        getBasePathForEntity() + "/report/" + pollableTaskId,
        ProtoAiTranslateGetReportResponse.class);
  }

  public ProtoAiTranslateGetReportLocaleResponse getReportLocale(String filename) {
    return authenticatedRestTemplate.getForObject(
        getBasePathForEntity() + "/report/" + filename,
        ProtoAiTranslateGetReportLocaleResponse.class);
  }

  public record ProtoAiTranslateRequest(
      String repositoryName,
      List<String> targetBcp47tags,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      boolean useBatch,
      String useModel,
      String promptSuffix,
      String relatedStringsType,
      String translateType,
      String statusFilter,
      String importStatus,
      String glossaryName,
      String glossaryTermSource,
      String glossaryTermSourceDescription,
      String glossaryTermTarget,
      String glossaryTermTargetDescription,
      boolean glossaryTermDoNotTranslate,
      boolean glossaryTermCaseSensitive,
      boolean glossaryOnlyMatchedTextUnits,
      boolean dryRun,
      Integer timeoutSeconds) {}

  public record ProtoAiTranslateResponse(PollableTask pollableTask) {}

  public record ProtoAiTranslateRetryImportRequest(long childPollableTaskId, boolean resume) {}

  public record ProtoAiTranslateRetryImportResponse(long pollableTaskId) {}

  public record ProtoAiTranslateGetReportResponse(List<String> reportLocaleUrls) {}

  public record ProtoAiTranslateGetReportLocaleResponse(String content) {}
}
