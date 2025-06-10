package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.AiReviewProto;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.textunit.AiReviewType.AiReviewTextUnitVariantOutput;
import com.box.l10n.mojito.service.oaireview.AiReviewService;
import com.box.l10n.mojito.service.oaireview.AiReviewService.AiReviewTextUnitVariantInput;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.tm.AiReviewProtoRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiReviewWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiReviewWS.class);

  static final String RUN_NAME_FOR_FRONTEND = "for-frontend";

  AiReviewProtoRepository aiReviewProtoRepository;

  ObjectMapper objectMapper;

  TextUnitSearcher textUnitSearcher;

  AiReviewService aiReviewService;

  TMTextUnitVariantRepository tmTextUnitVariantRepository;

  public AiReviewWS(
      TextUnitSearcher textUnitSearcher,
      AiReviewService aiReviewService,
      AiReviewProtoRepository aiReviewProtoRepository,
      TMTextUnitVariantRepository tmTextUnitVariantRepository,
      @Qualifier("objectMapperReview") ObjectMapper objectMapper) {
    this.textUnitSearcher = textUnitSearcher;
    this.aiReviewService = aiReviewService;
    this.aiReviewProtoRepository = aiReviewProtoRepository;
    this.tmTextUnitVariantRepository = tmTextUnitVariantRepository;
    this.objectMapper = objectMapper;
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/proto-ai-review")
  @ResponseStatus(HttpStatus.OK)
  public ProtoAiReviewResponse aiReview(@RequestBody ProtoAiReviewRequest protoAiReviewRequest) {

    PollableFuture<Void> pollableFuture =
        aiReviewService.aiReviewAsync(
            new AiReviewService.AiReviewInput(
                protoAiReviewRequest.repositoryName(),
                protoAiReviewRequest.targetBcp47tags(),
                protoAiReviewRequest.sourceTextMaxCountPerLocale(),
                protoAiReviewRequest.tmTextUnitIds(),
                protoAiReviewRequest.useBatch(),
                protoAiReviewRequest.useModel(),
                protoAiReviewRequest.runName(),
                protoAiReviewRequest.reviewType()));

    return new ProtoAiReviewResponse(pollableFuture.getPollableTask());
  }

  public record ProtoAiReviewRequest(
      String repositoryName,
      List<String> targetBcp47tags,
      int sourceTextMaxCountPerLocale,
      boolean useBatch,
      String useModel,
      String runName,
      List<Long> tmTextUnitIds,
      boolean allLocales,
      String reviewType) {}

  public record ProtoAiReviewResponse(PollableTask pollableTask) {}

  @RequestMapping(method = RequestMethod.GET, value = "/api/proto-ai-review-single-text-unit")
  @ResponseStatus(HttpStatus.OK)
  public ProtoAiReviewSingleTextUnitResponse getAiReviewForSingleTextUnit(
      ProtoAiReviewSingleTextUnitRequest protoAiReviewSingleTextUnitRequest) {

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setTmTextUnitVariantId(
        protoAiReviewSingleTextUnitRequest.tmTextUnitVariantId);

    List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
    if (search.isEmpty()) {
      throw new RuntimeException("Wrong tmTextUnitVariantId");
    }

    TextUnitDTO textUnit = search.getFirst();

    logger.info("Check for pre-computed review");

    AiReviewProto alreadyReviewed =
        aiReviewProtoRepository.findByTmTextUnitVariantIdAndRunName(
            protoAiReviewSingleTextUnitRequest.tmTextUnitVariantId(), RUN_NAME_FOR_FRONTEND);

    AiReviewTextUnitVariantOutput aiReviewTextUnitVariantOutput = null;

    if (alreadyReviewed != null) {
      try {
        aiReviewTextUnitVariantOutput =
            objectMapper.readValueUnchecked(
                alreadyReviewed.getJsonReview(), AiReviewTextUnitVariantOutput.class);
      } catch (RuntimeException e) {
        logger.warn("Can't deserialize the existing review, we will recompute");
      }
    }

    if (aiReviewTextUnitVariantOutput == null) {

      AiReviewTextUnitVariantInput input =
          new AiReviewTextUnitVariantInput(
              textUnit.getTargetLocale(),
              textUnit.getSource(),
              textUnit.getComment(),
              new AiReviewTextUnitVariantInput.ExistingTarget(
                  textUnit.getTarget(), !textUnit.isIncludedInLocalizedFile()));

      aiReviewTextUnitVariantOutput = aiReviewService.getAiReviewSingleTextUnit(input);

      AiReviewProto aiReviewProto = new AiReviewProto();
      aiReviewProto.setTmTextUnitVariant(
          tmTextUnitVariantRepository.getReferenceById(
              protoAiReviewSingleTextUnitRequest.tmTextUnitVariantId()));
      aiReviewProto.setRunName(RUN_NAME_FOR_FRONTEND);
      aiReviewProto.setJsonReview(
          objectMapper.writeValueAsStringUnchecked(aiReviewTextUnitVariantOutput));
      aiReviewProtoRepository.save(aiReviewProto);
    }

    return new ProtoAiReviewSingleTextUnitResponse(textUnit, aiReviewTextUnitVariantOutput);
  }

  public record ProtoAiReviewSingleTextUnitRequest(long tmTextUnitVariantId) {}

  public record ProtoAiReviewSingleTextUnitResponse(
      TextUnitDTO textUnitDTO, AiReviewTextUnitVariantOutput aiReviewOutput) {}

  public record ProtoAiReviewRetryImportRequest(long childPollableTaskId) {}

  public record ProtoAiReviewRetryImportResponse(long pollableTaskId) {}

  @RequestMapping(method = RequestMethod.POST, value = "/api/proto-ai-review/retry-import")
  @ResponseStatus(HttpStatus.OK)
  public ProtoAiReviewRetryImportResponse aiReviewRetryImport(
      @RequestBody ProtoAiReviewRetryImportRequest protoAiReviewRetryImportRequest) {
    PollableFuture<Void> pollableFuture =
        aiReviewService.retryImport(
            protoAiReviewRetryImportRequest.childPollableTaskId(),
            PollableTask.INJECT_CURRENT_TASK);
    return new ProtoAiReviewRetryImportResponse(pollableFuture.getPollableTask().getId());
  }
}
