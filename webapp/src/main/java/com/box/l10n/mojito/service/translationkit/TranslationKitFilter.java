package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.okapi.ImportNoteBuilder;
import com.box.l10n.mojito.okapi.TextUnitDTOAnnotation;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.ImportExportTextUnitUtils;
import com.box.l10n.mojito.okapi.XliffState;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.XLIFFNote;
import net.sf.okapi.common.annotation.XLIFFNoteAnnotation;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * An {@link IFilter} that extracts {@link TextUnit} to build a translation kit.
 *
 * @author jaurambault
 */
@Configurable
public class TranslationKitFilter implements IFilter {

    static Logger logger = LoggerFactory.getLogger(TranslationKitFilter.class);

    public static final String FILTER_NAME = "box_translationkit_filter";

    private static final String DISPLAY_NAME = "Translation Kit Filter";

    @Autowired
    TranslationKitService translationKitService;

    @Autowired
    LocaleService localeService;

    @Autowired
    ImportExportTextUnitUtils importExportTextUnitUtils;

    @Autowired
    TextUnitUtils textUnitUtils;

    /**
     * {@link TranslationKit#id}
     */
    Long translationKitId;

    TranslationKit.Type type;

    Boolean useInheritance = false;

    /**
     * The text units to be returned by the filter
     */
    Iterator<TextUnitDTO> textUnitsIterator;

    /**
     * Tracks when filter is done returning events ({@code true} when the filter
     * is done).
     */
    boolean finished = false;

    /**
     * Stores the next event to be returned by {@link #next() }.
     */
    Event nextEvent = null;

    LocaleId targetLocale;

    Locale locale;

    public TranslationKitFilter(Long translationKitId, TranslationKit.Type type, Boolean useInheritance) {
        this.translationKitId = Preconditions.checkNotNull(translationKitId);
        this.type = Preconditions.checkNotNull(type);
        this.useInheritance = useInheritance;
    }

    @Override
    public String getName() {
        return FILTER_NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public void open(RawDocument input) {
        open(input, false);
    }

    @Override
    public void open(RawDocument input, boolean generateSkeleton) {

        logger.debug("Open raw document: get the textunitDTOs for translationKitId: {}", translationKitId);

        targetLocale = input.getTargetLocale();
        locale = localeService.findByBcp47Tag(targetLocale.toBCP47());

        List<TextUnitDTO> textUnitDTOsForTranslationKit;
        if (useInheritance) {
            textUnitDTOsForTranslationKit = translationKitService.getTextUnitDTOsForTranslationKitWithInheritance(translationKitId);
        } else {
            textUnitDTOsForTranslationKit = translationKitService.getTextUnitDTOsForTranslationKit(translationKitId, type);
        }

        textUnitsIterator = textUnitDTOsForTranslationKit.iterator();

        StartDocument startDoc = new StartDocument("Translation Kit translationKitId: " + translationKitId);

        //TODO(P1) To be reviewed, would have used the "id" attribute but it
        // doesn't work. To read back this value when using the XLIFF filter,
        // it is required to process the startSubDocument event. It is possible
        // this is not used the proper way
        startDoc.setName(String.valueOf(translationKitId));
        startDoc.setEncoding("UTF-8", true);
        startDoc.setLocale(LocaleId.ENGLISH);
        //TODO(P1) Review what multilingual actually means for okapi
        startDoc.setMultilingual(true);

        nextEvent = new Event(EventType.START_DOCUMENT, startDoc);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean hasNext() {
        return !finished && (nextEvent != null || textUnitsIterator.hasNext());
    }

    @Override
    public Event next() {

        //TODO(P1) refactor? have a stack initialized (getTextUnitDTOsForTranslationKit) in the ctor instead of this logic
        Event event;

        if (nextEvent != null) {
            logger.debug("Return existing event, type: {}", nextEvent.getEventType().toString());
            event = nextEvent;
            nextEvent = null;
        } else if (textUnitsIterator.hasNext()) {
            logger.debug("There are TextUnitDTOs available, create a text unit and return the text unit event");
            event = new Event(EventType.TEXT_UNIT, getNextTextUnit());
        } else {
            logger.debug("No more TextUnitDTO, create end document event and return it");
            event = new Event(EventType.END_DOCUMENT);
            finished = true;
        }

        return event;
    }

    /**
     * Gets the next {@link TextUnit} to be returned by the filter by converting
     * the next {@link TextUnitDTO} available.
     *
     * @return the next {@link TextUnit} to be returned by the filter
     * @throws NoSuchElementException if there is no more {@link TextUnitDTO}
     */
    private TextUnit getNextTextUnit() throws NoSuchElementException {

        TextUnitDTO textUnitDTO = textUnitsIterator.next();

        if (logger.isDebugEnabled()) {
            logger.debug("Get next text unit for tuId: {}, name: {}, locale: {}, source: {}",
                    textUnitDTO.getTmTextUnitId(),
                    textUnitDTO.getName(),
                    textUnitDTO.getTargetLocale(),
                    textUnitDTO.getSource());
        }

        TextUnit textUnit = new TextUnit(textUnitDTO.getTmTextUnitId().toString());

        textUnit.setName(textUnitDTO.getName());
        textUnitUtils.replaceSourceString(textUnit, textUnitDTO.getSource());
        textUnit.setAnnotation(new TextUnitDTOAnnotation(textUnitDTO));

        TextContainer target;
        XliffState state;

        if (textUnitDTO.getTarget() != null) {
            logger.debug("Translation for name: {}, needs to be reviewed", textUnitDTO.getName());
            target = new TextContainer(textUnitDTO.getTarget());
            state = TranslationKit.Type.TRANSLATION.equals(type) ? XliffState.NEEDS_TRANSLATION : XliffState.NEEDS_REVIEW_TRANSLATION;

            //TODO(P1) must build the note from the comments, needs service to enrich DTO with comments
            ImportNoteBuilder importNoteBuilder = new ImportNoteBuilder();
            importNoteBuilder.setMustReview(!textUnitDTO.isIncludedInLocalizedFile());

            //TODO(P1):new status Support more state later, for now just re-implement with enum instead of boolean
            importNoteBuilder.setNeedsReview(TMTextUnitVariant.Status.REVIEW_NEEDED.equals(textUnitDTO.getStatus()));

            if (!Strings.isNullOrEmpty(textUnitDTO.getTargetComment())) {
                textUnit.setProperty(new Property(com.box.l10n.mojito.okapi.Property.TARGET_COMMENT, textUnitDTO.getTargetComment()));
            }

            textUnit.setProperty(new Property(com.box.l10n.mojito.okapi.Property.TARGET_NOTE, importNoteBuilder.toString()));
        } else {
            target = new TextContainer(textUnitDTO.getSource());
            state = XliffState.NEW;
        }

        textUnit.setTarget(targetLocale, target);
        target.setProperty(new Property(com.box.l10n.mojito.okapi.Property.STATE, state.toString()));

        if (textUnitDTO.getComment() != null) {
            importExportTextUnitUtils.setNote(textUnit, textUnitDTO.getComment());

            XLIFFNoteAnnotation xliffNoteAnnotation = new XLIFFNoteAnnotation();
            XLIFFNote xliffNote = new XLIFFNote(textUnitDTO.getComment());
            xliffNoteAnnotation.add(xliffNote);
            textUnit.getSource().setAnnotation(xliffNoteAnnotation);
        }

        return textUnit;
    }

    @Override
    public void cancel() {
        nextEvent = new Event(EventType.CANCELED);
    }

    @Override
    public IParameters getParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setParameters(IParameters params) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFilterConfigurationMapper(IFilterConfigurationMapper fcMapper) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ISkeletonWriter createSkeletonWriter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IFilterWriter createFilterWriter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EncoderManager getEncoderManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getMimeType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<FilterConfiguration> getConfigurations() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
