package com.box.l10n.mojito.okapi.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.encoder.EncoderContext;
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
    private static final Pattern ANDROID_VARIABLE_WITHIN_HTML = Pattern.compile("(&lt;[b|i|u]&gt;)((.*?)%(([-0+ #]?)[-0+ #]?)((\\d\\$)?)(([\\d\\*]*)(\\.[\\d\\*]*)?)[dioxXucsfeEgGpn](.*?))+(&lt;/[b|i|u]&gt;)");
    private static final Pattern ANDROID_HTML = Pattern.compile("(&lt;)(/?)(b|i|u)(&gt;)");
    private static final Pattern UNESCAPED_DOUBLE_QUOTE = Pattern.compile("([^\\\\])(\")");
    private static final Pattern START_WITH_DOUBLE_QUOTE = Pattern.compile("(^\")");
    private static final Pattern UNESCAPED_SINGLE_QUOTE = Pattern.compile("([^\\\\])(')");
    private static final Pattern START_WITH_SINGLE_QUOTE = Pattern.compile("(^')");
    
    
    boolean androidStrings = false;

    @Override
    public String encode(String text, EncoderContext context) {
        String encoded = super.encode(text, context);
        if (isAndroidStrings()) {
            encoded = escapeAndroid(encoded);
        }
        return encoded;
    }

    public boolean isAndroidStrings() {
        return androidStrings;
    }

    public void setAndroidStrings(boolean androidStrings) {
        this.androidStrings = androidStrings;
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
        text = ANDROID_HTML.matcher(text).replaceAll(replacement);
        text = text.replace("…", "&#8230;");
        text = text.replaceAll("\n", "\\\\n");
        text = text.replaceAll("\r", "\\\\r");
        text = escapeDoubleQuotes(text);
        if (!enclosedInDoubleQuotes) {
            text = escapeSingleQuotes(text);
        }
        return enclosedInDoubleQuotes ? "\"" + text + "\"" : text;

    }

    private boolean needsAndroidEscapeHTML(String text) {
        Matcher matcher = ANDROID_VARIABLE_WITHIN_HTML.matcher(text);
        return matcher.find();
    }

    private String escapeDoubleQuotes(String text) {
        String escaped = UNESCAPED_DOUBLE_QUOTE.matcher(text).replaceAll("$1\\\\$2");
        escaped = START_WITH_DOUBLE_QUOTE.matcher(escaped).replaceFirst("\\\\$1");
        return escaped;
    }
    
    private String escapeSingleQuotes(String text) {
        String escaped = UNESCAPED_SINGLE_QUOTE.matcher(text).replaceAll("$1\\\\$2");
        escaped = START_WITH_SINGLE_QUOTE.matcher(escaped).replaceFirst("\\\\$1");
        return escaped;
    }
}
