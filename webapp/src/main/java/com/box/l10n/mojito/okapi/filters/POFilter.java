package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.CopyFormsOnImport;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.po.PoPluralRule;
import com.google.common.collect.Multimap;
import java.lang.reflect.Field;
import java.util.ArrayList;
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

    static final Pattern PO_PLURAL_FORM_PATTERN = Pattern.compile("(.*?\\-)(.*?)(])");

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
                "PO file with pluarl handling and text unit name including msgctxt",
                "Configuration for .po files."));

        return list;
    }

    List<Event> eventQueue = new ArrayList<>();

    LocaleId targetLocale;

    PoPluralRule poPluralRule;

    boolean hasCopyFormsOnImport = false;

    boolean isProcessingPlural = false;

    String msgIDPlural;

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
            addContextToTextUnitName(textUnit);
        }
    }

    boolean isPluralGroupStarting(Event event) {
        return event != null && event.isStartGroup() && "x-gettext-plurals".equals(event.getStartGroup().getType());
    }

    boolean isPluralGroupEnding(Event event) {
        return isProcessingPlural && event != null && event.isEndGroup();
    }

    Event getNextWithProcess() {
        Event next = super.next();
        processTextUnit(next);
        return next;
    }

    void readPlurals(Event next) {

        logger.debug("First event is the start group, load msgidplural from parent and move to next");
        loadMsgIDPluralFromParent();
        eventQueue.add(next);
        next = getNextWithProcess();

        List<Event> pluralEvents = new ArrayList<>();

        isProcessingPlural = true;

        // add the start event
        pluralEvents.add(next);

        next = getNextWithProcess();

        // read others until the end
        while (next != null && !isPluralGroupEnding(next)) {
            pluralEvents.add(next);
            next = getNextWithProcess();
        }

        // that doesn't contain last
        pluralEvents = adaptPlurals(pluralEvents);

        eventQueue.addAll(pluralEvents);

        if (isPluralGroupStarting(next)) {
            readPlurals(next);
        } else {
            eventQueue.add(next);
        }
    }

    List<Event> adaptPlurals(List<Event> pluralEvents) {
        logger.debug("Adapt plural forms if needed");
        PluralsHolder pluralsHolder = new PoPluralsHolder();
        pluralsHolder.loadEvents(pluralEvents);
        List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);
        return completedForms;
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

            renameTextUnit(textUnit, cldrPluralForm);

            if (!"one".equals(cldrPluralForm)) {
                // source should always be plural form unless for "one" form, this is need for language with only one entry like japanese: [0] --> other
                logger.debug("Set message plural: {}", msgIDPlural);
                textUnit.setSource(new TextContainer(msgIDPlural));
            }
        }

        @Override
        String getCldrPluralFormOfEvent(Event event) {
            String pluralForm = null;
            String poPluralFormFromName = getPoPluralFormFromTextUnitName(event.getTextUnit().getName());

            if (poPluralFormFromName != null) {
                pluralForm = poPluralRule.poFormToCldrForm(poPluralFormFromName);
            }

            return pluralForm;
        }

        String getPoPluralFormFromTextUnitName(String name) {
            logger.debug("getPoPluralFormFromName: {}", name);

            String poPluralForm = null;
            Matcher matcher = PO_PLURAL_FORM_PATTERN.matcher(name);

            if (matcher.find()) {
                poPluralForm = matcher.group(2);
            }

            return poPluralForm;
        }

        @Override
        String getNewTextUnitName(String name, String targetForm) {

            Matcher matcher = PO_PLURAL_FORM_PATTERN.matcher(name);

            StringBuffer sb = new StringBuffer();

            if (matcher.find()) {
                matcher.appendReplacement(sb, "$1" + targetForm + "$3");
            }

            matcher.appendTail(sb);

            String newName = sb.toString();

            logger.debug("Rename textunit ({}) to: {}. Plural form: {}", name, newName, targetForm);
            return newName;
        }

        @Override
        void replaceFormInSkeleton(GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
            logger.debug("Replace in skeleton form: {} to {} ({})", sourceForm, targetForm, poPluralRule.cldrFormToPoForm(targetForm));

            String cldrFormToGettextForm = poPluralRule.cldrFormToPoForm(targetForm);

            if (cldrFormToGettextForm != null) {
                for (GenericSkeletonPart part : genericSkeleton.getParts()) {
                    StringBuilder sb = part.getData();
                    //TODO make more flexible
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
    void addContextToTextUnitName(ITextUnit textUnit) {
        Property property = textUnit.getProperty(POFilter.PROPERTY_CONTEXT);

        textUnit.setName("[" + textUnit.getName() + "] ");
        if (property != null) {
            textUnit.setName(textUnit.getName() + property.getValue());
        }
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
}
