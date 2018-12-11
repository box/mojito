package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.CopyFormsOnImport;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import net.sf.okapi.common.*;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.*;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author emagalindan
 */
@Configurable
public class MacStringsdictFilter extends XMLFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(MacStringsdictFilter.class);

    public static final String FILTER_CONFIG_ID = "okf_xml@mojito";
    public static final String MAC_STRINGSDICT_CONFIG_FILE_NAME = "macStringsdict_mojito.fprm";

    // Match single or multi-line comments
    private static final String XML_COMMENT_PATTERN = "\\s*?<!--(?<comment>(.*?\\s)*?)-->";
    private static final String XML_COMMENT_GROUP_NAME = "comment";

    LocaleId targetLocale;

    List<Event> eventQueue = new ArrayList<>();

    @Autowired
    TextUnitUtils textUnitUtils;

    boolean hasAnnotation;

    String comment;
    Set<String> usages;

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    /**
     * Overriding to include only mac stringsdict, resx, xtb and AndroidStrings filters
     *
     * @return
     */
    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<>();
        list.add(new FilterConfiguration(getName() + "-stringsdict",
                getMimeType(),
                getClass().getName(),
                "Apple Stringsdict",
                "Configuration for Apple Stringsdict files.",
                MAC_STRINGSDICT_CONFIG_FILE_NAME,
                ".stringsdict;"));
        return list;
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

    @Override
    public void open(RawDocument input) {
        super.open(input);
        targetLocale = input.getTargetLocale();
        logger.debug("target locale: ", targetLocale);
        hasAnnotation = input.getAnnotation(CopyFormsOnImport.class) != null;
    }

    private void readNextEvents() {
        Event next = getNextWithProcess();

        if (next.isTextUnit() && isPluralGroupStarting(next.getResource())) {
            readPlurals(next);
        } else  {
            eventQueue.add(next);
        }
    }

    private String unescape(String text) {
        // unescape double or single quotes
        String unescapedText = text.replaceAll("(\\\\)(\"|')", "$2");
        // unescape \n
        unescapedText = unescapedText.replaceAll("\\\\n", "\n");
        // unescape \r
        unescapedText = unescapedText.replaceAll("\\\\r", "\r");
        return unescapedText;
    }

    private void processTextUnit(Event event) {
        if (event != null && event.isTextUnit()) {
            TextUnit textUnit = (TextUnit) event.getTextUnit();
            String sourceString = textUnit.getSource().toString();
            // if source has escaped double-quotes, single-quotes, \r or \n, unescape
            TextContainer source = new TextContainer(unescape(sourceString));
            textUnit.setSource(source);
            extractNoteFromXMLCommentInSkeletonIfNone(textUnit);
            textUnit.setProperty(new Property(Property.NOTE, comment));
            addUsagesToTextUnit(textUnit);
            textUnit.setAnnotation(new UsagesAnnotation(usages));

        }
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
                comment = note;
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
            String comment = matcher.group(XML_COMMENT_GROUP_NAME).trim();
            if (!comment.startsWith("Location: ")) {
                if (commentBuilder.length() > 0) {
                    commentBuilder.append(" ");
                }
                commentBuilder.append(comment);
            }
        }

        if (commentBuilder.length() > 0) {
            note = commentBuilder.toString();
        }

        return note;
    }

    /**
     * Gets the note from the XML comments in the skeleton.
     *
     * @param skeleton that may contains comments
     * @return the note or <code>null</code>
     */
    protected Set<String> getLocationsFromXMLCommentsInSkeleton(String skeleton) {

        Set<String> locations = new LinkedHashSet<>();

        Pattern pattern = Pattern.compile(XML_COMMENT_PATTERN);
        Matcher matcher = pattern.matcher(skeleton);

        while (matcher.find()) {
            String comment = matcher.group(XML_COMMENT_GROUP_NAME).trim();
            if (comment.startsWith("Location: ")) {
                locations.add(comment.replace("Location: ", ""));
            }
        }

        return locations;
    }

    void addUsagesToTextUnit(TextUnit textUnit) {
        String skeleton = textUnit.getSkeleton().toString();
        Set<String> usagesFromSkeleton = getLocationsFromXMLCommentsInSkeleton(skeleton);
        if (usagesFromSkeleton.size() > 0) {
            usages = usagesFromSkeleton;
        }
    }

    private Event getNextWithProcess() {
        Event next = super.next();
        processTextUnit(next);
        return next;
    }

    private void readPlurals(Event next) {

        List<Event> pluralEvents = new ArrayList<>();

        // read others until the end
        do {
            pluralEvents.add(next);
            next = getNextWithProcess();
        } while (next != null && !isPluralGroupEnding(next.getResource()));

        // that doesn't contain last
        pluralEvents = adaptPlurals(pluralEvents);

        eventQueue.addAll(pluralEvents);


        if (isPluralGroupStarting(next.getResource())) {
            readPlurals(next);
        } else {
            eventQueue.add(next);
        }
    }

    // finds start of plural group
    protected boolean isPluralGroupStarting(IResource resource) {
        String toString = resource.getSkeleton().toString();
        Pattern p = Pattern.compile("<key>NSStringFormatSpecTypeKey</key>");
        Matcher matcher = p.matcher(toString);
        boolean found = matcher.find();
        return found;
    }


    //finds end of plural group
    protected boolean isPluralGroupEnding(IResource resource) {
        String toString = resource.getSkeleton().toString();
        Pattern p = Pattern.compile("</dict>\n</dict>");
        Matcher matcher = p.matcher(toString);
        return matcher.find();
    }

    protected List<Event> adaptPlurals(List<Event> pluralEvents) {
        logger.debug("Adapt plural forms if needed");
        PluralsHolder pluralsHolder = new MacStringsdictPluralsHolder();
        pluralsHolder.loadEvents(pluralEvents); // make sure get proper number
        logger.debug("target locale: ", targetLocale);
        List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);
        return completedForms;
    }

    class MacStringsdictPluralsHolder extends PluralsHolder {

        String firstForm = null;
        String comments = null;

        @Override
        protected void loadEvents(List<Event> pluralEvents) {

            if (!pluralEvents.isEmpty()) {
                Event firstEvent = pluralEvents.get(0);
                firstForm = getPluralFormFromSkeleton(firstEvent.getResource());
            }

            super.loadEvents(pluralEvents);
        }

        @Override
        public List<Event> getCompletedForms(LocaleId localeId) {
            List<Event> completedForms = super.getCompletedForms(localeId);
            swapSkeletonBetweenOldFirstAndNewFirst(firstForm, getPluralFormFromSkeleton(completedForms.get(0).getResource()));

            for (Event newForm : completedForms) {
                if (comments != null) {
                    newForm.getTextUnit().setProperty(new Property(Property.NOTE, comments));
                }
            }

            return completedForms;
        }

        String getPluralFormFromSkeleton(IResource resource) {
            String toString = resource.getSkeleton().toString();
            Pattern p = Pattern.compile("<key>(?<res>.+?)</key>");
            Matcher matcher = p.matcher(toString);
            String res = null;
            while (matcher.find()) {
                res = matcher.group("res").trim();
            }
            return res;
        }

        @Override
        void updateItemFormInSkeleton(ITextUnit textUnit) {
            boolean ignore = true;
            GenericSkeleton genericSkeleton = (GenericSkeleton) textUnit.getSkeleton();
            for (GenericSkeletonPart genericSkeletonPart : genericSkeleton.getParts()) {
                String partString = genericSkeletonPart.toString();
                Pattern p = Pattern.compile("\n\\s*?<key>.+?$");
                Matcher matcher = p.matcher(partString);
                if (matcher.find()) {
                    String match = matcher.group();
                    genericSkeletonPart.setData(match);
                    ignore = false;
                }
                if (ignore) {
                    genericSkeletonPart.setData("");
                }
            }
        }

        @Override
        void replaceFormInSkeleton(GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
            for (GenericSkeletonPart part : genericSkeleton.getParts()) {
                StringBuilder sb = part.getData();
                String str = sb.toString().replace("<key>" + sourceForm, "<key>" + targetForm);
                sb.replace(0, sb.length(), str);
            }
        }

    }
}
