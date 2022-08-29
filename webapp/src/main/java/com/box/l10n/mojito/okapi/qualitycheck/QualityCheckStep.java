package com.box.l10n.mojito.okapi.qualitycheck;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotation;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotations;
import java.lang.reflect.Field;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.lib.verification.Issue;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;

/** @author aloison */
public class QualityCheckStep extends net.sf.okapi.steps.qualitycheck.QualityCheckStep {

  /** logger */
  static Logger logger = getLogger(QualityCheckStep.class);

  private QualityCheckSession parentSession;
  private LocaleId targetLocale;

  @SuppressWarnings("deprecation")
  @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
  public void setTargetLocale(LocaleId targetLocale) {
    super.setTargetLocale(targetLocale);
    this.targetLocale = targetLocale;
  }

  /** Initializes the QualityCheckSession defined in the parent step, using Reflection */
  public QualityCheckStep() {
    super();

    try {
      parentSession = new QualityCheckSession();

      // replace the "session" with our own version
      Field field = FieldUtils.getDeclaredField(this.getClass().getSuperclass(), "session", true);
      field.set(this, parentSession);
    } catch (IllegalAccessException e) {
      logger.error("Cannot replace the QualityCheckSession with Reflection");
    }
  }

  @Override
  protected Event handleTextUnit(Event event) {
    super.handleTextUnit(event);

    ITextUnit textUnit = event.getTextUnit();

    List<Issue> issues = parentSession.getIssuesForTextUnit(textUnit);
    for (Issue issue : issues) {
      if (isIssueOnSource(issue)) {
        addAnnotationOnSource(textUnit, issue);
      } else {
        addAnnotationOnTarget(textUnit, issue);
      }
    }

    return event;
  }

  /**
   * Returns whether the given issue is on the text unit's source
   *
   * @param issue The issue to be checked
   * @return {@code true} if the issue is on the source container
   */
  private boolean isIssueOnSource(Issue issue) {
    return !(issue.getSourceStart() == 0 && issue.getSourceEnd() == -1);
  }

  /**
   * Adds an issue annotation on the source of the given text unit
   *
   * @param textUnit
   * @param issue
   */
  private void addAnnotationOnSource(ITextUnit textUnit, Issue issue) {
    TextContainer sourceContainer = textUnit.getSource();
    addAnnotation(sourceContainer, issue);
  }

  /**
   * Adds an issue annotation on the target of the given text unit
   *
   * @param textUnit
   * @param issue
   */
  private void addAnnotationOnTarget(ITextUnit textUnit, Issue issue) {
    TextContainer targetContainer = textUnit.getTarget(targetLocale);
    addAnnotation(targetContainer, issue);
  }

  /**
   * Adds an issue annotation on the given text container
   *
   * @param textContainer
   * @param issue
   */
  private void addAnnotation(TextContainer textContainer, Issue issue) {
    TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation =
        new TMTextUnitVariantCommentAnnotation();
    tmTextUnitVariantCommentAnnotation.setCommentType(TMTextUnitVariantComment.Type.QUALITY_CHECK);
    tmTextUnitVariantCommentAnnotation.setMessage(issue.getMessage());
    tmTextUnitVariantCommentAnnotation.setSeverity(TMTextUnitVariantComment.Severity.WARNING);
    new TMTextUnitVariantCommentAnnotations(textContainer)
        .addAnnotation(tmTextUnitVariantCommentAnnotation);
  }
}
