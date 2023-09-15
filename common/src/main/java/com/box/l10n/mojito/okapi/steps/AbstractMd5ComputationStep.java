package com.box.l10n.mojito.okapi.steps;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.filters.ConvertToHtmlCodesAnnotation;
import com.box.l10n.mojito.okapi.filters.DocumentPartPropertyAnnotation;
import com.google.common.base.Strings;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Step to compute md5 from the {@link TextUnit}.
 *
 * @author jyi
 */
@Configurable
public abstract class AbstractMd5ComputationStep extends BasePipelineStep {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(AbstractMd5ComputationStep.class);

  /**
   * when developer does not provide comment, some tools auto-generate comment auto-generated
   * comments should be ignored
   */
  private static final String COMMENT_TO_IGNORE = "No comment provided by engineer";

  @Autowired protected TextUnitUtils textUnitUtils;

  protected String name;
  protected String source;
  protected String comments;
  protected String md5;
  protected ITextUnit textUnit;

  protected DocumentPart documentPart;
  /**
   * if not null, indicates a document part property that can be localized in child steps.
   *
   * <p>It is meant to be processed in {@link #handleDocumentPart(Event)}. The related {@link
   * #documentPart} can be accessed directly in child steps.
   */
  protected DocumentPartPropertyAnnotation documentPartPropertyAnnotation;

  protected boolean shouldConvertToHtmlCodes = false;
  protected RawDocument rawDocument;

  @StepParameterMapping(parameterType = StepParameterType.INPUT_RAWDOC)
  public void setInput(RawDocument rawDocument) {
    this.rawDocument = rawDocument;
  }

  @Override
  protected Event handleStartDocument(Event event) {
    shouldConvertToHtmlCodes =
        rawDocument.getAnnotation(ConvertToHtmlCodesAnnotation.class) != null;

    return super.handleStartDocument(event);
  }

  @Override
  protected Event handleTextUnit(Event event) {
    textUnit = event.getTextUnit();

    if (textUnit.isTranslatable()) {
      name = Strings.isNullOrEmpty(textUnit.getName()) ? textUnit.getId() : textUnit.getName();

      if (!shouldConvertToHtmlCodes) {
        // This is Mojito's original behavior, just get a raw string a let other part of the system
        // deal with codes.
        //
        // Nested document part are not properly handled as the placeholder is written in raw
        // string format (eg. [#$dp1]).
        //
        // It won't be restored later on in the process
        source = textUnitUtils.getSourceAsString(textUnit);
      } else {
        // Newest option: the codes are transformed into HTML markup and are passed to downstream
        // systems.
        //
        // Nested document parts can be process properly restored as long as markup is decoded and
        // text fragments recreated with the code information.
        //
        // This is only applied to new filter for now (e.g. HTML), since it would break backward
        // compatibility if applied to existing filters
        source = textUnitUtils.getSourceAsCodedHtml(textUnit);
      }

      comments = textUnitUtils.getNote(textUnit);
      if (comments != null && comments.contains(COMMENT_TO_IGNORE)) {
        comments = null;
      }
      // In the case of an import, for monolingual document, "source" contains the target and this
      // md5 won't be the md5 that can identify the tm text unit. Only for multilingual document it
      // would be the case.
      md5 = textUnitUtils.computeTextUnitMD5(name, source, comments);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Handle text unit with name: {}\nsource: {}\ncomments: {}\nmd5: {}",
          name,
          source,
          comments,
          md5);
    }

    return event;
  }

  /**
   * {@link DocumentPartPropertyAnnotation} is set on {@link DocumentPart}s that have a property to
   * be localized. The annotation contains information that cannot be inferred directly from the
   * DocumentPart, and that are instead provided by the {@link net.sf.okapi.common.filters.IFilter}
   *
   * <p>Similarly to text units, we compute: name, source, comment and md5 to be used by child
   * steps.
   *
   * @param event event to handle.
   * @return
   */
  @Override
  protected Event handleDocumentPart(Event event) {
    event = super.handleDocumentPart(event);
    documentPart = event.getDocumentPart();
    documentPartPropertyAnnotation =
        documentPart.getAnnotation(DocumentPartPropertyAnnotation.class);
    if (documentPartPropertyAnnotation != null) {
      name = documentPartPropertyAnnotation.getName();
      source = documentPartPropertyAnnotation.getSource();
      comments = documentPartPropertyAnnotation.getComment();
      md5 = textUnitUtils.computeTextUnitMD5(name, source, comments);
    }
    return event;
  }
}
