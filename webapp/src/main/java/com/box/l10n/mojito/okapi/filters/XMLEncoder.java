package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.filters.its.Parameters;
import org.apache.commons.lang.StringUtils;

/**
 * This overrides the {@link net.sf.okapi.common.encoder.XMLEncoder} 
 * to not to escape supported HTML elements for Android strings.
 * Also it overrides the default quotemode setting so the quotes do not get escaped.
 * According to Android specification in http://developer.android.com/guide/topics/resources/string-resource.html,
 * <b>bold</b>, <i>italian</i> and <u>underline</u> should be in localized file as-is.
 * 
 * @author jyi
 */
public class XMLEncoder extends net.sf.okapi.common.encoder.XMLEncoder {
    
    @Override
    public String encode(String text, EncoderContext context) {
        String encoded = super.encode(text, context);
        if (isAndroidStrings()) {
            encoded = escape(encoded);
        }
        return encoded;
    }

    private boolean isAndroidStrings() {
        Parameters params = (Parameters) getParameters();
        if (params != null && params.getURI() != null && StringUtils.endsWith(params.getURI().toString(), XMLFilter.ANDROIDSTRINGS_CONFIG_FILE_NAME)) {
            return true;
        } else {
            return false;
        }
    }
    
    public String escape(String text) {
        boolean enclosedInDoubleQuotes = StringUtils.startsWith(text, "\"") && StringUtils.endsWith(text, "\"");
        if (enclosedInDoubleQuotes) {
            text = text.substring(1, text.length() - 1);
        }
        
        String pattern = "(&lt;)(/?)(b|i|u)(&gt;)";
        String replacement = "<$2$3>";
        text = text.replaceAll(pattern, replacement);
        text = text.replaceAll("\n", "\\\\n");
        text = text.replaceAll("\r", "\\\\r");
        text = escapeDoubleQuotes(text);
        if (!enclosedInDoubleQuotes) {
            text = escapeSingleQuotes(text);
        }
        return enclosedInDoubleQuotes ? "\"" + text + "\"" : text;
        
    }
    
    private String escapeDoubleQuotes(String text) {
        String pattern1 = "([^\\\\])(\")";
        String pattern2 = "(^\")";  
        String escaped = text.replaceAll(pattern1, "$1\\\\$2");
        escaped = escaped.replaceFirst(pattern2, "\\\\$1");
        return escaped;
    }
    
    private String escapeSingleQuotes(String text) {
        String pattern1 = "([^\\\\])(')";
        String pattern2 = "(^')";    
        String escaped = text.replaceAll(pattern1, "$1\\\\$2");
        escaped = escaped.replaceFirst(pattern2, "\\\\$1");
        return escaped;
    }
}
