package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.AiReviewProto;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.oaireview.AiReviewService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.tm.AiReviewProtoRepository;
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

  private final AiReviewProtoRepository aiReviewProtoRepository;
  private final ObjectMapper objectMapper;

  TextUnitSearcher textUnitSearcher;

  AiReviewService aiReviewService;

  AiReviewProtoRepository AiReviewProtoRepository;

  public AiReviewWS(
      TextUnitSearcher textUnitSearcher,
      AiReviewService aiReviewService,
      AiReviewProtoRepository AiReviewProtoRepository,
      AiReviewProtoRepository aiReviewProtoRepository,
      @Qualifier("AiTranslate") ObjectMapper objectMapper) {
    this.textUnitSearcher = textUnitSearcher;
    this.aiReviewService = aiReviewService;
    this.AiReviewProtoRepository = AiReviewProtoRepository;
    this.aiReviewProtoRepository = aiReviewProtoRepository;
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
                protoAiReviewRequest.useModel()));

    return new ProtoAiReviewResponse(pollableFuture.getPollableTask());
  }

  public record ProtoAiReviewRequest(
      String repositoryName,
      List<String> targetBcp47tags,
      int sourceTextMaxCountPerLocale,
      boolean useBatch,
      String useModel,
      List<Long> tmTextUnitIds,
      boolean allLocales) {}

  public record ProtoAiReviewResponse(PollableTask pollableTask) {}

  @RequestMapping(method = RequestMethod.GET, value = "/api/proto-ai-review-single-text-unit")
  @ResponseStatus(HttpStatus.OK)
  public ProtoAiReviewSingleTextUnitResponse getTextUnitsWithGet(
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
        aiReviewProtoRepository.findByTmTextUnitVariantId(
            protoAiReviewSingleTextUnitRequest.tmTextUnitVariantId());

    AiReviewService.AiReviewSingleTextUnitOutput aiReviewSingleTextUnitOutput = null;

    if (alreadyReviewed != null) {
      try {
        aiReviewSingleTextUnitOutput =
            objectMapper.readValueUnchecked(
                alreadyReviewed.getJsonReview(),
                AiReviewService.AiReviewSingleTextUnitOutput.class);
      } catch (RuntimeException e) {
        logger.warn("Can't deserialize the existing review, we will recompute");
      }
    }

    if (aiReviewSingleTextUnitOutput == null) {

      AiReviewService.AiReviewSingleTextUnitInput input =
          new AiReviewService.AiReviewSingleTextUnitInput(
              textUnit.getTargetLocale(),
              textUnit.getSource(),
              textUnit.getComment(),
              new AiReviewService.AiReviewSingleTextUnitInput.ExistingTarget(
                  textUnit.getTarget(), !textUnit.isIncludedInLocalizedFile()));

      aiReviewSingleTextUnitOutput = aiReviewService.getAiReviewSingleTextUnit(input);
    }

    return new ProtoAiReviewSingleTextUnitResponse(textUnit, aiReviewSingleTextUnitOutput);
  }

  public record ProtoAiReviewSingleTextUnitRequest(long tmTextUnitVariantId) {}

  public record ProtoAiReviewSingleTextUnitResponse(
      TextUnitDTO textUnitDTO, AiReviewService.AiReviewSingleTextUnitOutput aiReviewOutput) {}
}
