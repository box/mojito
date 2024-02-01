package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.filters.RemoveUntranslatedStategyAnnotation;
import com.box.l10n.mojito.okapi.filters.RemoveUntranslatedStrategy;
import com.box.l10n.mojito.okapi.steps.AbstractMd5ComputationStep;
import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author aloison
 */
@Configurable
public class TranslateStep extends AbstractMd5ComputationStep {

  static Logger logger = LoggerFactory.getLogger(TranslateStep.class);

  static final String ANDROID_FIRST_TEXT_UNIT_REGEX = "(?s)(^.*<resources>).*";

  Asset asset;
  RepositoryLocale repositoryLocale;
  TranslatorWithInheritance translatorWithInheritance;
  LocaleId targetLocale;
  Status status;
  InheritanceMode inheritanceMode;
  RawDocument rawDocument;
  boolean rawDocumentProcessingEnabled = false;
  boolean saveUsedTmTextUnitVariantIds = false;

  /**
   * This is the list of ids that have been used to generate the localized file and it can contain
   * duplicates. (for file that contain dupliacted entries)
   */
  List<Long> usedTmTextUnitVariantIds = new ArrayList<>();

  /**
   * Creates the {@link TranslateStep} for a given asset.
   *
   * @param asset {@link Asset} that will be used to lookup translations
   * @param repositoryLocale used to fetch translations. It can be different from the locale used in
   *     the Okapi pipeline ({@link #targetLocale}) in case the file needs to be generated for a tag
   *     that is different from the locale used for translation.
   * @param inheritanceMode
   * @param status
   * @param saveUsedTmTextUnitVariantIds
   */
  public TranslateStep(
      Asset asset,
      RepositoryLocale repositoryLocale,
      InheritanceMode inheritanceMode,
      Status status,
      boolean saveUsedTmTextUnitVariantIds) {
    this.asset = asset;
    this.inheritanceMode = inheritanceMode;
    this.repositoryLocale = repositoryLocale;
    this.saveUsedTmTextUnitVariantIds = saveUsedTmTextUnitVariantIds;

    StatusFilter statusFilter = getStatusFilter(status);

    this.translatorWithInheritance =
        new TranslatorWithInheritance(asset, repositoryLocale, inheritanceMode, statusFilter);
  }

  private StatusFilter getStatusFilter(Status status) {
    StatusFilter statusFilter = StatusFilter.TRANSLATED_AND_NOT_REJECTED;

    switch (status) {
      case ALL:
        statusFilter = StatusFilter.TRANSLATED_AND_NOT_REJECTED;
        break;
      case ACCEPTED:
        statusFilter = StatusFilter.APPROVED_AND_NOT_REJECTED;
        break;
      case ACCEPTED_OR_NEEDS_REVIEW:
        statusFilter = StatusFilter.APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED;
        break;
    }

    return statusFilter;
  }

  @SuppressWarnings("deprecation")
  @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
  public void setTargetLocale(LocaleId targetLocale) {
    this.targetLocale = targetLocale;
  }

  @StepParameterMapping(parameterType = StepParameterType.INPUT_RAWDOC)
  public void setInputDocument(RawDocument rawDocument) {
    this.rawDocument = rawDocument;
  }

  @Override
  public String getName() {
    return "Text units translation";
  }

  @Override
  public String getDescription() {
    return "Populates the target with the translations of the TM."
        + " Expects: raw document. Sends back: original events.";
  }

  @Override
  protected Event handleTextUnit(Event event) {
    event = super.handleTextUnit(event);

    if (textUnit.isTranslatable()) {

      TextUnitDTO textUnitDTO = translatorWithInheritance.getTextUnitDTO(md5);

      if (saveUsedTmTextUnitVariantIds && textUnitDTO != null) {
        usedTmTextUnitVariantIds.add(textUnitDTO.getTmTextUnitVariantId());
      }

      String translation =
          translatorWithInheritance.getTranslationFromTextUnitDTO(textUnitDTO, source);

      if (translation == null && InheritanceMode.REMOVE_UNTRANSLATED.equals(inheritanceMode)) {
        logger.debug("Remove untranslated text unit");
        Event androidEvent = getEventForAndroidFirstTextUnit(textUnit);

        if (androidEvent == null) {
          switch (getRemoveUntranslatedStrategyFromAnnotation()) {
            case NOOP_EVENT:
              event = Event.createNoopEvent();
              break;
            case PLACEHOLDER_AND_POST_PROCESSING:
              logger.debug("Set untranslated placeholder for text unit with name: {}", name);
              textUnit.setTarget(
                  targetLocale,
                  new TextContainer(RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER));
              enableOutputDocumentPostProcessing();
              break;
          }
        }
      } else {

        if (!shouldConvertToHtmlCodes) {
          // This is Mojito's original behavior, just write a raw string as target
          //
          // Nested document part are not properly handled as the placeholder is written in raw
          // string format (eg. [#$dp1]).
          logger.debug(
              "Set translation for text unit with name: {}, translation: {}", name, translation);
          textUnit.setTarget(targetLocale, new TextContainer(translation));
        } else {
          // Newest behavior: the codes are transformed into HTML markup and are passed to
          // downstream
          // systems.
          //
          // Nested document parts can be process properly restored as long as markup is decoded and
          // text fragments recreated with the code information.
          //
          // This is only applied to new filter for now (e.g. HTML), since it would break backward
          // compatibility if applied to existing filters
          TextFragment tf = textUnitUtils.fromCodedHTML(textUnit, translation);
          textUnit.setTarget(targetLocale, new TextContainer(tf));
          logger.debug(
              "Set translation for text unit with name: {}, translation from fragment: {}",
              name,
              tf.toText());
        }
      }
    }

    return event;
  }

  @Override
  protected Event handleDocumentPart(Event event) {
    event = super.handleDocumentPart(event);
    if (documentPartPropertyAnnotation != null) {
      String translation = translatorWithInheritance.getTranslation(source, md5);
      documentPart.setSourceProperty(
          new Property(documentPartPropertyAnnotation.getPropertyKey(), translation));
    }
    return event;
  }

  void enableOutputDocumentPostProcessing() {
    OutputDocumentPostProcessingAnnotation annotation =
        rawDocument.getAnnotation(OutputDocumentPostProcessingAnnotation.class);
    if (annotation != null && !rawDocumentProcessingEnabled) {
      annotation.setEnabled(true);
      rawDocumentProcessingEnabled = true;
    }
  }

  RemoveUntranslatedStrategy getRemoveUntranslatedStrategyFromAnnotation() {
    RemoveUntranslatedStategyAnnotation removeUntranslatedStategyAnnotation =
        rawDocument.getAnnotation(RemoveUntranslatedStategyAnnotation.class);
    return removeUntranslatedStategyAnnotation == null
        ? RemoveUntranslatedStrategy.NOOP_EVENT
        : removeUntranslatedStategyAnnotation.getUntranslatedStrategy();
  }

  Event getEventForAndroidFirstTextUnit(ITextUnit textUnit) {
    Event event = null;
    String skeleton = textUnit.getSkeleton().toString();
    if (skeleton.matches(ANDROID_FIRST_TEXT_UNIT_REGEX)) {
      textUnit.setSkeleton(
          new GenericSkeleton(skeleton.replaceAll(ANDROID_FIRST_TEXT_UNIT_REGEX, "$1")));
      event = new Event(EventType.TEXT_UNIT, textUnit);
    }
    return event;
  }

  public List<Long> getUsedTmTextUnitVariantIds() {
    return usedTmTextUnitVariantIds;
  }
}
