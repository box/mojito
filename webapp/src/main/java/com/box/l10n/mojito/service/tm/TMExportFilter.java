package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.ImportExportTextUnitUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.translationkit.TranslationKitService;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.*;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * An {@link IFilter} to export all {@link TextUnit}s of a {@link TM}
 *
 * @author jaurambault
 */
@Configurable
public class TMExportFilter implements IFilter {

    static Logger logger = LoggerFactory.getLogger(TMExportFilter.class);

    public static final String FILTER_NAME = "box_tmexport_filter";

    private static final String DISPLAY_NAME = "TM Export Filter";

    @Autowired
    TranslationKitService translationKitService;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ImportExportTextUnitUtils importExportTextUnitUtils;

    @Autowired
    TextUnitUtils textUnitUtils;

    /**
     * {@link TM#id} to be exported
     */
    Long assetId;

    /**
     * The text units to be returned by the filter
     */
    Iterator<TextUnitDTOWithComments> textUnitsIterator;

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

    public TMExportFilter(Long tmId) {
        this.assetId = tmId;
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

        logger.debug("Open raw document: get the textunitDTOs to export asset, id: {}", assetId);

        targetLocale = input.getTargetLocale();
        locale = localeService.findByBcp47Tag(targetLocale.toBCP47());

        List<TextUnitDTO> textUnitDTOsForExport = getTextUnitDTOsForExport();
        List<TextUnitDTOWithComments> textUnitDTOWithCommentsForExport = tmTextUnitVariantCommentService.enrichTextUnitDTOsWithComments(textUnitDTOsForExport);
        textUnitsIterator = textUnitDTOWithCommentsForExport.iterator();

        StartDocument startDoc = new StartDocument("Export asset id: " + assetId);

        Asset asset = assetRepository.findOne(assetId);

        startDoc.setName(asset.getPath());
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
     * the next {@link TextUnitDTOWithComments} available.
     *
     * @return the next {@link TextUnit} to be returned by the filter
     * @throws NoSuchElementException if there is no more {@link TextUnitDTOWithComments}
     */
    private TextUnit getNextTextUnit() throws NoSuchElementException {

        TextUnitDTOWithComments textUnitDTO = textUnitsIterator.next();

        if (logger.isDebugEnabled()) {
            logger.debug("Get next text unit for tuId: {}, name: {}, locale: {}, source: {}",
                    textUnitDTO.getTmTextUnitId(),
                    textUnitDTO.getName(),
                    textUnitDTO.getTargetLocale(),
                    textUnitDTO.getSource());
        }

        TextUnit textUnit = new TextUnit("");
        textUnit.setName(textUnitDTO.getName());

        textUnitUtils.replaceSourceString(textUnit, textUnitDTO.getSource());

        TextContainer targetTextContainer = new TextContainer(textUnitDTO.getTarget());
        textUnit.setTarget(targetLocale, targetTextContainer);

        ImportExportNote importExportNote = new ImportExportNote();
        importExportNote.setSourceComment(textUnitDTO.getComment());
        importExportNote.setTargetComment(textUnitDTO.getTargetComment());
        importExportNote.setStatus(textUnitDTO.getStatus());
        importExportNote.setIncludedInLocalizedFile(textUnitDTO.isIncludedInLocalizedFile());
        importExportNote.setCreatedDate(textUnitDTO.getCreatedDate());
        importExportNote.setVariantComments(textUnitDTO.getTmTextUnitVariantComments());
        importExportNote.setPluralForm(textUnitDTO.getPluralForm());
        importExportNote.setPluralFormOther(textUnitDTO.getPluralFormOther());
        
        importExportTextUnitUtils.setImportExportNote(textUnit, importExportNote);
        textUnit.setPreserveWhitespaces(true);
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

    /**
     * Gets the list of {@link TextUnitDTO}s to generate a
     * {@link TranslationKit}
     *
     * @return the list of {@link TextUnitDTO}s to generate a
     * {@link TranslationKit}
     */
    public List<TextUnitDTO> getTextUnitDTOsForExport() {

        logger.debug("Get TextUnitDTOs for export, locale: {}");
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();

        textUnitSearcherParameters.setAssetId(assetId);
        textUnitSearcherParameters.setRootLocaleExcluded(false);
        textUnitSearcherParameters.setLocaleId(locale.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);

        return textUnitSearcher.search(textUnitSearcherParameters);
    }

}
