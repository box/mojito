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
public abstract class PseudoLocalizeStep extends BasePipelineStep {

    protected String source;
    protected ITextUnit textUnit;

    private LocaleId targetLocale;

    @SuppressWarnings("deprecation")
    @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
    public void setTargetLocale(LocaleId targetLocale) {
        this.targetLocale = targetLocale;
    }

    @Override
    protected Event handleTextUnit(Event event) {
        textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {
            PseudoLocalization pseudoLoc = new PseudoLocalization();

            source = textUnit.getSource().toString();
            String pseudoTranslation = pseudoLoc.convertStringToPseudoLoc(source);
            textUnit.setTarget(targetLocale, new TextContainer(pseudoTranslation));
        }

        return event;
    }
}
