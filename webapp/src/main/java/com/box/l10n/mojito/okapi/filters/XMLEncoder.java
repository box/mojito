package com.box.l10n.mojito.okapi.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.filters.its.Parameters;
import org.apache.commons.lang.StringUtils;

/**
 * This overrides the {@link net.sf.okapi.common.encoder.XMLEncoder} for Android
 * strings.
 *
 * It does not escape supported HTML elements for Android strings unless there
 * are variables within the HTML elements.
 * For example, <b>songs</b> vs. &lt;b>%d songs&lt;/b>
 *
 * Also it overrides the default quotemode setting so the quotes do not get escaped.
 *
 * For detailed information, see to Android specification in
 * http://developer.android.com/guide/topics/resources/string-resource.html,
 *
 * @author jyi
 */
public class XMLEncoder extends net.sf.okapi.common.encoder.XMLEncoder {

    // trying to match variables between html tags, for example, <b>%d</b>, <i>%1$s</i>, <u>%2$s</u>
    Pattern androidVariableWithinHTML = Pattern.compile("(&lt;[b|i|u]&gt;)((.*?)%(([-0+ #]?)[-0+ #]?)((\\d\\$)?)(([\\d\\*]*)(\\.[\\d\\*]*)?)[dioxXucsfeEgGpn](.*?))+(&lt;/[b|i|u]&gt;)");
    Pattern androidHTML = Pattern.compile("(&lt;)(/?)(b|i|u)(&gt;)");
    Pattern unescapedDoubleQuote = Pattern.compile("([^\\\\])(\")");
    Pattern startsWithDoubleQuote = Pattern.compile("(^\")");
    Pattern unescapedSingleQuote = Pattern.compile("([^\\\\])(')");
    Pattern startsWithSingleQuote = Pattern.compile("(^')");

    @Override
    public String encode(String text, EncoderContext context) {
        String encoded = super.encode(text, context);
        if (isAndroidStrings()) {
            encoded = escapeAndroid(encoded);
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

    public String escapeAndroid(String text) {
        boolean enclosedInDoubleQuotes = StringUtils.startsWith(text, "\"") && StringUtils.endsWith(text, "\"");
        if (enclosedInDoubleQuotes) {
            text = text.substring(1, text.length() - 1);
        }

        String replacement;
        if (needsAndroidEscapeHTML(text)) {
            replacement = "$1$2$3>";
        } else {
            replacement = "<$2$3>";
        }
        text = androidHTML.matcher(text).replaceAll(replacement);
        text = text.replaceAll("\n", "\\\\n");
        text = text.replaceAll("\r", "\\\\r");
        text = escapeDoubleQuotes(text);
        if (!enclosedInDoubleQuotes) {
            text = escapeSingleQuotes(text);
        }
        return enclosedInDoubleQuotes ? "\"" + text + "\"" : text;

    }

    private boolean needsAndroidEscapeHTML(String text) {
        Matcher matcher = androidVariableWithinHTML.matcher(text);
        return matcher.find();
    }

    private String escapeDoubleQuotes(String text) {
        String escaped = unescapedDoubleQuote.matcher(text).replaceAll("$1\\\\$2");
        escaped = startsWithDoubleQuote.matcher(escaped).replaceFirst("\\\\$1");
        return escaped;
    }
    
    private String escapeSingleQuotes(String text) {
        String escaped = unescapedSingleQuote.matcher(text).replaceAll("$1\\\\$2");
        escaped = startsWithSingleQuote.matcher(escaped).replaceFirst("\\\\$1");
        return escaped;
    }
}
