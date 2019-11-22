package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.okapi.filters.FilterOptions;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotation;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotations;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;

import java.util.HashSet;
import java.util.Set;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class that contains the logic to import XLIFFs.
 *
 * Extending class will define the logic to perform the lookup of the
 * TMTextUnits in which translations will be inserted.
 *
 * @author aloison
 */
@Configurable
public abstract class AbstractImportTranslationsStep extends AbstractMd5ComputationStep {

    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(AbstractImportTranslationsStep.class);

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    TMTextUnitVariantCommentService tmMTextUnitVariantCommentService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuditorAwareImpl auditorAwareImpl;

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

    /**
     * Indicates if the document that is processed is multilingual or not
     */
    boolean isMultilingual = false;

    /**
     * Created date used for all the text unit imported
     */
    DateTime createdDate;

    /**
     * Target comment to set during import
     */
    String targetComment = null;

    /**
     * User used for all the text unit imported
     */
    User createdBy = null;

    String dropImporterUsernameOverride = null;

    public void setDropImporterUsernameOverride(String dropImporterUsernameOverride) {
        this.dropImporterUsernameOverride = dropImporterUsernameOverride;
    }

    @SuppressWarnings("deprecation")
    @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
    public void setTargetLocale(LocaleId targetLocale) {
        this.targetLocale = targetLocale;
    }

    @StepParameterMapping(parameterType = StepParameterType.INPUT_RAWDOC)
    public void setInputDocument(net.sf.okapi.common.resource.RawDocument rawDocument) {
        this.rawDocument = rawDocument;
        applyFilterOptions(rawDocument);
    }

    @Override
    protected Event handleStartDocument(Event event) {
        logger.debug("Initialize statistics for the import");
        notFoundTextUnitIds = new HashSet<>();

        StartDocument startDocument = event.getStartDocument();
        isMultilingual = startDocument.isMultilingual();

        createdDate = new DateTime();
        if (dropImporterUsernameOverride == null) {
            createdBy = auditorAwareImpl.getCurrentAuditor();
        } else {
            createdBy = userRepository.findByUsername(dropImporterUsernameOverride);
        }

        return super.handleStartDocument(event);
    }

    /**
     * TODO reusing filter options in a step might be debatable.. but it is handy to pass the target comment without
     * a big change. Doing that for now
     */
    void applyFilterOptions(RawDocument input) {
        FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);

        if (filterOptions != null) {
            filterOptions.getString("targetComment", s -> targetComment = s);
        }

        logger.debug("filter option, target comment: {}", targetComment);
    }

    @Override
    protected Event handleStartSubDocument(Event event) {
        documentReviewNeeded = false;
        return event;
    }

    /**
     * This step can be used to import translation from monolingual and
     * bi-lingual document. The text container that contains the translation to
     * be imported is either the source or target container
     *
     * @return the text container that contains the translation to be imported
     */
    TextContainer getTargetTextContainer() {
        TextContainer tc;

        if (isMultilingual) {
            tc = textUnit.getTarget(targetLocale);
        } else {
            tc = textUnit.getSource();
        }

        return tc;
    }

    /**
     * Indicates if the translation should be imported or not and with what
     * status.
     *
     * If null is returned then the import should be skipped.
     *
     * This is allow to define import strategies when the source and target are
     * the same, etc.
     *
     * @param tmTextUnit in which the target might be added
     * @param target to potentially be imported
     * @return the status to be used for the import or null to skip
     */
    protected TMTextUnitVariant.Status getStatusForImport(TMTextUnit tmTextUnit, TextContainer target) {

        TMTextUnitVariant.Status status;

        if (tmTextUnit == null) {
            status = null;
        } else if (importWithStatus != null) {
            status = importWithStatus;
        } else {
            status = getStatusFromTarget(target);
        }

        return status;
    }

    @Override
    protected Event handleTextUnit(Event event) {
        event = super.handleTextUnit(event);

        if (textUnit.isTranslatable()) {
            TMTextUnit tmTextUnit = getTMTextUnit();

            ImportNoteBuilder importNoteBuilder;

            XliffState xliffState = XliffState.NEEDS_TRANSLATION;
            TextContainer target = getTargetTextContainer();

            if (tmTextUnit == null) {
                logger.debug("Could not find a TMTextUnit for id: {}. Skipping it...", textUnit.getId());
                notFoundTextUnitIds.add(textUnit.getId());
                importNoteBuilder = new ImportNoteBuilder();
                importNoteBuilder.setMustReview(true);
                importNoteBuilder.addError("Text unit for id: " + textUnit.getId() + ", Skipping it...");
                documentReviewNeeded = true;
            } else if (target == null) {
                logger.debug("Missing target container for TMTextUnit with id: {}. Skipping it...", textUnit.getId());
                importNoteBuilder = new ImportNoteBuilder();
                importNoteBuilder.setMustReview(true);
                importNoteBuilder.addError("Target missing for text unit with id: " + textUnit.getId() + ", Skipping it...");
                documentReviewNeeded = true;
            } else {
                TMTextUnitVariant.Status statusForImport = getStatusForImport(tmTextUnit, target);

                if (statusForImport != null) {
                    TMTextUnitVariant importTextUnit = importTextUnit(tmTextUnit, target, statusForImport, createdDate);
                    importNoteBuilder = buildImportNoteBuilderFromVariant(importTextUnit);
                    xliffState = getXliffState(importTextUnit);
                } else {
                    logger.debug("Skip import for TMTextUnit id: {}", textUnit.getId());
                    importNoteBuilder = new ImportNoteBuilder();
                    importNoteBuilder.addInfo("Skip import for TMTextUnit id: " + textUnit.getId());
                }
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
        if (target != null) {
            target.setProperty(new Property(com.box.l10n.mojito.okapi.Property.STATE, xliffState.toString()));
        }
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
                    break;
                case WARNING:
                    importNoteBuilder.addWarning(tmTextUnitVariantComment.getContent());
                    needsReview = true;
                    break;
                case INFO:
                    importNoteBuilder.addInfo(tmTextUnitVariantComment.getContent());
                    break;
            }
        }

        //TODO:new status Support more state later, for now just re-implement with enum instead of boolean
        importNoteBuilder.setNeedsReview(needsReview);

        return importNoteBuilder;
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

        TMTextUnitVariant.Status status = null;

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

    Long getTargetLocaleId() {
        String bcp47TagLowerCase = targetLocale.toBCP47();
        return localeService.findByBcp47Tag(bcp47TagLowerCase).getId();
    }

    @Transactional
    TMTextUnitVariant importTextUnit(TMTextUnit tmTextUnit, TextContainer target, TMTextUnitVariant.Status status, DateTime createdDate) {

        Long targetLocaleId = getTargetLocaleId();
        Long tmTextUnitId = tmTextUnit.getId();

        //TODO(P1) check text unit in translation kit translakit and complain? or not ...
        String targetString = target.toString();

        logger.debug("Looking at TMTextUnitVariantCommentAnnotations to decide if translation should be included or needs review (by default included and no review)");
        TMTextUnitVariantCommentAnnotations tmTextUnitVariantCommentAnnotations = new TMTextUnitVariantCommentAnnotations(target);

        boolean includedInLocalizedFile = true;

        if (tmTextUnitVariantCommentAnnotations.hasCommentWithWarningSeverity()) {
            logger.debug("translation has comments with warning, document review is not needed though");
        }

        if (tmTextUnitVariantCommentAnnotations.hasCommentWithErrorSeverity()) {
            logger.debug("translation has comments with error, document review is needed and translation is excluded from localized file");
            includedInLocalizedFile = false;
            status = TMTextUnitVariant.Status.TRANSLATION_NEEDED;
            documentReviewNeeded = true;
        }

        logger.debug("Adding TMTextUnitVariant for textUnitId: {}, localeId: {}, status: {}, target: {}", tmTextUnitId, targetLocaleId, status, targetString);
        TMTextUnitVariant addedTMTextUnitVariant;

        if (shouldImportAsCurrentTranslation(tmTextUnitId)) {
            addedTMTextUnitVariant = tmService.addTMTextUnitCurrentVariantWithResult(
                    getTMTextUnitCurrentVariant(targetLocaleId, tmTextUnit),
                    tmTextUnit.getTm().getId(),
                    tmTextUnitId,
                    targetLocaleId,
                    targetString,
                    targetComment,
                    status,
                    includedInLocalizedFile,
                    createdDate,
                    createdBy).getTmTextUnitCurrentVariant().getTmTextUnitVariant();
        } else {
            addedTMTextUnitVariant = tmService.addTMTextUnitVariant(tmTextUnitId, targetLocaleId, targetString, null, status, includedInLocalizedFile, createdDate, createdBy);
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
    //TODO probably don't need that anymore since some class don't even use it
    // and fields are now available from Parent abstract class
    abstract TMTextUnit getTMTextUnit();

    /**
     * Indicates if the translation to be added should be added as as current
     * translation or just as an entry in the translation memory.
     */
    boolean shouldImportAsCurrentTranslation(Long tmTextUnitId) {
        return true;
    }

    /**
     * Get the text unit current variant, can be overridden to optimize import.
     *
     * @param localeId
     * @param tmTextUnit
     * @return
     */
    TMTextUnitCurrentVariant getTMTextUnitCurrentVariant(Long localeId, TMTextUnit tmTextUnit) {
        return tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(localeId, tmTextUnit.getId());
    }
}
