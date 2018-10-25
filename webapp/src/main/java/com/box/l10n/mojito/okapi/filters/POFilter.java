package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.CopyFormsOnImport;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.po.PoPluralRule;
import com.google.common.collect.Multimap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
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

/**
 * Extends {@link net.sf.okapi.filters.po.POFilter} to somehow support gettext
 * plural and surface message context as part of the textunit name.
 *
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

    @Autowired
    TextUnitUtils textUnitUtils;

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

    List<Event> eventQueue = new ArrayList<>();

    LocaleId targetLocale;

    PoPluralRule poPluralRule;

    boolean hasCopyFormsOnImport = false;

    String msgIDPlural;

    String msgID;

    Integer poPluralForm;

    @Override
    public void open(RawDocument input) {
        super.open(input);
        targetLocale = input.getTargetLocale();
        hasCopyFormsOnImport = input.getAnnotation(CopyFormsOnImport.class) != null;
        poPluralRule = PoPluralRule.fromBcp47Tag(targetLocale.toBCP47());
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
            addUsagesToTextUnit(textUnit);
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
        for (Event pluralEvent: pluralEvents) {
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
                logger.debug("Other is not defined, means it is for a language where few can be copied like Russian");
                other = createCopyOf(few, "few", "other");
            }

            return super.getCompletedForms(localeId);
        }

        @Override
        void adaptTextUnitToCLDRForm(ITextUnit textUnit, String cldrPluralForm) {

            if (!"one".equals(cldrPluralForm)) {
                // source should always be plural form unless for "one" form, 
                // this is needed for language with only one entry like 
                // japanese: [0] --> other
                logger.debug("Set message plural: {}", msgIDPlural);
                textUnit.setSource(new TextContainer(msgIDPlural));
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
     *
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
            newName.append(" _").append(cldrForm);
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
        ReflectionUtils.makeAccessible(msgIDPluralParent);
        try {
            msgIDPlural = (String) msgIDPluralParent.get(this);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    void loadMsgIDFromParent() {
        Field msgIDParent = ReflectionUtils.findField(net.sf.okapi.filters.po.POFilter.class,
                "msgID");
        ReflectionUtils.makeAccessible(msgIDParent);
        try {
            msgID = (String) msgIDParent.get(this);
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

}
