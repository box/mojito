package com.box.l10n.mojito.okapi;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.TextContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import com.box.l10n.mojito.pseudoloc.PseudoLocalization;

/**
 *
 * @author srizvi
 */
@Configurable
public class PseudoLocalizeStep extends BasePipelineStep {

    private LocaleId targetLocale;

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
            String pseudoTranslation = pseudoLocalization.convertStringToPseudoLoc(source);
            textUnit.setTarget(targetLocale, new TextContainer(pseudoTranslation));
        }

        return event;
    }
}
