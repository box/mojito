package com.box.l10n.mojito.service.tm.textunitdtocache;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantDTO;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * This class is the meant to access TextUnitDTOs in an optimized way, reading most of data
 * from the {@link com.box.l10n.mojito.service.blobstorage.BlobStorage} and optionally reading deltas from the database.
 * <p>
 * It is designed to access data given an asset and a locale only. It is meant to be used when the whole state
 * needs to be processed like for file generation, or syncing translation memories. Using the TextUnitSearcher directly
 * in those cases bring to much load on the database and doesn't scale.
 */
@Component
public class TextUnitDTOsCacheService {

    static final int FETCH_BATCH_SIZE = 5000;

    static Logger logger = LoggerFactory.getLogger(TextUnitDTOsCacheService.class);

    @Autowired
    TextUnitDTOsCacheBlobStorage textUnitDTOsCacheBlobStorage;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TextUnitUtils textUnitUtils;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

    @Autowired
    AssetRepository assetRepository;

    public ImmutableMap<String, TextUnitDTO> getTextUnitDTOsForAssetAndLocaleByMD5(Long assetId, Long localeId, StatusFilter statusFilter, boolean isRootLocale, UpdateType updateType) {
        ImmutableList<TextUnitDTO> textUnitDTOsForAssetAndLocale = getTextUnitDTOsForAssetAndLocale(assetId, localeId, isRootLocale, updateType);
        ImmutableMap<String, TextUnitDTO> filteredWithStatus = filterWithStatusAndMap(statusFilter, textUnitDTOsForAssetAndLocale);
        return filteredWithStatus;
    }

    ImmutableMap<String, TextUnitDTO> filterWithStatusAndMap(StatusFilter statusFilter, ImmutableList<TextUnitDTO> textUnitDTOsForAssetAndLocale) {
        return textUnitDTOsForAssetAndLocale.stream()
                .filter(statusPredicate(statusFilter))
                .collect(ImmutableMap.toImmutableMap(funGetTextUnitDTOMd5(), Function.identity()));
    }

    @Timed("TextUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocale")
    public ImmutableList<TextUnitDTO> getTextUnitDTOsForAssetAndLocale(Long assetId, Long localeId, boolean isRootLocale, UpdateType updateType) {

        Optional<ImmutableList<TextUnitDTO>> optionalTextUnitDTOs = textUnitDTOsCacheBlobStorage.getTextUnitDTOs(assetId, localeId);

        ImmutableList<TextUnitDTO> textUnitDTOs = optionalTextUnitDTOs.orElse(ImmutableList.of());

        if (UpdateType.ALWAYS.equals(updateType) ||
                (UpdateType.IF_MISSING.equals(updateType) && !optionalTextUnitDTOs.isPresent())) {
            textUnitDTOs = updateTextUnitDTOsWithDeltaFromDatabase(textUnitDTOs, assetId, localeId, isRootLocale);
        }

        return textUnitDTOs;
    }

    /**
     * - Looks for updated translations
     * - Make sure all text units in the database have an entry in the cache
     * - Update "used" status as needed
     *
     * @param toUpdate
     * @param assetId
     * @param localeId
     * @param isRootLocale
     * @return
     */
    @Timed("TextUnitDTOsCacheService.updateTextUnitDTOsWithDeltaFromDatabase")
    ImmutableList<TextUnitDTO> updateTextUnitDTOsWithDeltaFromDatabase(ImmutableList<TextUnitDTO> toUpdate, Long assetId, Long localeId, boolean isRootLocale) {

        Asset asset = getAssetById(assetId);

        ImmutableList<TMTextUnitCurrentVariantDTO> currentTranslations = getCurrentTranslationsOfAllTextUnits(assetId, localeId);
        ImmutableList<Long> idsOfAllTextUnits = getIdsOfAllTextUnits(assetId);
        ImmutableSet<Long> idsOfUsedTextUnits = getIdsOfUsedTextUnits(asset);

        ImmutableMap<Long, TextUnitDTO> toUpdateByTmTextUnitIds = toUpdate.stream()
                .collect(ImmutableMap.toImmutableMap(TextUnitDTO::getTmTextUnitId, Function.identity()));

        ImmutableSet<Long> textUnitIdsToFetch = Streams.concat(
                getTmTextUnitIdsForNewTranslations(currentTranslations, toUpdateByTmTextUnitIds),
                getTmTextUnitIdsOfMissingTextUnits(idsOfAllTextUnits, toUpdateByTmTextUnitIds),
                getTmTextUnitIdsForChangedUsedStatus(idsOfUsedTextUnits, toUpdate))
                .collect(ImmutableSet.toImmutableSet());

        logger.debug("Number of text units to fetch: {} (of total: {})", textUnitIdsToFetch.size(), idsOfAllTextUnits.size());

        ImmutableMap<Long, TextUnitDTO> fetchedByTmTextUnitId = fetchTextUnitDTOForTmTextUnitIds(assetId, localeId, isRootLocale, textUnitIdsToFetch);

        ImmutableList<TextUnitDTO> textUnitDTOsForAllTextUnits = getTextUnitDTOsForAllTextUnits(asset, idsOfAllTextUnits, toUpdateByTmTextUnitIds, fetchedByTmTextUnitId);

        if (!toUpdate.equals(textUnitDTOsForAllTextUnits)) {
            textUnitDTOsCacheBlobStorage.putTextUnitDTOs(assetId, localeId, textUnitDTOsForAllTextUnits);
        } else {
            logger.debug("No change in text units, don't write blob");
        }

        return textUnitDTOsForAllTextUnits;
    }

    /**
     * First look into what has just been fetch (to get updated and a newly fetch entries and then look for what was
     * in the old cache (unchanged entries)
     *
     * @param asset
     * @param idsOfAllTextUnits
     * @param textUnitDTOsToUpdateByTmTextUnitIds
     * @param fetchedByTmTextUnitId
     * @return
     */
    ImmutableList<TextUnitDTO> getTextUnitDTOsForAllTextUnits(Asset asset, List<Long> idsOfAllTextUnits, ImmutableMap<Long, TextUnitDTO> textUnitDTOsToUpdateByTmTextUnitIds, ImmutableMap<Long, TextUnitDTO> fetchedByTmTextUnitId) {
        return idsOfAllTextUnits.stream()
                .map(id -> {
                    TextUnitDTO textUnitDTO = fetchedByTmTextUnitId.get(id);

                    if (textUnitDTO == null) {
                        textUnitDTO = textUnitDTOsToUpdateByTmTextUnitIds.get(id);
                    }
                    return textUnitDTO;
                })
                .filter(Objects::nonNull) // plural forms won't have a match for some language so need to skip
                .map(textUnitDTO -> {
                    textUnitDTO.setAssetDeleted(asset.getDeleted()); // this has side effects...
                    return textUnitDTO;
                })
                .collect(ImmutableList.toImmutableList());
    }

    ImmutableMap<Long, TextUnitDTO> fetchTextUnitDTOForTmTextUnitIds(Long assetId, Long localeId, boolean isRootLocale, ImmutableSet<Long> textUnitIdsToFetch) {
        return Lists.partition(textUnitIdsToFetch.asList(), FETCH_BATCH_SIZE).stream()
                .flatMap(tmTextUnitIdsForBatch -> {
                    logger.debug("fetching for tmTextUnitIds: {}", tmTextUnitIdsForBatch);

                    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
                    textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitIdsForBatch);
                    textUnitSearcherParameters.setLocaleId(localeId);
                    textUnitSearcherParameters.setAssetId(assetId);

                    if (isRootLocale) {
                        textUnitSearcherParameters.setRootLocaleExcluded(false);
                        textUnitSearcherParameters.setPluralFormsFiltered(false);
                    }

                    List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
                    return search.stream();
                })
                .collect(ImmutableMap.toImmutableMap(TextUnitDTO::getTmTextUnitId, Function.identity()));
    }

    Stream<Long> getTmTextUnitIdsForChangedUsedStatus(Set<Long> idsOfUsedTextUnits, ImmutableList<TextUnitDTO> textUnitDTOsToUpdate) {
        return textUnitDTOsToUpdate.stream()
                .filter(t -> {
                    boolean newUsed = idsOfUsedTextUnits.contains(t.getTmTextUnitId());
                    return newUsed != t.isUsed();
                })
                .map(TextUnitDTO::getTmTextUnitId);
    }

    Stream<Long> getTmTextUnitIdsOfMissingTextUnits(List<Long> idsOfAllTextUnits, ImmutableMap<Long, TextUnitDTO> textUnitDTOsToUpdateByTmTextUnitIds) {
        return idsOfAllTextUnits.stream()
                .filter(tmTextUnitId -> !textUnitDTOsToUpdateByTmTextUnitIds.containsKey(tmTextUnitId));
    }

    Stream<Long> getTmTextUnitIdsForNewTranslations(List<TMTextUnitCurrentVariantDTO> currentTranslations, ImmutableMap<Long, TextUnitDTO> previousTextUnitDTOsByTmTextUnitIds) {
        return currentTranslations.stream()
                .filter(current -> {
                    TextUnitDTO previous = previousTextUnitDTOsByTmTextUnitIds.get(current.getTmTextUnitId());
                    return previous == null ||
                            !Objects.equals(previous.getTmTextUnitVariantId(), current.getTmTextUnitVariantId());
                })
                .map(TMTextUnitCurrentVariantDTO::getTmTextUnitId);
    }

    Asset getAssetById(Long assetId) {
        return assetRepository.findById(assetId).orElseThrow(() -> new IllegalArgumentException("Asset missing for given id: " + assetId));
    }

    ImmutableSet<Long> getIdsOfUsedTextUnits(Asset asset) {
        ImmutableSet<Long> ids = ImmutableSet.of();
        if (asset.getLastSuccessfulAssetExtraction() != null) {
            ids = ImmutableSet.copyOf(assetTextUnitToTMTextUnitRepository.findTmTextUnitIdsByAssetExtractionId(asset.getLastSuccessfulAssetExtraction().getId()));
        }
        return ids;
    }

    /**
     * Gets the ids of all the text unit in an asset regardless if those text units are used or not.
     *
     * @param assetId
     * @return
     */
    @Timed("TextUnitDTOsCacheService.getIdsOfAllTextUnits")
    ImmutableList<Long> getIdsOfAllTextUnits(Long assetId) {
        return ImmutableList.copyOf(tmTextUnitRepository.getTextUnitIdsByAssetId(assetId));
    }

    /**
     * This provides the current translations of all text units (regardless if they are used or not).
     * <p>
     * Can be used to check if the current translation has changed and to refresh part of the cache.
     * <p>
     *
     * @param assetId
     * @param localeId
     * @return
     */
    @Timed("TextUnitDTOsCacheService.getCurrentTranslationsOfAllTextUnits")
    ImmutableList<TMTextUnitCurrentVariantDTO> getCurrentTranslationsOfAllTextUnits(Long assetId, Long localeId) {
        return ImmutableList.copyOf(tmTextUnitCurrentVariantRepository.findByAsset_idAndLocale_Id(assetId, localeId));
    }

    /**
     * This implementation should be in sync with what's done in {@link TextUnitSearcher}
     *
     * @param statusFilter
     * @return
     */
    public Predicate<TextUnitDTO> statusPredicate(StatusFilter statusFilter) {
        return t -> {
            if (statusFilter == null) {
                return true;
            }
            switch (statusFilter) {
                case ALL:
                    return true;
                case NOT_REJECTED:
                    return t.isIncludedInLocalizedFile();
                case REJECTED:
                    return !t.isIncludedInLocalizedFile();
                case REVIEW_NEEDED:
                    return TMTextUnitVariant.Status.REVIEW_NEEDED.equals(t.getStatus());
                case REVIEW_NOT_NEEDED:
                    return !TMTextUnitVariant.Status.REVIEW_NEEDED.equals(t.getStatus());
                case TRANSLATION_NEEDED:
                    return t.isTranslated() && TMTextUnitVariant.Status.TRANSLATION_NEEDED.equals(t.getStatus());
                case TRANSLATED:
                    return t.isTranslated();
                case APPROVED_AND_NOT_REJECTED:
                    return t.isTranslated()
                            &&
                            TMTextUnitVariant.Status.APPROVED.equals(t.getStatus())
                            &&
                            t.isIncludedInLocalizedFile();
                case APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED:
                    return (t.isTranslated() &&
                            TMTextUnitVariant.Status.APPROVED.equals(t.getStatus()) ||
                            TMTextUnitVariant.Status.REVIEW_NEEDED.equals(t.getStatus()))
                            &&
                            t.isIncludedInLocalizedFile();
                case TRANSLATED_AND_NOT_REJECTED:
                    return t.isTranslated() && t.isIncludedInLocalizedFile();
                case UNTRANSLATED:
                    return t.getTmTextUnitVariantId() == null;
                case FOR_TRANSLATION: // TODO(perf) for translation doesn't seem tested for deletes
                    return t.getTmTextUnitVariantId() == null ||
                            TMTextUnitVariant.Status.TRANSLATION_NEEDED.equals(t.getStatus()) ||
                            !t.isIncludedInLocalizedFile();
                default:
                    throw new RuntimeException("Filter type not implemented");
            }
        };
    }

    Function<TextUnitDTO, String> funGetTextUnitDTOMd5() {
        return textUnitDTO -> textUnitUtils.computeTextUnitMD5(textUnitDTO.getName(), textUnitDTO.getSource(), textUnitDTO.getComment());
    }
}