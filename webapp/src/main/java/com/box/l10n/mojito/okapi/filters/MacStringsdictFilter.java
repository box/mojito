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

    // Match single or multiple comments
    private static final String XML_COMMENT_PATTERN = "<!--(?<comment>.*?)-->";
    private static final String XML_COMMENT_GROUP_NAME = "comment";

    // Match variable name
    static final String VARIABLE_NAME_PATTERN = "%#@.*@";

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
        } else if (next.isTextUnit() && (isCommentTextUnit(next.getResource()) || isValueTextUnit(next.getResource()))) {
            eventQueue.add(new Event(EventType.NO_OP));
        } else  {
            eventQueue.add(next);
        }
    }

    protected boolean isCommentTextUnit(IResource resource) {
        return resource.toString().trim().startsWith("<!--");
    }

    protected boolean isValueTextUnit(IResource resource) {
        // TODO: add support for processing the variable name. This event with the variable name is dropped for now.
        return resource.toString().trim().matches(VARIABLE_NAME_PATTERN);
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
            if (comment == null) {
                getNoteAndLocationFromEvents(textUnit.toString());
            }
            textUnit.setProperty(new Property(Property.NOTE, comment));
            addUsagesToTextUnit(textUnit);
        }
    }

    void addUsagesToTextUnit(TextUnit textUnit) {
        textUnit.setAnnotation(new UsagesAnnotation(usages));
    }

    protected void getNoteAndLocationFromEvents(String text) {

        String note = null;

        StringBuilder commentBuilder = new StringBuilder();

        Pattern pattern = Pattern.compile(XML_COMMENT_PATTERN);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String comment = matcher.group(XML_COMMENT_GROUP_NAME).trim();
            if (comment.startsWith("Location: ")) {
                if (usages == null) {
                    usages = new LinkedHashSet<>();
                }
                usages.add(comment.replace("Location: ", ""));
            } else {
                if (commentBuilder.length() > 0) {
                    commentBuilder.append(" ");
                }
                commentBuilder.append(comment);
            }
        }

        // get locations from here?
        if (commentBuilder.length() > 0) {
            note = commentBuilder.toString();
        }
        if (note != null) {
            comment = note;
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
        Pattern p = Pattern.compile("<key>NSStringFormatValueTypeKey</key>\n");
        Matcher matcher = p.matcher(toString);
        boolean found = matcher.find();
        return found;
    }


    //finds end of plural group
    protected boolean isPluralGroupEnding(IResource resource) {
        String toString = resource.getSkeleton().toString();
        Pattern p = Pattern.compile("</dict>");
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

        @Override
        protected void loadEvents(List<Event> pluralEvents) {

            if (!pluralEvents.isEmpty()) {
                Event firstEvent = pluralEvents.get(0);
                firstForm = getPluralFormFromSkeleton(firstEvent.getResource());

            }

            super.loadEvents(pluralEvents);
        }

        String getPluralFormFromSkeleton(IResource resource) {
            String toString = resource.getSkeleton().toString();
            Pattern p = Pattern.compile("<key>(.+?)</key>");
            Matcher matcher = p.matcher(toString);
            String res = null;
            if (matcher.find()) {
                res = matcher.group(1);
            }
            return res;
        }

        @Override
        protected Event createCopyOf(Event event, String sourceForm, String targetForm) {
            logger.debug("Create copy of: {}, source form: {}, target form: {}", event.getTextUnit().getName(), sourceForm, targetForm);
            ITextUnit textUnit = event.getTextUnit().clone();
            renameTextUnit(textUnit, sourceForm, targetForm);
            updateItemFormInSkeleton(textUnit);
            replaceFormInSkeleton((GenericSkeleton) textUnit.getSkeleton(), sourceForm, targetForm);
            Event copyOfOther = new Event(EventType.TEXT_UNIT, textUnit);
            return copyOfOther;
        }

        void updateItemFormInSkeleton(ITextUnit textUnit) {
            boolean ignore = true;
            GenericSkeleton genericSkeleton = (GenericSkeleton) textUnit.getSkeleton();
            for (GenericSkeletonPart genericSkeletonPart : genericSkeleton.getParts()) {
                String partString = genericSkeletonPart.toString();
                Pattern p = Pattern.compile("<key>.+?</key>");
                Matcher matcher = p.matcher(partString);
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

        @Override
        void replaceFormInSkeleton(GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
            for (GenericSkeletonPart part : genericSkeleton.getParts()) {
                StringBuilder sb = part.getData();
                String str = sb.toString().replace("<key>" + sourceForm + "</key>", "<key>" + targetForm + "</key>");
                sb.replace(0, sb.length(), str);
            }
        }

    }
}
