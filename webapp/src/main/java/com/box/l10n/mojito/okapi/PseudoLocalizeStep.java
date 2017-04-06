package com.box.l10n.mojito.okapi;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import com.box.l10n.mojito.pseudoloc.PseudoLocalization;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.TextContainer;

/**
 *
 * @author srizvi
 */
public class PseudoLocalizeStep extends BasePipelineStep {

    protected String source;
    protected ITextUnit textUnit;
    protected PseudoLocalization pseudoloc;

    private LocaleId targetLocale;

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
        textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {
            source = textUnit.getSource().toString();
            String pseudoTranslation = pseudoloc.convertStringToPseudoLoc(source);
            textUnit.setTarget(targetLocale, new TextContainer(pseudoTranslation));
        }

        return event;
    }
}
