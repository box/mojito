package com.box.l10n.mojito.okapi.filters;

import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.regex.RegexFilter;

/**
 * Overrides {@link RegexFilter} to handle escape/unescape special characters
 * 
 * @author jyi
 */
public class MacStringsFilter extends RegexFilter {
    
    public static final String FILTER_CONFIG_ID = "okf_regex@box_webapp";
    
    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }
    
    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();
        list.add(new FilterConfiguration(getName() + "-macStrings",
                getMimeType(),
                getClass().getName(),
                "Text (Mac Strings)",
                "Configuration for Macintosh .strings files.",
                "macStrings_box_webapp.fprm"));
        return list;
    }

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
    
    private String unescape(String text) {
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
        encoderManager.setMapping(getMimeType(), "com.box.l10n.mojito.okapi.filters.MacStringsEncoder");
        return encoderManager;
    }
}
