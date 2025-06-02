package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateConfigurationProperties;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateService;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateService.AiTranslateInput;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiTranslateWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiTranslateWS.class);

  @Autowired AiTranslateService aiTranslateService;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired AiTranslateConfigurationProperties aiTranslateConfigurationProperties;

  @RequestMapping(method = RequestMethod.POST, value = "/api/proto-ai-translate")
  @ResponseStatus(HttpStatus.OK)
  public ProtoAiTranslateResponse aiTranslate(
      @RequestBody ProtoAiTranslateRequest protoAiTranslateRequest) {

    PollableFuture<Void> pollableFuture =
        aiTranslateService.aiTranslateAsync(
            new AiTranslateInput(
                protoAiTranslateRequest.repositoryName(),
                protoAiTranslateRequest.targetBcp47tags(),
                protoAiTranslateRequest.sourceTextMaxCountPerLocale(),
                protoAiTranslateRequest.tmTextUnitIds(),
                protoAiTranslateRequest.useBatch(),
                protoAiTranslateRequest.useModel(),
                protoAiTranslateRequest.promptSuffix()));

    return new ProtoAiTranslateResponse(pollableFuture.getPollableTask());
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
