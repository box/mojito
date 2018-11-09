package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.pseudoloc.PseudoLocalization;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 *
 * @author srizvi
 */
@Configurable
public class PseudoLocalizeStep extends BasePipelineStep {

    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(PseudoLocalizeStep.class);

    private LocaleId targetLocale;
    private Map<Long, Set<TextUnitIntegrityChecker>> textUnitIntegrityCheckerMap = new HashMap<>();

    @Autowired
    IntegrityCheckerFactory integrityCheckerFactory;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    PseudoLocalization pseudoLocalization;

    @SuppressWarnings("deprecation")
    @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
    public void setTargetLocale(LocaleId targetLocale) {
        this.targetLocale = targetLocale;
    }

    @Override
    public String getName() {
        return "Pseudolocalize a string";
    }

    @Override
    public String getDescription() {
        return "Takes a string in english and pseudolocalizes it.";
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {
            String source = textUnit.getSource().toString();
            Set<TextUnitIntegrityChecker> checkers = getTextUnitIntegrityCheckers(textUnit.getId());
            String pseudoTranslation = pseudoLocalization.convertStringToPseudoLoc(source, checkers);
            textUnit.setTarget(targetLocale, new TextContainer(pseudoTranslation));
        }

        return event;
    }

    /**
     * @param textUnitId
     * @return The created or cached TextUnitIntegrityChecker for the given
     * asset
     */
    private Set<TextUnitIntegrityChecker> getTextUnitIntegrityCheckers(String textUnitId) {
        Set<TextUnitIntegrityChecker> checkers = new HashSet<>();
        TMTextUnit tmTextUnit = null;
        Asset asset = null;

        try {
            Long tmTextUnitId = Long.valueOf(textUnitId);
            tmTextUnit = tmTextUnitRepository.findOne(tmTextUnitId);
        } catch (NumberFormatException nfe) {
            logger.debug("Could not convert the textUnit id into a Long (TextUnit id)", nfe);
        }

        if (tmTextUnit != null) {
            asset = tmTextUnit.getAsset();
            checkers = textUnitIntegrityCheckerMap.get(asset.getId());

            if (checkers == null) {
                logger.debug("There is no cached integrity checkers for asset id {}", asset.getId());
                checkers = integrityCheckerFactory.getTextUnitCheckers(asset);
                if (checkers.isEmpty()) {
                    logger.debug("There is no integrity checkers for asset id {}", asset.getId());
                } else {
                    logger.debug("Found {} integrity checker(s) for asset id {}", checkers.size(), asset.getId());
                }
                textUnitIntegrityCheckerMap.put(asset.getId(), checkers);
            }
        }
        return checkers;
    }
}
