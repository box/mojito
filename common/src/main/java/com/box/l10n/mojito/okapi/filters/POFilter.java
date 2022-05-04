package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation;
import com.box.l10n.mojito.po.PoPluralRule;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extends {@link net.sf.okapi.filters.po.POFilter} to somehow support gettext
 * plural and surface message context as part of the textunit name.
 * <p>
 * Maps po plural form to cldr using {@link PoPluralRuleHelper
 *
 * @author jaurambualt
 */
@Configurable
public class POFilter extends net.sf.okapi.filters.po.POFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(POFilter.class);

    public static final String FILTER_CONFIG_ID = "okf_po@mojito";

    static final String USAGE_LOCATION_GROUP_NAME = "location";
    static final String USAGE_LOCATION_PATTERN = "#: (?<location>.*)";

    // Having " _" as plural separator wasn't very wise... Android filter for example doesn't have a space: "_".
    // see {@link ThirdPartySyncCommand} too.
    // Consider cleanup but that not that simple as it would require migrating data...
    static final String PLURAL_SEPARATOR = " _";

    @Autowired
    TextUnitUtils textUnitUtils;

    @Autowired
    UnescapeUtils unescapeUtils;

    List<Event> eventQueue = new ArrayList<>();

    LocaleId targetLocale;

    PoPluralRule poPluralRule;

    boolean hasCopyFormsOnImport = false;

    String msgIDPlural;

    String msgID;

    Integer poPluralForm;

    EncoderManager encoderManager;

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
                "PO file with plural handling and text unit name including msgctxt",
                "Configuration for .po files."));

        return list;
    }

    @Override
    public void open(RawDocument input) {
        super.open(input);
        targetLocale = input.getTargetLocale();
        hasCopyFormsOnImport = input.getAnnotation(CopyFormsOnImport.class) != null;
        poPluralRule = PoPluralRule.fromBcp47Tag(targetLocale.toBCP47());

        input.setAnnotation(new RemoveUntranslatedStategyAnnotation(RemoveUntranslatedStrategy.PLACEHOLDER_AND_POST_PROCESSING));
        // Post processing is disable for now, it will be enabled by the TranslsateStep if there are actual text unit to remove
        input.setAnnotation(new OutputDocumentPostProcessingAnnotation(POFilter::removeUntranslated, false));
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

    void readNextEvents() {
        Event next = getNextWithProcess();

        if (isPluralGroupStarting(next)) {
            poPluralForm = 0;
            readPlurals(next);
        } else {
            if (next.isDocumentPart()) {
                rewritePluralFormInHeader(next.getDocumentPart());
            }
            eventQueue.add(next);
        }
    }

    void processTextUnit(Event event) {
        if (event != null && event.isTextUnit()) {
            TextUnit textUnit = (TextUnit) event.getTextUnit();
            renameTextUnitWithSourceAndContent(textUnit);
            unescpae(textUnit);
            addUsagesToTextUnit(textUnit);
        }
    }

    void unescpae(TextUnit textUnit) {
        unescapeSource(textUnit);
        unescapeTarget(textUnit);
    }

    void unescapeSource(TextUnit textUnit) {
        String sourceString = textUnitUtils.getSourceAsString(textUnit);
        String unescapedSourceString = unescapeUtils.replaceEscapedQuotes(sourceString);
        textUnitUtils.replaceSourceString(textUnit, unescapedSourceString);
    }

    void unescapeTarget(TextUnit textUnit) {
        TextContainer target = textUnit.getTarget(targetLocale);
        if (target != null) {
            String targetString = target.toString();
            String unescapedTargetString = unescapeUtils.replaceEscapedQuotes(targetString);
            TextContainer newTarget = new TextContainer(unescapedTargetString);
            textUnit.setTarget(targetLocale, newTarget);
        }
    }

    boolean isPluralGroupStarting(Event event) {
        return event != null && event.isStartGroup() && "x-gettext-plurals".equals(event.getStartGroup().getType());
    }

    boolean isPluralGroupEnding(Event event) {
        return poPluralForm != null && event != null && event.isEndGroup();
    }

    Event getNextWithProcess() {
        Event next = super.next();
        loadMsgIDFromParent();
        processTextUnit(next);
        return next;
    }

    void readPlurals(Event next) {

        logger.debug("First event is the start group, load msgidplural from parent and move to next");
        Set<String> usagesFromSkeleton = getUsagesFromSkeleton(next.getStartGroup().getSkeleton().toString());

        loadMsgIDPluralFromParent();
        eventQueue.add(next);

        List<Event> pluralEvents = new ArrayList<>();
        next = getNextWithProcess();

        // add the start event 
        pluralEvents.add(next);

        poPluralForm++;
        next = getNextWithProcess();

        // read others until the end
        while (next != null && !isPluralGroupEnding(next)) {
            pluralEvents.add(next);
            poPluralForm++;
            next = getNextWithProcess();
        }

        poPluralForm = null;

        // that doesn't contain last
        pluralEvents = adaptPlurals(pluralEvents, usagesFromSkeleton);

        eventQueue.addAll(pluralEvents);

        if (isPluralGroupStarting(next)) {
            poPluralForm = 0;
            readPlurals(next);
        } else {
            eventQueue.add(next);
        }
    }

    List<Event> adaptPlurals(List<Event> pluralEvents, Set<String> usagesFromSkeleton) {
        logger.debug("Adapt plural forms if needed");
        PluralsHolder pluralsHolder = new PoPluralsHolder();
        pluralsHolder.loadEvents(pluralEvents);
        List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);
        setUsagesOnTextUnits(completedForms, usagesFromSkeleton);
        return completedForms;
    }

    private void setUsagesOnTextUnits(List<Event> pluralEvents, Set<String> usagesFromSkeleton) {
        for (Event pluralEvent : pluralEvents) {
            if (pluralEvent.isTextUnit()) {
                setUsagesAnnotationOnTextUnit(usagesFromSkeleton, pluralEvent.getTextUnit());
            }
        }
    }

    private void setUsagesAnnotationOnTextUnit(Set<String> usagesFromSkeleton, ITextUnit textUnit) {
        textUnit.setAnnotation(new UsagesAnnotation((usagesFromSkeleton)));
    }

    class PoPluralsHolder extends PluralsHolder {

        @Override
        public List<Event> getCompletedForms(LocaleId localeId) {

            if (other == null) {
                if (few != null) {
                    logger.debug("Other is not defined but few is, means it is for a language where few can be copied like Russian");
                    other = createCopyOf(few, "few", "other");
                } else if (zero != null) {
                    logger.debug("Other and few are not defined but one is, means it is for a language where few can be copied like Arabic");
                    other = createCopyOf(zero, "zero", "other");
                } else if (two != null) {
                    logger.debug("Other, few and zero are not defined but one is, copy from two.");
                    other = createCopyOf(two, "two", "other");
                }
            }

            return super.getCompletedForms(localeId);
        }

        @Override
        void adaptTextUnitToCLDRForm(ITextUnit textUnit, String cldrPluralForm) {

            if ("one".equals(cldrPluralForm)) {
                // source should always be singular form for "one" form,
                // this is needed for language with 6 entry like arabic
                logger.debug("Set message singular: {}", msgID);
                textUnit.setSource(new TextContainer(unescapeUtils.replaceEscapedQuotes(msgID)));
            } else {
                // source should always be plural form unless for "one" form,
                // this is needed for language with only one entry like
                // japanese: [0] --> other
                logger.debug("Set message plural: {}", msgIDPlural);
                textUnit.setSource(new TextContainer(unescapeUtils.replaceEscapedQuotes(msgIDPlural)));
            }
        }

        @Override
        void replaceFormInSkeleton(GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
            logger.debug("Replace in skeleton form: {} to {} ({})", sourceForm, targetForm, poPluralRule.cldrFormToPoForm(targetForm));

            String cldrFormToGettextForm = poPluralRule.cldrFormToPoForm(targetForm);

            if (cldrFormToGettextForm != null) {
                for (GenericSkeletonPart part : genericSkeleton.getParts()) {
                    StringBuilder sb = part.getData();
                    String str = sb.toString().replaceAll("msgstr\\[\\d\\]", "msgstr[" + cldrFormToGettextForm + "]");
                    sb.replace(0, sb.length(), str);
                }
            } else {
                logger.debug("No replacement, no PO idx for CLDR form: {}", targetForm);
            }
        }

        @Override
        Multimap<String, String> getFormsToCopy(LocaleId localeId) {
            Multimap<String, String> formsToCopy = super.getFormsToCopy(localeId);

            if (hasCopyFormsOnImport) {
                logger.debug("Copy required form for import");
                for (Map.Entry<String, String> entry : poPluralRule.getFormsToCopyOnImport().getFormMap().entries()) {
                    formsToCopy.put(entry.getKey(), entry.getValue());
                }
            }

            return formsToCopy;
        }

        @Override
        void retainForms(LocaleId localeId, List<String> pluralForms) {
            if (localeId != null && !LocaleId.EMPTY.equals(localeId)) {
                Set<String> cldrRules = poPluralRule.getCldrForms();
                pluralForms.retainAll(cldrRules);
            }
        }

    }

    /**
     * If context is present, add it to the text unit name. We keep the
     * generated ID by Okapi for prefix of the text unit name allows to
     * distinguish plural form easily.
     * <p>
     * Note: Decided not to go with empty string id only based the message
     * context to have more consistent IDs and support plural forms. It has a
     * little draw back when searching for string though as it prevents exact
     * match on context.
     *
     * @param textUnit
     */
    void renameTextUnitWithSourceAndContent(ITextUnit textUnit) {

        Property property = textUnit.getProperty(POFilter.PROPERTY_CONTEXT);

        StringBuilder newName = new StringBuilder(msgID);

        if (property != null) {
            newName.append(" --- ").append(property.getValue());
        }

        if (poPluralForm != null) {
            String cldrForm = poPluralRule.poFormToCldrForm(Integer.toString(poPluralForm));
            newName.append(PLURAL_SEPARATOR).append(cldrForm);
        }

        textUnit.setName(newName.toString());
    }

    /**
     * Rewrite the plural forms if processing a target locale.
     *
     * @param documentPart
     */
    void rewritePluralFormInHeader(DocumentPart documentPart) {
        if (targetLocale != null && !LocaleId.EMPTY.equals(targetLocale)) {
            documentPart.setProperty(new Property("pluralforms", poPluralRule.getRule()));
        }
    }

    void loadMsgIDPluralFromParent() {
        Field msgIDPluralParent = ReflectionUtils.findField(net.sf.okapi.filters.po.POFilter.class,
                "msgIDPlural");
        msgIDPlural = makeAccessibleAndGetString(msgIDPluralParent);
    }

    void loadMsgIDFromParent() {
        Field msgIDParent = ReflectionUtils.findField(net.sf.okapi.filters.po.POFilter.class,
                "msgID");
        msgID = makeAccessibleAndGetString(msgIDParent);
    }

    String makeAccessibleAndGetString(Field msgID) {
        ReflectionUtils.makeAccessible(msgID);
        try {
            return (String) msgID.get(this);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    void addUsagesToTextUnit(TextUnit textUnit) {
        Set<String> usageLocationsFromSkeleton = getUsagesFromSkeleton(textUnit.getSkeleton().toString());
        setUsagesAnnotationOnTextUnit(usageLocationsFromSkeleton, textUnit);
    }

    Set<String> getUsagesFromSkeleton(String skeleton) {
        Set<String> locations = new LinkedHashSet<>();

        Pattern pattern = Pattern.compile(USAGE_LOCATION_PATTERN);
        Matcher matcher = pattern.matcher(skeleton);

        while (matcher.find()) {
            locations.add(matcher.group(USAGE_LOCATION_GROUP_NAME));
        }

        return locations;
    }

    @Override
    public EncoderManager getEncoderManager() {
        if (encoderManager == null) {
            encoderManager = new EncoderManager();
            encoderManager.setMapping(MimeTypeMapper.PO_MIME_TYPE, "com.box.l10n.mojito.okapi.filters.POEncoder");
        }
        return encoderManager;
    }

    static String removeUntranslated(String poFile) {
        StringBuilder poFileWithoutUntranslatedPlural = new StringBuilder();
        Scanner scanner = new Scanner(poFile);

        StringBuilder block = new StringBuilder();
        boolean hasMsgId = false;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            String trimedLine = line.trim();
            boolean lineStartWithMsgId = trimedLine.startsWith("msgid ");
            boolean lineHasCommentOrMsgId = lineStartWithMsgId || trimedLine.startsWith("#");

            if (lineHasCommentOrMsgId && hasMsgId) {
                if (block.indexOf(RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER) == -1) {
                    poFileWithoutUntranslatedPlural.append(block);
                }
                block = new StringBuilder(line).append("\n");
                hasMsgId = false;
            } else {
                hasMsgId = hasMsgId || lineStartWithMsgId;
                block.append(line).append("\n");
            }
        }

        if (block.indexOf(RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER) == -1) {
            poFileWithoutUntranslatedPlural.append(block);
        }

        boolean hasBuilderEndReturnLine = poFileWithoutUntranslatedPlural.length() > 0
                && poFileWithoutUntranslatedPlural.charAt(poFileWithoutUntranslatedPlural.length() - 1) == '\n';

        if (!poFile.endsWith("\n") && hasBuilderEndReturnLine) {
            poFileWithoutUntranslatedPlural.deleteCharAt(poFileWithoutUntranslatedPlural.length() - 1 );
        }

        return poFileWithoutUntranslatedPlural.toString();
    }
}
