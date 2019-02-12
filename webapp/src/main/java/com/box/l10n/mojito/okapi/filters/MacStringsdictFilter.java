package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.Event;

import net.sf.okapi.common.resource.ITextUnit;


public class MacStringsdictFilter extends MacStringsdictFilterKey {

    @Override
    public Event next() {
        Event event;

        if (eventQueue.isEmpty()) {
            readNextEvents();
        }

        event = eventQueue.remove(0);

        if (event.isTextUnit()) {
            ITextUnit textUnit = event.getTextUnit();
            if (textUnit.getName().endsWith("NSStringLocalizedFormatKey")) {
                textUnit.setIsTranslatable(false);
            }
        }

        return event;
    }
}
