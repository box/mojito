package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author aloison
 */
@Configurable
public class IntegrityCheckStep extends BasePipelineStep {

    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(IntegrityCheckStep.class);

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    IntegrityCheckerFactory integrityCheckerFactory;

    Map<Long, Set<TextUnitIntegrityChecker>> textUnitIntegrityCheckerMap = new HashMap<>();

    private LocaleId targetLocale;
    private RawDocument rawDocument;

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
        return "Integrity Check";
    }

    @Override
    public String getDescription() {
        return "Updates the TM with the extracted new/changed variants."
                + " Expects: raw document. Sends back: original events.";
    }

    @Override
    protected Event handleStartDocument(Event event) {
        logger.debug("Check integrity of document");

        String documentContent = rawDocument.getInputCharSequence().toString();

        // TODO(P1): do not hardcode the type here
        List<DocumentIntegrityChecker> documentIntegrityCheckers = integrityCheckerFactory.getDocumentCheckers("xliff");
        for (DocumentIntegrityChecker checker : documentIntegrityCheckers) {
            checker.check(documentContent);
        }

        return super.handleStartDocument(event);
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {
            TMTextUnit tmTextUnit = null;
            Asset asset = null;

            try {
                Long tmTextUnitId = Long.valueOf(textUnit.getId());
                tmTextUnit = tmTextUnitRepository.findById(tmTextUnitId).orElse(null);
            } catch (NumberFormatException nfe) {
                logger.debug("Could not convert the textUnit id into a Long (TextUnit id)", nfe);
            }

            if (tmTextUnit != null) {
                asset = tmTextUnit.getAsset();
                TextContainer target = textUnit.getTarget(targetLocale);

                logger.debug("Check integrity of text unit");

                try {
                    Set<TextUnitIntegrityChecker> textUnitIntegrityCheckers = getTextUnitIntegrityCheckers(asset);

                    for (TextUnitIntegrityChecker textUnitIntegrityChecker : textUnitIntegrityCheckers) {
                        textUnitIntegrityChecker.check(textUnit.getSource().toString(), target.toString());
                    }
                } catch (IntegrityCheckException e) {
                    TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation = new TMTextUnitVariantCommentAnnotation();
                    tmTextUnitVariantCommentAnnotation.setCommentType(TMTextUnitVariantComment.Type.INTEGRITY_CHECK);
                    tmTextUnitVariantCommentAnnotation.setMessage(e.getMessage());
                    tmTextUnitVariantCommentAnnotation.setSeverity(TMTextUnitVariantComment.Severity.ERROR);
                    new TMTextUnitVariantCommentAnnotations(target).addAnnotation(tmTextUnitVariantCommentAnnotation);
                }
            } else {
                logger.debug("Could not find a TMTextUnit for id: {}. Skipping it...", textUnit.getId());
            }
        }

        return event;
    }

    /**
     * @param asset
     * @return The created or cached TextUnitIntegrityChecker for the given asset
     */
    private Set<TextUnitIntegrityChecker> getTextUnitIntegrityCheckers(Asset asset) {

        Set<TextUnitIntegrityChecker> checkers = textUnitIntegrityCheckerMap.get(asset.getId());
        
        if (checkers == null) {
            checkers = integrityCheckerFactory.getTextUnitCheckers(asset);
            textUnitIntegrityCheckerMap.put(asset.getId(), checkers);
        }

        return checkers;
    }
}