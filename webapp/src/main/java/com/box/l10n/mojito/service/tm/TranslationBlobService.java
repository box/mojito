package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.aspect.StopWatch;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetMappingDTO;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
public class TranslationBlobService {

    static Logger logger = LoggerFactory.getLogger(TranslationBlobService.class);

    @Autowired
    StructuredBlobStorage structuredBlobStorage;

    @Autowired
    @Qualifier("fail_on_unknown_properties_false")
    ObjectMapper objectMapper;

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

    public ImmutableMap<String, TextUnitDTO> getTextUnitDTOsForLocaleByMD5New(Long assetId, Long localeId, StatusFilter statusFilter, boolean isRootLocale, boolean updateBlob) {

        TranslationBlob translationBlob = getTranslationBlob(assetId, localeId);

        if (!updateBlob) {
            //TODO(perf) argggg - mutli return ect....
            return  translationBlob.getTextUnitDTOs().stream()
                    .filter(statusPredicate(statusFilter))
                    .collect(ImmutableMap.toImmutableMap(getTextUnitDTOMd5(), Function.identity()));
        }

        //TODO(perf) this will become slow as the asset becomes fully translated
        List<TMTextUnitCurrentVariantDTO> currentVariants = getCurrentVariantsByAssetIdAndLocaleId(assetId, localeId);

        // will keep querying that which is bad for perf and consitancy - pass it as an option
        //TODO(perf) this could/should be read for S3 too since this would change only when the asset changes. so
        // we have this data
        //TODO(perf) this doesn't capture unused strings!
        //TODO(perf) check that this asset is deleted?
        List<Long> tmTextUnitIds = getTextUnitIdsByAssetId(assetId);

        // should be in service?
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> new IllegalArgumentException("Asset missing for given id"));
        Set<Long> currentTmTextUnits = findTmTextUnitIdsByAssetExtractionId(asset);

        // need to fetch current variant to are not translationblob or that have been changed
        ImmutableMap<Long, TextUnitDTO> fromBlobByTmTextUnitId = translationBlob.getTextUnitDTOs().stream()
                .collect(ImmutableMap.toImmutableMap(TextUnitDTO::getTmTextUnitId, Function.identity()));

        Stream<Long> toFetchFromCurrentVariant = currentVariants.stream()
                .filter(t -> {
                    TextUnitDTO textUnitDTO = fromBlobByTmTextUnitId.get(t.getTmTextUnitId());
                    return textUnitDTO == null ||
                            !Objects.equals(textUnitDTO.getTmTextUnitVariantId(), t.getTmTextUnitVariantId());
                })
                .map(TMTextUnitCurrentVariantDTO::getTmTextUnitId);

        Stream<Long> toFetchFromTextUnits = tmTextUnitIds.stream()
                .filter(tmTextUnitId -> !fromBlobByTmTextUnitId.containsKey(tmTextUnitId));

        Stream<Long> toFetchUsedChanged = translationBlob.getTextUnitDTOs().stream()
                .peek(t -> logger.debug("checking used has changed: {}", t.getName()))
                .filter(t -> {
                    boolean newUsed = currentTmTextUnits.contains(t.getTmTextUnitId());
                    return newUsed != t.isUsed(); // useless fetch when asset is removed? but makes it more synced?
                })
                .peek(t -> logger.debug("used has changed: {}", t.getName()))
                .map(TextUnitDTO::getTmTextUnitId);

        ImmutableSet<Long> textUnitsToFetch = Streams.concat(toFetchFromCurrentVariant, toFetchFromTextUnits, toFetchUsedChanged).collect(ImmutableSet.toImmutableSet());

        logger.info("Number of text units to fetch: {} (of total: {})", textUnitsToFetch.size(), tmTextUnitIds.size());

        ImmutableMap<Long, TextUnitDTO> fetchedByTmTextUnitId = Lists.partition(textUnitsToFetch.asList(), 100).stream()
                .flatMap(tmTextUnitIdsForBatch -> {
                    logger.debug("fetching for tmTextUnitIds: {}", tmTextUnitIdsForBatch);

                    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
                    textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitIdsForBatch);
                    textUnitSearcherParameters.setLocaleId(localeId);
                    textUnitSearcherParameters.setAssetId(assetId);

                    if (isRootLocale) {
                        //TODO(perf) we probably want to use the asset instead
                        textUnitSearcherParameters.setRootLocaleExcluded(false);
                        textUnitSearcherParameters.setPluralFormsFiltered(false);
                    }

                    List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
                    return search.stream();
                })
                .collect(ImmutableMap.toImmutableMap(TextUnitDTO::getTmTextUnitId, Function.identity()));

        ImmutableMap<String, TextUnitDTO> byMd5s = tmTextUnitIds.stream()
                .map(id -> {
                    TextUnitDTO textUnitDTO = fetchedByTmTextUnitId.get(id);

                    if (textUnitDTO == null) {
                        textUnitDTO = fromBlobByTmTextUnitId.get(id);
                    }
                    return textUnitDTO;
                })
                .filter(Objects::nonNull) // plural forms won't match for some language so need to skip
                .map(textUnitDTO -> {
                    textUnitDTO.setAssetDeleted(asset.getDeleted());
                    return textUnitDTO;
                })
                .collect(ImmutableMap.toImmutableMap(getTextUnitDTOMd5(), Function.identity()));


        TranslationBlob toWrite = new TranslationBlob();
        toWrite.setTextUnitDTOs(byMd5s.values().asList());
        logger.debug("Writting to translation blob: {}", toWrite.getTextUnitDTOs().size());
        structuredBlobStorage.put(StructuredBlobStorage.Prefix.TRANSLATIONS, getBlobName(assetId, localeId), objectMapper.writeValueAsStringUnchecked(toWrite), Retention.PERMANENT);

        ImmutableMap<String, TextUnitDTO> withStatus = byMd5s.values().stream()
                .filter(statusPredicate(statusFilter))
                .collect(ImmutableMap.toImmutableMap(getTextUnitDTOMd5(), Function.identity()));

        return withStatus;
    }

    Set<Long> findTmTextUnitIdsByAssetExtractionId(Asset asset) {
        return assetTextUnitToTMTextUnitRepository.findTmTextUnitIdsByAssetExtractionId(asset.getLastSuccessfulAssetExtraction().getId());
    }

    @StopWatch
    TranslationBlob getTranslationBlob(Long assetId, Long localeId) {
        Optional<String> translationBlobString = structuredBlobStorage.getString(StructuredBlobStorage.Prefix.TRANSLATIONS, getBlobName(assetId, localeId));
        return translationBlobString.map(s -> objectMapper.readValueUnchecked(s, TranslationBlob.class)).orElse(new TranslationBlob());
    }

    @StopWatch
    List<Long> getTextUnitIdsByAssetId(Long assetId) {
        return tmTextUnitRepository.getTextUnitIdsByAssetId(assetId);
    }

    @StopWatch
    List<TMTextUnitCurrentVariantDTO> getCurrentVariantsByAssetIdAndLocaleId(Long assetId, Long localeId) {
        return tmTextUnitCurrentVariantRepository.findByAsset_idAndLocale_Id(assetId, localeId);
    }

    /**
     * this implementation should be in sync with what's done in {@llink TextUnitSearcher}
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
                    boolean b = t.getTmTextUnitVariantId() == null ||
                            TMTextUnitVariant.Status.TRANSLATION_NEEDED.equals(t.getStatus()) ||
                            !t.isIncludedInLocalizedFile();
                    logger.trace("predicate for translation: {}, {} --> {} ({}, {}, {})", t.getTargetLocale(), t.getName(), b, t.getTmTextUnitVariantId(), t.isIncludedInLocalizedFile(), t.getStatus());
                    return b;

                default:
                    throw new RuntimeException("Filter type not implemented");
            }
        };
    }


    private String getBlobName(Long assetId, Long localeId) {
        return "asset/" + assetId + "/locale/" + localeId;
    }

    Function<TextUnitDTO, String> getTextUnitDTOMd5() {
        return textUnitDTO -> textUnitUtils.computeTextUnitMD5(textUnitDTO.getName(), textUnitDTO.getSource(), textUnitDTO.getComment());
    }
}