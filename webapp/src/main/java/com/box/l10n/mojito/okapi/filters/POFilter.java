package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.POExtraPluralAnnotation;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends {@link net.sf.okapi.filters.po.POFilter} to somehow support gettext
 * plural and surface message context as part of the textunit name.
 *
 * We create the 3 textunits for the 3 possible forms supported by Gettext.
 *
 * This is far from ideal as many languages need only 2 forms but it allows 3
 * forms for others. Note that 3 forms is not enough for some languages either
 * but that's the maximum supported by Gettext anyway (so no need to create 6
 * textunits say to support arabic).
 *
 * This is somewhat a hacky solution but will provide basic support for plural
 * with gettext. End of the day people shouldn't use that plural support anyway
 * as it is too limited.
 *
 * @author jaurambualt
 */
public class POFilter extends net.sf.okapi.filters.po.POFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(POFilter.class);

    public static final String FILTER_CONFIG_ID = "okf_po@mojito";

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

    /**
     * Event that contains an extra text unit for the 3rd plural form supported
     * by Gettext.
     */
    Event extraPluralEvent = null;
    
    /**
     * Indicates if the target locale requires an extra plural form
     */
    boolean needsExtraPluralForm;

    @Override
    public void open(RawDocument input) {
        super.open(input);
        boolean hasAnnotation = input.getAnnotation(POExtraPluralAnnotation.class) != null;
        needsExtraPluralForm = hasAnnotation && needsExtraPluralForm(input.getTargetLocale());
    }
    
    @Override
    public boolean hasNext() {
        return extraPluralEvent != null || super.hasNext();
    }
 
    /**
     * Return the extraPluralEvent textunit if needed or get the event from the
 parent filter.
     *
     * TextUnits from the parents are modified to include context in the
     * textunit name.
     *
     * @return
     */
    @Override
    public Event next() {
        Event event;
        if (extraPluralEvent != null) {
            event = extraPluralEvent;
            extraPluralEvent = null;
        } else {

            event = super.next();

            if (event.isTextUnit()) {
                ITextUnit textUnit = event.getTextUnit();
                addExtraPluralIfTextUnitIsSecondPluralForm(textUnit);
                addContextToTextUnitName(textUnit);
            }
        }

        return event;
    }
  

    /**
     * Indicates if a locale needs an extra plural form in PO files. 
     * 
     * If no target locale (source file) extra plural is needed as well as for
     * language that requires more than 2 plural forms.
     * 
     * @param localeId from the input document
     * @return if an extra plural text unit is needed or not
     */
    protected boolean needsExtraPluralForm(LocaleId localeId) {
        boolean required = true;
        
        if (localeId != null && !LocaleId.EMPTY.equals(localeId)) {
            ULocale ulocale = ULocale.forLanguageTag(localeId.toBCP47());
            PluralRules pluralRules = PluralRules.forLocale(ulocale);
            required = pluralRules.getKeywords().size() > 2;
        }
        
        return required;
    }

    /**
     * If the text unit correspond to the 2nd gettext entry of a plural, create
     * a clone of the textunit to make it an extra text unit to support the 3
     * gettext plural forms. Create an event with that new textunit to be
     * returned next.
     *
     * @param textUnit
     */
    private void addExtraPluralIfTextUnitIsSecondPluralForm(ITextUnit textUnit) {

        if (needsExtraPluralForm && textUnit.getName().endsWith("-1")) {
            ITextUnit clone = textUnit.clone();
            clone.setName(clone.getName().replace("-1", "-2"));
            GenericSkeleton genericSkeleton = (GenericSkeleton) clone.getSkeleton();
            StringBuilder sb = genericSkeleton.getFirstPart().getData();
            String str = sb.toString().replace("msgstr[1]", "msgstr[2]");
            sb.replace(0, sb.length(), str);

            addContextToTextUnitName(clone);
            extraPluralEvent = new Event(EventType.TEXT_UNIT, clone);
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
    private void addContextToTextUnitName(ITextUnit textUnit) {
        Property property = textUnit.getProperty(POFilter.PROPERTY_CONTEXT);
        textUnit.setName("[" + textUnit.getName() + "] ");
        if (property != null) {
            textUnit.setName(textUnit.getName() + property.getValue());
        }
    }

    private void rewritePluralFormInHeader(ITextUnit textUnit) {
        System.out.println(textUnit.getSkeleton());
        
    }
   
}
