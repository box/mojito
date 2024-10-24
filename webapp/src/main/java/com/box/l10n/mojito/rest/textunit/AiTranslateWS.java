package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateConfigurationProperties;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
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
  public PollableTask aiTranslate(@RequestBody ProtoAiTranslateRequest protoAiTranslateRequest)
      throws RepositoryWithIdNotFoundException {

    repositoryRepository
        .findById(protoAiTranslateRequest.repositoryId())
        .orElseThrow(
            () -> new RepositoryWithIdNotFoundException(protoAiTranslateRequest.repositoryId()));

    PollableFuture<Void> pollableFuture =
        aiTranslateService.aiTranslateAsync(protoAiTranslateRequest.repositoryId());
    return pollableFuture.getPollableTask();
  }

  public record ProtoAiTranslateRequest(long repositoryId) {}
}
