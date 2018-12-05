package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.regex.RegexFilter;

/**
 *
 * @author jyi
 */
public class RegexEscapeDoubleQuoteFilter extends RegexFilter {

    @Override
    public Event next() {
        Event event = super.next();
        if (event.getEventType() == EventType.TEXT_UNIT) {
            // if source has escaped double-quotes, unescape
            TextUnit textUnit = (TextUnit) event.getTextUnit();
            String sourceString = textUnit.getSource().toString();
            String unescapedSourceString = unescape(sourceString);
            TextContainer source = new TextContainer(unescapedSourceString);
            textUnit.setSource(source);
        }
        return event;
    }

    protected String unescape(String text) {
        String unescapedText = text.replaceAll("(\\\\)(\")", "$2");
        unescapedText = unescapedText.replaceAll("\\\\n", "\n");
        unescapedText = unescapedText.replaceAll("\\\\r", "\r");
        return unescapedText;
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
