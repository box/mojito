package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.importer.ImporterCacheService;
import com.google.common.cache.LoadingCache;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.utils.Predicates.logIfFalse;

@Component
public class ThirdPartyTextUnitSearchService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ThirdPartyTextUnitSearchService.class);

    @Autowired
    ImporterCacheService importerCacheService;

    @Autowired
    ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    final private String pluralSuffix = "_other";

    List<ThirdPartyTextUnitForBatchImport> convertDTOToBatchImport(Set<ThirdPartyTextUnitDTO> thirdPartyTextUnitDTOSet, boolean isPluralFile) {

        logger.debug("Create caches to map convert to ThirdPartyTextUnitForBatchImport list");
        LoadingCache<Map.Entry<String, String>, Asset> assetsCache = importerCacheService.createAssetsCache();

        logger.debug("Convert to ThirdPartyTextUnitForBatchImport");
        return thirdPartyTextUnitDTOSet.stream()
                .filter(logIfFalse(t -> t.getThirdPartyTextUnitId() != null, logger, "Missing mandatory third party text unit id, skip: {}", ThirdPartyTextUnitDTO::getThirdPartyTextUnitId))
                .filter(logIfFalse(t -> t.getMappingKey() != null, logger, "Missing mandatory mapping key, skip: {}", ThirdPartyTextUnitDTO::getThirdPartyTextUnitId))
                .filter(logIfFalse(t -> t.getTmTextUnitName() != null, logger, "Missing mandatory text unit name, skip: {}", ThirdPartyTextUnitDTO::getThirdPartyTextUnitId))

                .map(t -> {
                    ThirdPartyTextUnitForBatchImport thirdPartyTextUnitForBatchImport = new ThirdPartyTextUnitForBatchImport();
                    thirdPartyTextUnitForBatchImport.setThirdPartyTextUnitId(t.getThirdPartyTextUnitId());
                    thirdPartyTextUnitForBatchImport.setMappingKey(t.getMappingKey());

                    thirdPartyTextUnitForBatchImport.setAsset(assetsCache.getUnchecked(new AbstractMap.SimpleEntry<>(
                            t.getAssetPath(), t.getRepositoryName())));
                    if (thirdPartyTextUnitForBatchImport.getAsset() != null) {
                        thirdPartyTextUnitForBatchImport.setRepository(thirdPartyTextUnitForBatchImport.getAsset().getRepository());
                        String textUnitName = t.getTmTextUnitName();

                        if (isPluralFile) {
                            textUnitName += pluralSuffix;
                        }

                        Optional<TMTextUnitCurrentVariant> foundCurrentVariant = tmTextUnitCurrentVariantRepository.findAllByTmTextUnit_NameAndTmTextUnit_Asset_Id(
                                textUnitName,
                                thirdPartyTextUnitForBatchImport.getAsset().getId()
                        ).stream().filter(notAlreadyMatched()).findFirst();

                        foundCurrentVariant.ifPresent(tmTextUnitCurrentVariant -> thirdPartyTextUnitForBatchImport.setTmTextUnit(tmTextUnitCurrentVariant.getTmTextUnit()));
                    }

                    return thirdPartyTextUnitForBatchImport;
                })

                .filter(logIfFalse(t -> t.getRepository() != null, logger, "No repository found, skip: {}", ThirdPartyTextUnitForBatchImport::getThirdPartyTextUnitId))
                .filter(logIfFalse(t -> t.getAsset() != null, logger, "No asset found, skip: {}", ThirdPartyTextUnitForBatchImport::getThirdPartyTextUnitId))
                .filter(logIfFalse(t -> t.getTmTextUnit() != null, logger, "No tm text unit found, skip: {}", ThirdPartyTextUnitForBatchImport::getThirdPartyTextUnitId))

                .collect(Collectors.toList());
    }

    List<ThirdPartyTextUnitDTO> getByThirdPartyTextUnitIdsAndMappingKeys(List<ThirdPartyTextUnitForBatchImport> thirdPartyTextUnitsToImport) {
        List<String> thirdPartyTextUnitIds = thirdPartyTextUnitsToImport
                .stream()
                .map(ThirdPartyTextUnitForBatchImport::getThirdPartyTextUnitId)
                .collect(Collectors.toList());

        List<String> mappingKeys = thirdPartyTextUnitsToImport
                .stream()
                .map(ThirdPartyTextUnitForBatchImport::getMappingKey)
                .collect(Collectors.toList());

        return search(thirdPartyTextUnitIds, mappingKeys);
    }

    @Transactional
    public List<ThirdPartyTextUnitDTO> search(List<String> thirdPartyTextUnitIds, List<String> mappingKeys) {
        return thirdPartyTextUnitRepository.getByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(
                thirdPartyTextUnitIds, mappingKeys
        );
    }

    // Predicate that make sure that current variant text units are matched only once.
    private Predicate<TMTextUnitCurrentVariant> notAlreadyMatched() {
        ConcurrentHashSet<Long> alreadyMappedTmTextUnitIds = new ConcurrentHashSet<>();
        return (t) -> {
            boolean notAlreadyMatched = !alreadyMappedTmTextUnitIds.contains(t.getTmTextUnit().getId());
            if (notAlreadyMatched) {
                logger.debug("Text unit: {} ({}) not matched yet", t.getTmTextUnit().getId(), t.getTmTextUnit().getName());
                alreadyMappedTmTextUnitIds.add(t.getTmTextUnit().getId());
            } else {
                logger.debug("Text unit: {}, ({}) is already matched, can't used it", t.getTmTextUnit().getId(), t.getTmTextUnit().getName());
            }
            return notAlreadyMatched;
        };
    }

}