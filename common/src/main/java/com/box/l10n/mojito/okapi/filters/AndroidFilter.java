package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jaurambault
 */
@Configurable
public class AndroidFilter extends XMLFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AndroidFilter.class);

    public static final String ANDROIDSTRINGS_CONFIG_FILE_NAME = "AndroidStrings_mojito.fprm";

    public static final String FILTER_CONFIG_ID = "okf_xml@mojito-AndroidStrings";

    private static final String OPTION_OLD_ESCAPING = "oldEscaping";

    private static final Pattern PATTERN_PLURAL_START = Pattern.compile("<plurals");
    private static final Pattern PATTERN_PLURAL_END = Pattern.compile("</plurals>");
    private static final Pattern PATTERN_XML_COMMENT = Pattern.compile("<!--(?<comment>.*?)-->");
    private static final Pattern PATTERN_REPLACE_FORM = Pattern.compile("<.*?item.+?quantity.+?\"(.+?)\"");
    private static final Pattern PATTERN_UPDATE_FORM = Pattern.compile("(\\s*<.*?item.+?quantity.+?\".+?\">)");

    private static final String XML_COMMENT_GROUP_NAME = "comment";

    @Autowired
    TextUnitUtils textUnitUtils;

    @Autowired
    UnescapeUtils unescapeUtils;

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

    LocaleId targetLocale;

    boolean hasAnnotation;

    /**
     * To set old escaping, can be removed if the oldEscaping option is removed.
     */
    AndroidXMLEncoder androidXMLEncoder;

    /**
     * Option to enable old escaping for the Android filter.
     */
    boolean oldEscaping = false;

    List<Event> eventQueue = new ArrayList<>();

    @Override
    public void open(RawDocument input) {
        super.open(input);
        targetLocale = input.getTargetLocale();
        hasAnnotation = input.getAnnotation(CopyFormsOnImport.class) != null;
        applyFilterOptions(input);
    }

    void applyFilterOptions(RawDocument input) {
        FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);

        if (filterOptions != null) {
            filterOptions.getBoolean(OPTION_OLD_ESCAPING, b -> {
                oldEscaping = b;
                if (androidXMLEncoder != null) {
                    androidXMLEncoder.oldEscaping = oldEscaping;
                }
            });
        }

        logger.debug("filter option, old escaping: {}", oldEscaping);
    }

    @Override
    public boolean hasNext() {
        return !eventQueue.isEmpty() || super.hasNext();
    }

    @Override
    public Event next() {
        Event event;

        if (eventQueue.isEmpty()) {
            readNextEvents();
        }

        event = eventQueue.remove(0);

        return event;
    }

    private void processTextUnit(Event event) {
        if (event != null && event.isTextUnit()) {

            TextUnit textUnit = (TextUnit) event.getTextUnit();
            String sourceString = textUnitUtils.getSourceAsString(textUnit);

            String unescapedSourceString;

            if (oldEscaping) {
                unescapedSourceString = unescapeUtils.unescape(sourceString);
            } else {
                unescapedSourceString = unescape(sourceString);
            }

            textUnitUtils.replaceSourceString(textUnit, unescapedSourceString);
            extractNoteFromXMLCommentInSkeletonIfNone(textUnit);
        }
    }

    /**
     * Should cover the main cases mention in doc:
     * https://developer.android.com/guide/topics/resources/string-resource#FormattingAndStyling
     *
     * @param sourceString
     * @return
     */
    String unescape(String sourceString) {
        String unescapedSourceString;

        unescapedSourceString = sourceString.trim();

        if (StringUtils.startsWith(unescapedSourceString, "\"") && StringUtils.endsWith(unescapedSourceString, "\"")) {
            unescapedSourceString = unescapedSourceString.substring(1, unescapedSourceString.length() - 1);
        } else {
            unescapedSourceString = unescapeUtils.replaceLineFeedWithSpace(unescapedSourceString);
            unescapedSourceString = unescapeUtils.collapseSpaces(unescapedSourceString).trim();
        }

        unescapedSourceString = unescapeUtils.replaceEscapedLineFeed(unescapedSourceString);
        unescapedSourceString = unescapeUtils.replaceEscapedCarriageReturn(unescapedSourceString);
        unescapedSourceString = unescapeUtils.replaceEscapedCharacters(unescapedSourceString);

        return unescapedSourceString;
    }

    protected boolean isPluralGroupStarting(IResource resource) {
        String toString = resource.getSkeleton().toString();
        Matcher matcher = PATTERN_PLURAL_START.matcher(toString);
        boolean startPlural = matcher.find();
        return startPlural;
    }

    protected boolean isPluralGroupEnding(IResource resource) {
        String toString = resource.getSkeleton().toString();
        Matcher matcher = PATTERN_PLURAL_END.matcher(toString);
        boolean endPlural = matcher.find();
        return endPlural;
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
                textUnitUtils.setNote(textUnit, note);
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

        Matcher matcher = PATTERN_XML_COMMENT.matcher(skeleton);

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

    @Override
    public AndroidXMLEncoder getXMLEncoder() {
        androidXMLEncoder = new AndroidXMLEncoder(oldEscaping);
        return androidXMLEncoder;
    }

    private void readNextEvents() {
        Event next = getNextWithProcess();

        if (next.isTextUnit() && isPluralGroupStarting(next.getResource())) {
            readPlurals(next);
        } else {
            eventQueue.add(next);
        }
    }

    private Event getNextWithProcess() {
        Event next = super.next();
        processTextUnit(next);
        return next;
    }

    private void readPlurals(Event next) {

        List<Event> pluralEvents = new ArrayList<>();

        // add the start event
        pluralEvents.add(next);

        next = getNextWithProcess();

        // read others until the end
        while (next != null && !isPluralGroupEnding(next.getResource())) {
            pluralEvents.add(next);
            next = getNextWithProcess();
        }

        // that doesn't contain last
        pluralEvents = adaptPlurals(pluralEvents);

        eventQueue.addAll(pluralEvents);

        if (isPluralGroupStarting(next.getResource())) {
            readPlurals(next);
        } else {
            eventQueue.add(next);
        }
    }

    protected List<Event> adaptPlurals(List<Event> pluralEvents) {
        logger.debug("Adapt plural forms if needed");
        PluralsHolder pluralsHolder = new AndroidPluralsHolder();
        pluralsHolder.loadEvents(pluralEvents);
        List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);
        return completedForms;
    }

    class AndroidPluralsHolder extends PluralsHolder {

        String firstForm = null;
        String comments = null;

        @Override
        protected void loadEvents(List<Event> pluralEvents) {

            if (!pluralEvents.isEmpty()) {
                Event firstEvent = pluralEvents.get(0);
                firstForm = getPluralFormFromSkeleton(firstEvent.getResource());
                ITextUnit firstTextUnit = firstEvent.getTextUnit();
                comments = textUnitUtils.getNote(firstTextUnit);
            }

            super.loadEvents(pluralEvents);
        }

        @Override
        public List<Event> getCompletedForms(LocaleId localeId) {
            List<Event> completedForms = super.getCompletedForms(localeId);
            swapSkeletonBetweenOldFirstAndNewFirst(firstForm, getPluralFormFromSkeleton(completedForms.get(0).getResource()));

            for (Event newForm : completedForms) {
                if (comments != null) {
                    textUnitUtils.setNote(newForm.getTextUnit(), comments);
                }
            }

            return completedForms;
        }

        @Override
        void replaceFormInSkeleton(GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
            for (GenericSkeletonPart part : genericSkeleton.getParts()) {
                StringBuilder sb = part.getData();
                //TODO make more flexible
                String str = sb.toString().replace(sourceForm + "\"", targetForm + "\"");
                sb.replace(0, sb.length(), str);
            }
        }

        String getPluralFormFromSkeleton(IResource resource) {
            String toString = resource.getSkeleton().toString();
            Matcher matcher = PATTERN_REPLACE_FORM.matcher(toString);
            String res = null;
            if (matcher.find()) {
                res = matcher.group(1);
            }
            return res;
        }

        void updateFormInSkeleton(ITextUnit textUnit) {
            boolean ignore = true;
            GenericSkeleton genericSkeleton = (GenericSkeleton) textUnit.getSkeleton();
            for (GenericSkeletonPart genericSkeletonPart : genericSkeleton.getParts()) {
                String partString = genericSkeletonPart.toString();
                Matcher matcher = PATTERN_UPDATE_FORM.matcher(partString);
                if (matcher.find()) {
                    String match = matcher.group(1);
                    genericSkeletonPart.setData(match);
                    ignore = false;
                }
                if (ignore) {
                    genericSkeletonPart.setData("");
                }
            }
        }
    }
}
