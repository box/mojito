package com.box.l10n.mojito.okapi.extractor;

import com.box.l10n.mojito.okapi.steps.AbstractMd5ComputationStep;
import net.sf.okapi.common.Event;

import java.util.ArrayList;
import java.util.List;

class ConvertToExtractionTextUnitsStep extends AbstractMd5ComputationStep {

    List<TextUnit> textUnits;

    @Override
    protected Event handleStartDocument(Event event) {
        textUnits = new ArrayList<>();
        return super.handleStartDocument(event);
    }

    @Override
    public String getName() {
        return "Convert to text units step";
    }

    @Override
    public String getDescription() {
        return "Convert okapi text units to extraction text units";
    }

    @Override
    protected Event handleTextUnit(Event event) {
        Event eventToReturn = super.handleTextUnit(event);
        TextUnit textUnit = new TextUnit();
        textUnit.setName(name);
        textUnit.setSource(source);
        textUnit.setComments(comments);
        textUnits.add(textUnit);
        return eventToReturn;
    }

    public List<TextUnit> getTextUnits() {
        return textUnits;
    }
}
