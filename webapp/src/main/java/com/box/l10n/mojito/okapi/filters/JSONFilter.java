package com.box.l10n.mojito.okapi.filters;

import com.google.common.collect.Lists;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.annotation.XLIFFNote;
import net.sf.okapi.common.annotation.XLIFFNoteAnnotation;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.json.JsonEventBuilder;
import net.sf.okapi.filters.json.Parameters;
import net.sf.okapi.filters.json.parser.JsonKeyTypes;
import net.sf.okapi.filters.json.parser.JsonValueTypes;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jeanaurambault
 */
public class JSONFilter extends net.sf.okapi.filters.json.JSONFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(JSONFilter.class);

    public static final String FILTER_CONFIG_ID = "okf_json@mojito";

    Pattern noteKeyPattern = null;
    Pattern usagesKeyPattern = null;
    XLIFFNoteAnnotation xliffNoteAnnotation;
    UsagesAnnotation usagesAnnotation;
    String currentKeyName;
    String comment = null;

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();

        list.add(new FilterConfiguration(this.getName(),
                "application/json",
                this.getClass().getName(),
                "JSON (JavaScript Object Notation)",
                "Configuration for JSON files"));
        return list;
    }

    @Override
    public void open(RawDocument input) {
        applyFilterOptions(input);
        super.open(input);
    }

    void applyFilterOptions(RawDocument input) {
        FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);
        Parameters parameters = this.getParameters();
        logger.debug("Set default value for the filter");
        parameters.setUseFullKeyPath(true);
        parameters.setUseLeadingSlashOnKeyPath(false);

        logger.debug("Override with filter options");
        if (filterOptions != null) {
            // okapi options
            filterOptions.getBoolean("useFullKeyPath", b -> parameters.setUseFullKeyPath(b));
            filterOptions.getBoolean("extractAllPairs", b -> parameters.setExtractAllPairs(b));
            filterOptions.getString("exceptions", s -> parameters.setExceptions(s));

            // mojito options
            filterOptions.getString("noteKeyPattern", s -> noteKeyPattern = Pattern.compile(s));
            filterOptions.getString("usagesKeyPattern", s -> usagesKeyPattern = Pattern.compile(s));
        }
    }

    @Override
    public void handleComment(String c) {
        comment = c;
        super.handleComment(c);
    }

    @Override
    public void handleKey(String key, JsonValueTypes valueType, JsonKeyTypes keyType) {
        currentKeyName = key;
        super.handleKey(key, valueType, keyType);
    }

    @Override
    public void handleValue(String value, JsonValueTypes valueType) {
        extractNoteIfMatch(value);
        extractUsageIfMatch(value);
        super.handleValue(value, valueType);
        processComment();
    }

    void extractUsageIfMatch(String value) {
        if (usagesKeyPattern != null) {
            Matcher m = usagesKeyPattern.matcher(currentKeyName);

            if (m.matches()) {
                logger.debug("key matches usagesKeyPattern, add the value as usage");

                if (usagesAnnotation == null) {
                    usagesAnnotation = new UsagesAnnotation(new HashSet<>());
                }

                usagesAnnotation.getUsages().add(value);
            }
        }
    }

    void addXliffNote(String value) {
        XLIFFNote xliffNote = new XLIFFNote(value);
        xliffNote.setFrom(currentKeyName);
        xliffNote.setAnnotates(XLIFFNote.Annotates.SOURCE);

        if (xliffNoteAnnotation == null) {
            logger.debug("create the xliff note annotation");
            xliffNoteAnnotation = new XLIFFNoteAnnotation();
        }

        xliffNoteAnnotation.add(xliffNote);
    }

    void extractNoteIfMatch(String value) {
        if (noteKeyPattern != null) {
            Matcher m = noteKeyPattern.matcher(currentKeyName);

            if (m.matches()) {
                logger.debug("key matches noteKeyPattern, add the value as note");
                addXliffNote(value);
            }
        }
    }

    void processComment() {
        if (comment != null) {
            ITextUnit textUnit = getEventTextUnit();
            if (textUnit != null) {
                String xliffNote = comment.replace("//", "").trim();
                addXliffNote(xliffNote);
                textUnit.setAnnotation(xliffNoteAnnotation);
                xliffNoteAnnotation = null;
            }
            comment = null;
        }
    }

    @Override
    public void handleObjectEnd() {

        if (xliffNoteAnnotation != null || usagesAnnotation != null) {

            ITextUnit textUnit = getEventTextUnit();

            if (textUnit != null) {
                if (xliffNoteAnnotation != null) {
                    logger.debug("Set note on text unit with name: {}", textUnit.getName());
                    textUnit.setAnnotation(xliffNoteAnnotation);
                }

                if (usagesAnnotation != null) {
                    logger.debug("Set usages on text unit with name: {}", textUnit.getName());
                    textUnit.setAnnotation(usagesAnnotation);
                }
            } else {
                logger.debug("Annotation but no text unit. Skip them");
            }
        }

        logger.debug("Reset the xliffNoteAnnotation and Usage Annotation");
        xliffNoteAnnotation = null;
        usagesAnnotation = null;

        super.handleObjectEnd();
    }

    private ITextUnit getEventTextUnit() {
        ITextUnit textUnit = null;
        JsonEventBuilder eventBuilder = null;
        List<Event> events = null;

        try {
            eventBuilder = (JsonEventBuilder) FieldUtils.readField(this, "eventBuilder", true);
            events = (List<Event>) FieldUtils.readField(eventBuilder, "filterEvents", true);
        } catch (Exception e) {
            logger.error("Can't get the eventBuilder", eventBuilder);
            throw new RuntimeException(e);
        }

        Optional<Event> event = Lists.reverse(events).stream().filter(Event::isTextUnit).findFirst();

        if (event.isPresent()) {
            textUnit = event.get().getTextUnit();
        }
        return textUnit;
    }
}
