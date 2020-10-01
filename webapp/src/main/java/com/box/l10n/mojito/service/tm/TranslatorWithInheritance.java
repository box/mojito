package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jeanaurambault
 */
@Configurable
public class TranslatorWithInheritance {

    static Logger logger = LoggerFactory.getLogger(TranslatorWithInheritance.class);

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

    @Autowired
    TextUnitUtils textUnitUtils;

    @Autowired
    TextUnitDTOsCacheService textUnitDTOsCacheService;

    Asset asset;

    InheritanceMode inheritanceMode;

    RepositoryLocale repositoryLocale;
    /**
     * Cache that contains the translations required to translate the asset.
     */
    Map<Long, Map<String, TextUnitDTO>> localeToTextUnitDTOsForLocaleMap = new HashMap<>();

    private StatusFilter statusFilter;

    public TranslatorWithInheritance(Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode) {
        this(asset, repositoryLocale, inheritanceMode, StatusFilter.TRANSLATED_AND_NOT_REJECTED);
    }

    public TranslatorWithInheritance(Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode, StatusFilter statusFilter) {
        this.asset = asset;
        this.inheritanceMode = inheritanceMode;
        this.repositoryLocale = repositoryLocale;
        this.statusFilter = statusFilter;
    }

    public String getTranslation(
            String source,
            String md5) {

        TextUnitDTO textUnitDTO = getTextUnitDTO(md5);
        return getTranslationFromTextUnitDTO(textUnitDTO, source);
    }

    public String getTranslationFromTextUnitDTO(TextUnitDTO textUnitDTO, String source) {
        String translation = null;

        if (textUnitDTO != null) {
            translation = textUnitDTO.getTarget();
        } else if (InheritanceMode.USE_PARENT.equals(inheritanceMode)) {
            logger.debug("No TextUnitDTO, fallback to source");
            translation = source;
        }

        return translation;
    }

    public TextUnitDTO getTextUnitDTO(String md5) {

        logger.debug("Look for a textUnitDTO in target locale: {} for text unit with md5: {}", repositoryLocale.getLocale().getBcp47Tag(), md5);
        TextUnitDTO textUnitDTO = getTextUnitDTO(md5, repositoryLocale.getLocale().getId());

        if (textUnitDTO != null) {
            logger.debug("Found textUnitDTO for target locale");
        } else if (InheritanceMode.USE_PARENT.equals(inheritanceMode)) {
            logger.debug("No textUnitDTO found for target locale, look for translations in parent locales");
            textUnitDTO = getTextUnitDTOFromParents(md5);
        }

        return textUnitDTO;
    }

    public boolean hasTranslationWithoutInheritance() {
        return !getTextUnitDTOsForLocaleMapFromCache(repositoryLocale.getLocale().getId()).isEmpty();
    }

    /**
     * Look for a translation in parent locale excluding the root locale for a
     * given MD5.
     *
     * <p>
     * The root locale (repository locale with parent locale null) is excluded
     * for optimization purpose, the translations fetched would be the same as
     * the source, hence returning the source directly is more efficient.
     *
     * @param md5    MD5 to lookup the translation
     * @param source the source to fallback to if there is no translation
     * @return the translation from a parent locale if it exists else null
     */
    private TextUnitDTO getTextUnitDTOFromParents(String md5) {

        TextUnitDTO textUnitDTOWithInheritance = null;

        logger.debug("Get TextUnitDTO with inheritance");
        RepositoryLocale repositoryLocaleForFind = repositoryLocale.getParentLocale();

        while (textUnitDTOWithInheritance == null && repositoryLocaleForFind != null && repositoryLocaleForFind.getParentLocale() != null) {

            logger.debug("Looking for a TextUnitDTO in locale: {}", repositoryLocaleForFind.getLocale().getBcp47Tag());
            TextUnitDTO textUnitDTO = getTextUnitDTO(md5, repositoryLocaleForFind.getLocale().getId());

            if (textUnitDTO != null) {
                logger.debug("Found a TextUnitDTO using inheritance in locale: {}", repositoryLocaleForFind.getLocale().getBcp47Tag());
                textUnitDTOWithInheritance = textUnitDTO;
            } else {
                logger.debug("No inherited TextUnitDTO in locale: {}", repositoryLocaleForFind.getLocale().getBcp47Tag());
            }

            repositoryLocaleForFind = repositoryLocaleForFind.getParentLocale();
        }

        return textUnitDTOWithInheritance;
    }

    /**
     * Gets a {@link TextUnitDTO} from the cache for a given locale and text
     * unit MD5.
     *
     * @param md5      the MD5 of the text unit (see
     *                 {@link TMService#computeTMTextUnitMD5(java.lang.String, java.lang.String, java.lang.String)})
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
            translationsForLocaleMap = getTextUnitDTOsForLocaleByMD5(localeId);
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
    private Map<String, TextUnitDTO> getTextUnitDTOsForLocaleByMD5(Long localeId) {
        return textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(asset.getId(), localeId, statusFilter, false, UpdateType.ALWAYS);
    }
}
