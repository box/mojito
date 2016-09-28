package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextContainer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author aloison
 */
@Configurable
public class TranslateStep extends BasePipelineStep {

    static Logger logger = LoggerFactory.getLogger(TranslateStep.class);

    @Autowired
    TMService tmService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    private LocaleId targetLocale;

    @Autowired
    TextUnitUtils textUnitUtils;

    Asset asset;

    InheritanceMode inheritanceMode;

    RepositoryLocale repositoryLocale;

    /**
     * Cache that contains the translations required to translate the asset.
     */
    Map<Long, Map<String, TextUnitDTO>> localeToTextUnitDTOsForLocaleMap = new HashMap<>();

    /**
     * Creates the {@link TranslateStep} for a given asset.
     *
     * @param asset {@link Asset} that will be used to lookup translations
     * @param repositoryLocale used to fetch translations. It can be different
     * from the locale used in the Okapi pipeline ({@link #targetLocale}) in
     * case the file needs to be generated for a tag that is different from 
     * the locale used for translation.
     * @param inheritanceMode
     */
    public TranslateStep(Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode) {
        this.asset = asset;
        this.inheritanceMode = inheritanceMode;
        this.repositoryLocale = repositoryLocale;
    }
        
    @SuppressWarnings("deprecation")
    @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
    public void setTargetLocale(LocaleId targetLocale) {
        this.targetLocale = targetLocale;
    }

    @Override
    public String getName() {
        return "Text units translation";
    }

    @Override
    public String getDescription() {
        return "Populates the target with the translations of the TM."
                + " Expects: raw document. Sends back: original events.";
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {

            String translation = null;

            String name = StringUtils.isEmpty(textUnit.getName()) ? textUnit.getId() : textUnit.getName();
            String source = textUnit.getSource().toString();
            String comments = textUnitUtils.getNote(textUnit);
            String md5 = tmService.computeTMTextUnitMD5(name, source, comments);

            logger.debug("Look for a translation in target locale: {} for text unit with name: {}", targetLocale.toBCP47(), name);
            TextUnitDTO textUnitDTO = getTextUnitDTO(md5, repositoryLocale.getLocale().getId());

            if (textUnitDTO != null) {
                logger.debug("Found translation for target locale");
                translation = textUnitDTO.getTarget();
            } else if (InheritanceMode.USE_PARENT.equals(inheritanceMode)) {
                logger.debug("No translation found for target locale, look for translations in parent locales");
                translation = getTranslationWithInheritance(md5, source);
            }

            if (translation == null && InheritanceMode.REMOVE_UNTRANSLATED.equals(inheritanceMode)) {
                logger.debug("Remove untranslated text unit");
                event = Event.NOOP_EVENT;
            } else {
                logger.debug("Set translation for text unit with name: {}: {}", name, translation);
                textUnit.setTarget(targetLocale, new TextContainer(translation));
            }
        }

        return event;
    }

    /**
     * Look for a translation in parent locale excluding the root locale for a
     * given MD5, if none is found it returns the source.
     *
     * <p>
     * The root locale (repository locale with parent locale null) is excluded
     * for optimization purpose, the translations fetched would be the same as
     * the source, hence returning the source directly is more efficient.
     *
     * @param md5 MD5 to lookup the translation
     * @param source the source to fallback to if there is no translation
     * @return the translation from a parent locale or the source if no
     * translation is found.
     */
    protected String getTranslationWithInheritance(String md5, String source) {

        String translation = null;

        logger.debug("Get Translation with inheritance");
        RepositoryLocale repositoryLocaleForFind = repositoryLocale.getParentLocale();

        while (translation == null && repositoryLocaleForFind != null && repositoryLocaleForFind.getParentLocale() != null) {

            logger.debug("Looking for a translation in locale: {}", repositoryLocaleForFind.getLocale().getBcp47Tag());
            TextUnitDTO textUnitDTO = getTextUnitDTO(md5, repositoryLocaleForFind.getLocale().getId());

            if (textUnitDTO != null) {
                logger.debug("Found a translation using inheritance in locale: {}", repositoryLocaleForFind.getLocale().getBcp47Tag());
                translation = textUnitDTO.getTarget();
            } else {
                logger.debug("No inherited translation in locale: {}", repositoryLocaleForFind.getLocale().getBcp47Tag());
            }

            repositoryLocaleForFind = repositoryLocaleForFind.getParentLocale();
        }

        if (translation == null) {
            logger.debug("No translation using inheritance, fallback to source");
            translation = source;
        }

        return translation;
    }

    /**
     * Gets a {@link TextUnitDTO} from the cache for a given locale and text
     * unit MD5.
     *
     * @param md5 the MD5 of the text unit (see
     * {@link TMService#computeTMTextUnitMD5(java.lang.String, java.lang.String, java.lang.String)})
     * @param localeId the {@link Locale#id}
     * @return a {@link TextUnitDTO} or {@code null} if no translation is
     * available
     */
    private TextUnitDTO getTextUnitDTO(String md5, Long localeId) {
        Map<String, TextUnitDTO> translationsForLocaleMap = getTextUnitDTOsForLocaleMapFromCache(localeId);
        return translationsForLocaleMap.get(md5);
    }

    /**
     * Gets the cached map of {@link TextUnitDTO}s keyed by MD5 that contains
     * all current translations for a given locale.
     *
     * @param localeId the {@link Locale#id}
     * @return the cached map
     */
    private Map<String, TextUnitDTO> getTextUnitDTOsForLocaleMapFromCache(Long localeId) {

        logger.debug("Get TextUnitDTOs for a locale as a map from the cache");
        Map<String, TextUnitDTO> translationsForLocaleMap = localeToTextUnitDTOsForLocaleMap.get(localeId);

        if (translationsForLocaleMap == null) {
            logger.debug("No map in cache, get the map and cache it");
            translationsForLocaleMap = getTextUnitDTOsForLocaleByContentMD5(localeId);
            localeToTextUnitDTOsForLocaleMap.put(localeId, translationsForLocaleMap);
        }

        return translationsForLocaleMap;
    }

    /**
     * Gets the map of {@link TextUnitDTO}s keyed by MD5 that contains all
     * current translations for a given locale.
     *
     * @param localeId the {@link Locale#id}
     * @return the map of {@link TextUnitDTO}s keyed by MD5 that contains all
     * current translations for a given locale
     */
    private Map<String, TextUnitDTO> getTextUnitDTOsForLocaleByContentMD5(Long localeId) {

        Map<String, TextUnitDTO> res = new HashMap<>();

        logger.debug("Prepare TextUnitSearcherParameters to fetch translation for locale: {}", localeId);
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setLocaleId(localeId);
        textUnitSearcherParameters.setAssetId(asset.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED_AND_NOT_REJECTED);

        logger.debug("Getting TextUnitDTOs");
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        logger.debug("Transform TextUnitDTOs list into map keyed by MD5");
        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            String md5 = tmService.computeTMTextUnitMD5(textUnitDTO.getName(), textUnitDTO.getSource(), textUnitDTO.getComment());
            res.put(md5, textUnitDTO);
        }

        return res;
    }

}
