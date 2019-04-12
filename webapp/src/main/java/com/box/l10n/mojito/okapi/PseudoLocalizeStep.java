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

    private Asset asset;
    private LocaleId targetLocale;
    private Set<TextUnitIntegrityChecker> textUnitIntegrityCheckers = new HashSet<>();

    public PseudoLocalizeStep(Asset asset) {
        this.asset = asset;
    }

    @Autowired
    IntegrityCheckerFactory integrityCheckerFactory;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    PseudoLocalization pseudoLocalization;

    @Autowired
    TextUnitUtils textUnitUtils;

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
    protected Event handleStartDocument(Event event) {
        textUnitIntegrityCheckers = integrityCheckerFactory.getTextUnitCheckers(asset);
        if (textUnitIntegrityCheckers.isEmpty()) {
            logger.debug("There is no integrity checkers for asset id {}", asset.getId());
        } else {
            logger.debug("Found {} integrity checker(s) for asset id {}", textUnitIntegrityCheckers.size(), asset.getId());
        }
        return event;
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {
            String source = textUnitUtils.getSourceAsString(textUnit);
            String pseudoTranslation = pseudoLocalization.convertStringToPseudoLoc(source, textUnitIntegrityCheckers);
            textUnit.setTarget(targetLocale, new TextContainer(pseudoTranslation));
        }

        return event;
    }
}
