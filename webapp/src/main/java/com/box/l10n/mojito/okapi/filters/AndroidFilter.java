package com.box.l10n.mojito.okapi.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaurambault
 */
public class AndroidFilter extends XMLFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AndroidFilter.class);

    public static final String ANDROIDSTRINGS_CONFIG_FILE_NAME = "AndroidStrings_mojito.fprm";

    public static final String FILTER_CONFIG_ID = "okf_xml@mojito-AndroidStrings";

    private static final String XML_COMMENT_PATTERN = "<!--(?<comment>.*?)-->";
    private static final String XML_COMMENT_GROUP_NAME = "comment";

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<>();
        list.add(new FilterConfiguration(getName(),
                getMimeType(),
                getClass().getName(),
                "Android Strings",
                "Configuration for Android Strings XML documents.",
                ANDROIDSTRINGS_CONFIG_FILE_NAME));

        return list;
    }

    @Override
    public Event next() {
        Event event = super.next();

        if (event.isTextUnit()) {
            // if source has escaped double-quotes, single-quotes, \r or \n, unescape
            TextUnit textUnit = (TextUnit) event.getTextUnit();
            String sourceString = textUnit.getSource().toString();
            String unescapedSourceString = unescape(sourceString);
            TextContainer source = new TextContainer(unescapedSourceString);
            textUnit.setSource(source);
            extractNoteFromXMLCommentInSkeletonIfNone(textUnit);
        }

        return event;
    }

    /**
     * Extract the note from XML comments only if there is no note on the text
     * unit. In other words if a note was specify via attribute like description
     * for android it won't be overridden by an comments present in the XML
     * file.
     *
     * @param textUnit the text unit for which comments should be extracted
     */
    protected void extractNoteFromXMLCommentInSkeletonIfNone(TextUnit textUnit) {

        String skeleton = textUnit.getSkeleton().toString();

        if (textUnit.getProperty(Property.NOTE) == null) {
            String note = getNoteFromXMLCommentsInSkeleton(skeleton);
            if (note != null) {
                textUnit.setProperty(new Property(Property.NOTE, note));
            }
        }
    }

    /**
     * Gets the note from the XML comments in the skeleton.
     *
     * @param skeleton that may contains comments
     * @return the note or <code>null</code>
     */
    protected String getNoteFromXMLCommentsInSkeleton(String skeleton) {

        String note = null;

        StringBuilder commentBuilder = new StringBuilder();

        Pattern pattern = Pattern.compile(XML_COMMENT_PATTERN);
        Matcher matcher = pattern.matcher(skeleton);

        while (matcher.find()) {
            if (commentBuilder.length() > 0) {
                commentBuilder.append(" ");
            }
            commentBuilder.append(matcher.group(XML_COMMENT_GROUP_NAME).trim());
        }

        if (commentBuilder.length() > 0) {
            note = commentBuilder.toString();
        }

        return note;
    }

    private String unescape(String text) {
        String unescapedText = text.replaceAll("(\\\\)(\"|')", "$2");
        unescapedText = unescapedText.replaceAll("\\\\n", "\n");
        unescapedText = unescapedText.replaceAll("\\\\r", "\r");
        return unescapedText;
    }

    @Override
    public XMLEncoder getXMLEncoder() {
        XMLEncoder xmlEncoder = new XMLEncoder();
        xmlEncoder.setAndroidStrings(true);
        return xmlEncoder;
    }
}
