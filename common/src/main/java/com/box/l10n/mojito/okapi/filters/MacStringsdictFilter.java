package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.ITextUnit;

public class MacStringsdictFilter extends MacStringsdictFilterKey {

  public static final String FILTER_CONFIG_ID = "okf_macStringdict@mojito";

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

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
