package com.box.l10n.mojito.okapi.qualitycheck;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotation;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotations;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.lib.verification.Issue;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;

/**
 * @author aloison
 */
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

  /**
   * This is a workaround to allow using in-memory files with current Okapi implementation of the
   * QualityCheckStep.
   *
   * <p>In order for the {@link net.sf.okapi.steps.qualitycheck.QualityCheckStep} to work in a
   * non-interactive mode, it needs to be supplied with Filter Events. These come from {@link
   * net.sf.okapi.steps.common.RawDocumentToFilterEventsStep}, which accepts {@link
   * net.sf.okapi.common.resource.RawDocument} representation of a file to parse. Per the
   * RawDocument documentation, it can be constructed from either of:
   *
   * <ul>
   *   <li>URI
   *   <li>CharSequence (String)
   *   <li>InputStream
   * </ul>
   *
   * but it can only be one of those things at once.
   *
   * <p>QualityCheckStep through its {@link QualityCheckSession} eventually calls {@link
   * net.sf.okapi.lib.verification.QualityChecker#processStartDocument}. In the current version of
   * Okapi, this seems to expect the {@link StartDocument#getName} to be non-null, otherwise the
   * following line throws an NPE:
   *
   * <pre>
   *     (new File(sd.getName())).toURI()
   * </pre>
   *
   * Yet, for in-memory RawDocuments (created from CharSequence or Stream) the URI must be null.
   * Therefore, this workaround injects a fake document name instead of null into the {@link
   * StartDocument} resource by calling its public method {@link StartDocument#setName}.
   *
   * @param event event to handle
   */
  @Override
  protected Event handleStartDocument(Event event) {
    StartDocument sd = (StartDocument) event.getResource();
    String name = sd.getName();
    if (name == null) {
      sd.setName(createFakeDocumentName());
    }
    return super.handleStartDocument(event);
  }

  /**
   * Returns a fake document name to be used where the Okapi classes can't work with streams instead
   * of files.
   *
   * @return a fake output document name from stream
   */
  private static String createFakeDocumentName() {
    return "inMemoryDocument-" + UUID.randomUUID();
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
