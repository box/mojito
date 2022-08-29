package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.regex.RegexFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/** @author jyi */
@Configurable
public class RegexEscapeDoubleQuoteFilter extends RegexFilter {

  @Autowired UnescapeUtils unescapeUtils;

  @Autowired TextUnitUtils textUnitUtils;

  @Override
  public Event next() {
    Event event = super.next();
    if (event.getEventType() == EventType.TEXT_UNIT) {
      // if source has escaped double-quotes, unescape
      TextUnit textUnit = (TextUnit) event.getTextUnit();
      String sourceString = textUnitUtils.getSourceAsString(textUnit);
      String unescapedSourceString = unescapeUtils.unescape(sourceString);
      textUnitUtils.replaceSourceString(textUnit, unescapedSourceString);
    }
    return event;
  }

  @Override
  public EncoderManager getEncoderManager() {
    EncoderManager encoderManager = super.getEncoderManager();
    if (encoderManager == null) {
      encoderManager = new EncoderManager();
    }
    encoderManager.setMapping(getMimeType(), "com.box.l10n.mojito.okapi.filters.SimpleEncoder");
    return encoderManager;
  }
}
