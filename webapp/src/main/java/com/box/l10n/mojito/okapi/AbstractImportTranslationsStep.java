package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotation;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotations;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class that contains the logic to import XLIFFs.
 *
 * Extending class will define the logic to perform the lookup of the
 * TMTextUnits in which translations will be inserted.
 *
 * @author aloison
 */
@Configurable
public abstract class AbstractImportTranslationsStep extends BasePipelineStep {

    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(AbstractImportTranslationsStep.class);

    @Autowired
    TMService tmService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    TMTextUnitVariantCommentService tmMTextUnitVariantCommentService;

    net.sf.okapi.common.resource.RawDocument rawDocument;

    LocaleId targetLocale;

    /**
     * Optional parameter that can be used to force the import of new
     * translation with a given status. If not used it will use the state form
     * the XLIFF.
     */
    TMTextUnitVariant.Status importWithStatus = null;

    /**
     * Keep track of Text units that are in the XLIFF but not in the database
     * (file corruption).
     *
     * Use String and not Long because the XLIFF
     */
    Set<String> notFoundTextUnitIds;

    /**
     * Indicates if the whole document should be review because there was an
     * issue importing one of the text unit.
     */
    boolean documentReviewNeeded;

    @SuppressWarnings("deprecation")
    @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
    public void setTargetLocale(LocaleId targetLocale) {
        this.targetLocale = targetLocale;
    }

    @StepParameterMapping(parameterType = StepParameterType.INPUT_RAWDOC)
    public void setInputDocument(net.sf.okapi.common.resource.RawDocument rawDocument) {
        this.rawDocument = rawDocument;
    }

    @Override
    protected Event handleStartDocument(Event event) {
        logger.debug("Initialize statistics for the import");
        notFoundTextUnitIds = new HashSet<>();

        return super.handleStartDocument(event);
    }

    @Override
    protected Event handleStartSubDocument(Event event) {
        documentReviewNeeded = false;
        return event;
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {

            TMTextUnit tmTextUnit = getTMTextUnit(textUnit);

            ImportNoteBuilder importNoteBuilder;

            XliffState xliffState = XliffState.NEEDS_TRANSLATION;
            TextContainer target = textUnit.getTarget(targetLocale);

            if (tmTextUnit != null) {
                TMTextUnitVariant importTextUnit = importTextUnit(tmTextUnit.getId(), target);
                importNoteBuilder = buildImportNoteBuilderFromVariant(importTextUnit);
                xliffState = getXliffState(importTextUnit);
            } else {
                logger.debug("Could not find a TMTextUnit for id: {}. Skipping it...", textUnit.getId());
                notFoundTextUnitIds.add(textUnit.getId());
                importNoteBuilder = new ImportNoteBuilder();
                importNoteBuilder.setMustReview(true);
                importNoteBuilder.addError("Text unit for id: " + textUnit.getId() + ", Skipping it...");
                documentReviewNeeded = true;
            }

            setTargetNoteProperty(textUnit, importNoteBuilder.toString());
            setStateProperty(target, xliffState);
        }

        return event;
    }

    /**
     * Sets the note property on the {@link ITextUnit}.
     *
     * @param textUnit will contain the note
     * @param note the note to be added
     */
    void setTargetNoteProperty(ITextUnit textUnit, String note) {
        textUnit.setProperty(new Property(com.box.l10n.mojito.okapi.Property.TARGET_NOTE, note));
    }

    /**
     *
     * @param target
     * @param xliffState
     */
    void setStateProperty(TextContainer target, XliffState xliffState) {
        target.setProperty(new Property(com.box.l10n.mojito.okapi.Property.STATE, xliffState.toString()));
    }

    /**
     * Gets the "state" based on {@link TMTextUnitVariant#isIncludedInLocalizedFile()
     * }
     * and {@link TMTextUnitVariant#getStatus() }.
     *
     * @param tmTextUnitVariant contains information to decide if the target
     * should be reviewed or not return the xliff "state"
     */
    XliffState getXliffState(TMTextUnitVariant tmTextUnitVariant) {

        XliffState xliffState = XliffState.NEEDS_TRANSLATION;

        if (tmTextUnitVariant.isIncludedInLocalizedFile()) {
            if (TMTextUnitVariant.Status.APPROVED.equals(tmTextUnitVariant.getStatus())) {
                xliffState = XliffState.FINAL;
            } else if (tmTextUnitVariant.getStatus().equals(TMTextUnitVariant.Status.REVIEW_NEEDED)) {
                xliffState = XliffState.NEEDS_REVIEW_TRANSLATION;
            }
        }

        return xliffState;
    }

    /**
     * Creates and populate {@link ImportNoteBuilder} from a
     * {@link TMTextUnitVariant}.
     *
     * @param tmTextUnitVariant contains messages that need to be appended to
     * the {@link ImportNoteBuilder} and set the review status.
     * @return the {@link ImportNoteBuilder} containing message from the
     * {@link TMTextUnitVariant}
     */
    private ImportNoteBuilder buildImportNoteBuilderFromVariant(TMTextUnitVariant tmTextUnitVariant) {

        ImportNoteBuilder importNoteBuilder = new ImportNoteBuilder();

        importNoteBuilder.setMustReview(!tmTextUnitVariant.isIncludedInLocalizedFile());

        importNoteBuilder.addInfo("tuv id: " + tmTextUnitVariant.getId());

        boolean needsReview = false;

        for (TMTextUnitVariantComment tmTextUnitVariantComment : tmTextUnitVariant.getTmTextUnitVariantComments()) {
            switch (tmTextUnitVariantComment.getSeverity()) {
                case ERROR:
                    importNoteBuilder.addError(tmTextUnitVariantComment.getContent());
                    needsReview = true;
                case WARNING:
                    importNoteBuilder.addWarning(tmTextUnitVariantComment.getContent());
                    needsReview = true;
                case INFO:
                    importNoteBuilder.addInfo(tmTextUnitVariantComment.getContent());
            }
        }

        //TODO:new status Support more state later, for now just re-implement with enum instead of boolean
        importNoteBuilder.setNeedsReview(needsReview);

        return importNoteBuilder;
    }

    /**
     * Gets the status to be used when adding the translation.
     *
     * First consider errors (if target has some it will always be translation
     * needed), then looks at the step parameter for a specific status to be
     * used and finally uses the text unit state.
     *
     * @param target target that might contain a state property
     * @param hasError if error has been identified in the target
     * @return status to be used for adding a translation
     */
    protected TMTextUnitVariant.Status getStatusForAddingTranslation(TextContainer target, boolean hasError) {

        TMTextUnitVariant.Status status;

        if (hasError) {
            status = TMTextUnitVariant.Status.TRANSLATION_NEEDED;
        } else if (importWithStatus != null) {
            status = importWithStatus;
        } else {
            status = getStatusFromTarget(target);
        }

        return status;
    }

    /**
     * Gets the status to be used for adding a translation given a target and
     * its state.
     *
     * If no supported state is present then default to "review needed"
     *
     * @param target target that might contain a state property
     * @return status to be used for adding a translation
     */
    private TMTextUnitVariant.Status getStatusFromTarget(TextContainer target) {

        TMTextUnitVariant.Status status = TMTextUnitVariant.Status.REVIEW_NEEDED;

        String state = getTargetState(target);

        if (XliffState.TRANSLATED.toString().equals(state)) {
            status = TMTextUnitVariant.Status.REVIEW_NEEDED;
        } else if (XliffState.SIGNED_OFF.toString().equals(state)) {
            status = TMTextUnitVariant.Status.APPROVED;
        }

        return status;
    }

    /**
     * Gets the target state value if exists.
     *
     * @param target target that might contain a state property
     * @return the target state value or null if not the property doesn't exist
     */
    private String getTargetState(TextContainer target) {

        String state = null;

        Property stateProperty = target.getProperty(com.box.l10n.mojito.okapi.Property.STATE);

        if (stateProperty != null) {
            state = stateProperty.getValue();
        }

        return state;
    }

    @Transactional
    TMTextUnitVariant importTextUnit(Long tmTextUnitId, TextContainer target) {

        String bcp47TagLowerCase = targetLocale.toBCP47();
        Long targetLocaleId = localeService.findByBcp47Tag(bcp47TagLowerCase).getId();

        //TODO(P1) check text unit in translation kit translakit and complain? or not ...
        String targetString = target.toString();

        logger.debug("Looking at TMTextUnitVariantCommentAnnotations to decide if translation should be included or needs review (by default included and no review");
        TMTextUnitVariantCommentAnnotations tmTextUnitVariantCommentAnnotations = new TMTextUnitVariantCommentAnnotations(target);

        boolean includedInLocalizedFile = true;

        if (tmTextUnitVariantCommentAnnotations.hasCommentWithWarningSeverity()) {
            logger.debug("translation has comments with warning, document review is needed");
            documentReviewNeeded = true;
        }

        if (tmTextUnitVariantCommentAnnotations.hasCommentWithErrorSeverity()) {
            logger.debug("translation has comments with error, document review is needed and translation is excluded from localized file");
            includedInLocalizedFile = false;
            documentReviewNeeded = true;
        }

        TMTextUnitVariant.Status status = getStatusForAddingTranslation(target, !includedInLocalizedFile);

        logger.debug("Adding TMTextUnitVariant for textUnitId: {}, localeId: {}, status: {}, target: {}", tmTextUnitId, targetLocaleId, status, targetString);
        TMTextUnitVariant addedTMTextUnitVariant;

        if (shouldImportAsCurrentTranslation(tmTextUnitId)) {
            addedTMTextUnitVariant = tmService.addCurrentTMTextUnitVariant(tmTextUnitId, targetLocaleId, targetString, status, includedInLocalizedFile);
        } else {
            addedTMTextUnitVariant = tmService.addTMTextUnitVariant(tmTextUnitId, targetLocaleId, targetString, null, status, includedInLocalizedFile, null);
        }

        logger.debug("Create comments based on the list TMTextUnitVariantCommentAnnotations");
        for (TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation : tmTextUnitVariantCommentAnnotations.getAnnotations()) {

            tmMTextUnitVariantCommentService.addComment(
                    addedTMTextUnitVariant,
                    tmTextUnitVariantCommentAnnotation.getCommentType(),
                    tmTextUnitVariantCommentAnnotation.getSeverity(),
                    tmTextUnitVariantCommentAnnotation.getMessage());
        }

        return addedTMTextUnitVariant;
    }

    @Override
    protected Event handleEndDocument(Event event) {

        ImportTranslationsStepAnnotation importTranslationsStepAnnotation = new ImportTranslationsStepAnnotation();
        rawDocument.setAnnotation(importTranslationsStepAnnotation);

        if (documentReviewNeeded) {
            importTranslationsStepAnnotation.setComment("The document needs review, see import details in the file");
        }

        return super.handleEndDocument(event);
    }

    public TMTextUnitVariant.Status getImportWithStatus() {
        return importWithStatus;
    }

    public void setImportWithStatus(TMTextUnitVariant.Status importWithStatus) {
        this.importWithStatus = importWithStatus;
    }

    /**
     * Gets a {@link TMTextUnit} that matches the information contains in the
     * {@link ITextUnit} to add the translation into.
     *
     * @param textUnit a text unit that needs to be imported
     * @return the TMTextUnit in which the translation will be inserted.
     */
    abstract TMTextUnit getTMTextUnit(ITextUnit textUnit);

    /**
     * Indicates if the translation to be added should be added as as current
     * translation or just as an entry in the translation memory.
     */
    boolean shouldImportAsCurrentTranslation(Long tmTextUnitId) {
        return true;
    }

}
