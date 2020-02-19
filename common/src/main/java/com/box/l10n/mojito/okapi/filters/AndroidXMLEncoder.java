package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.encoder.EncoderContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This overrides the {@link net.sf.okapi.common.encoder.XMLEncoder} for Android
 * strings.
 * <p>
 * It does not escape supported HTML elements for Android strings unless there
 * are variables within the HTML elements.
 * For example, <b>songs</b> vs. &lt;b>%d songs&lt;/b>
 * <p>
 * Also it overrides the default quotemode setting so the quotes do not get escaped.
 * <p>
 * For detailed information, see to Android specification in
 * http://developer.android.com/guide/topics/resources/string-resource.html,
 *
 * @author jyi
 */
@Configurable
public class AndroidXMLEncoder extends net.sf.okapi.common.encoder.XMLEncoder {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AndroidXMLEncoder.class);

    // trying to match variables between html tags, for example, <b>%d</b>, <i>%1$s</i>, <u>%2$s</u>
    private static final Pattern ANDROID_VARIABLE_WITHIN_HTML = Pattern.compile("(&lt;(?:b|i|u|annotation.*?)&gt;)((.*?)%(([-0+ #]?)[-0+ #]?)((\\d\\$)?)(([\\d\\*]*)(\\.[\\d\\*]*)?)[dioxXucsfeEgGpn](.*?))+(&lt;/(?:b|i|u|annotation.*?)&gt;)");
    private static final Pattern ANDROID_HTML = Pattern.compile("(&lt;)(/?)(b|i|u|annotation.*?)(&gt;)");
    private static final Pattern LINE_FEED = Pattern.compile("\n");
    private static final Pattern CARIAGE_RETURN = Pattern.compile("\r");

    /**
     * To enable old escaping
     */
    boolean oldEscaping = false;

    @Autowired
    UnescapeUtils unescapeUtils;

    public AndroidXMLEncoder(boolean oldEscaping) {
        this.oldEscaping = oldEscaping;
    }

    @Override
    public String encode(String text, EncoderContext context) {
        String escaped = super.encode(text, context);

        if (oldEscaping) {
            escaped = escapeAndroidOld(escaped);
        } else {
            escaped = escapeAndroid(escaped);
        }

        return escaped;
    }

    public String escapeAndroid(String text) {
        text = escapeCommon(text);
        text = escapeSingleQuotes(text);
        return text;
    }

    /**
     * in new version, leading and ending double quotes are unescape remove treatment
     *
     * @param text
     * @return
     */
    public String escapeAndroidOld(String text) {

        boolean enclosedInDoubleQuotes = StringUtils.startsWith(text, "\"") && StringUtils.endsWith(text, "\"");

        if (enclosedInDoubleQuotes) {
            text = text.substring(1, text.length() - 1);
        }

        text = escapeCommon(text);

        if (!enclosedInDoubleQuotes) {
            text = escapeSingleQuotes(text);
        }

        return enclosedInDoubleQuotes ? "\"" + text + "\"" : text;
    }


    String escapeCommon(String text) {
        text = escapeDoubleQuotes(text);

        boolean needsAndroidEscapeHTML = needsAndroidEscapeHTML(text);
        Matcher matcher = ANDROID_HTML.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement((needsAndroidEscapeHTML ? matcher.group(1) : "<")
                    + matcher.group(2) + unescapeUtils.replaceEscapedQuotes(matcher.group(3)) + ">"));
        }
        matcher.appendTail(sb);
        text = sb.toString();

        text = escapeLineFeed(text);
        text = escapeCariageReturn(text);

        return text;
    }

    private String escapeCariageReturn(String text) {
        return CARIAGE_RETURN.matcher(text).replaceAll("\\\\r");
    }

    private String escapeLineFeed(String text) {
        return LINE_FEED.matcher(text).replaceAll("\\\\n");
    }

    private boolean needsAndroidEscapeHTML(String text) {
        Matcher matcher = ANDROID_VARIABLE_WITHIN_HTML.matcher(text);
        return matcher.find();
    }

    String escapeDoubleQuotes(String text) {
        return escapeCharacter(text, '"');
    }

    String escapeSingleQuotes(String text) {
        return escapeCharacter(text, '\'');
    }

    String escapeCharacter(String text, char toBeEscaped) {
        StringBuilder escaped = new StringBuilder();

        boolean escapeNext = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == toBeEscaped && escapeNext) {
                escaped.append('\\');
            }
            escaped.append(c);
            escapeNext = c != '\\';
        }

        return escaped.toString();
    }
}
