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

  public record ProtoAiTranslateRequest(
      String repositoryName,
      List<String> targetBcp47tags,
      int sourceTextMaxCountPerLocale,
      List<Long> tmTextUnitIds,
      boolean useBatch,
      String useModel,
      String promptSuffix) {}

  public record ProtoAiTranslateResponse(PollableTask pollableTask) {}
}
