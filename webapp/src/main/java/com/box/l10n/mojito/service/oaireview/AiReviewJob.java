package com.box.l10n.mojito.service.oaireview;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.oaireview.AiReviewService.AiReviewInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiReviewJob extends QuartzPollableJob<AiReviewInput, Void> {

  static Logger logger = LoggerFactory.getLogger(AiReviewJob.class);

  @Autowired AiReviewService aiReviewService;

  @Override
  public Void call(AiReviewInput aiReviewJobInput) throws Exception {
    aiReviewService.aiReview(aiReviewJobInput);
    return null;
  }
}
