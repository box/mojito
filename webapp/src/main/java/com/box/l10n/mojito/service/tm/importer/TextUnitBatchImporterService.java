package com.box.l10n.mojito.service.tm.importer;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
public class TextUnitBatchImporterService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TextUnitBatchImporterService.class);

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    TMService tmService;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    IntegrityCheckerFactory integrityCheckerFactory;

    /**
     * Imports a batch of text units.
     *
     * Assumes the text units have the following mandatory attributes:
     * repository name, target locale, asset path, name, target
     *
     * Optional attribute: tm text unit id, comment
     *
     * If mandatory attributes are missing the text unit will be skipped
     *
     * @param textUnitDTOs text units to import
     * @return
     */
    @Pollable(async = true)
    public PollableFuture asyncImportTextUnits(List<TextUnitDTO> textUnitDTOs) {
        logger.debug("Import {} text units", textUnitDTOs.size());
        List<TextUnitForBatchImport> skipOrConvertToTextUnitBatch = skipOrConvertToTextUnitBatchs(textUnitDTOs);
        Map<Locale, Map<Asset, List<TextUnitForBatchImport>>> textUnitsByLocales = groupTextUnitsByLocale(skipOrConvertToTextUnitBatch);

        for (Map.Entry<Locale, Map<Asset, List<TextUnitForBatchImport>>> textUnitsByLocale : textUnitsByLocales.entrySet()) {
            for (Map.Entry<Asset, List<TextUnitForBatchImport>> textUnitsByAsset : textUnitsByLocale.getValue().entrySet()) {

                Locale locale = textUnitsByLocale.getKey();
                Asset asset = textUnitsByAsset.getKey();
                List<TextUnitForBatchImport> textUnitsForBatchImport = textUnitsByAsset.getValue();

                mapTextUnitsToImportWithExistingTextUnits(locale, asset, textUnitsForBatchImport);
                applyIntegrityChecks(asset, textUnitsForBatchImport);
                importTextUnitsOfLocaleAndAsset(locale, asset, textUnitsForBatchImport);
            }
        }

        return new PollableFutureTaskResult();
    }

    /**
     * Maps text units to import with existing text units by first looking up
     * the tm text unit id then the name of used text unit and finally the name
     * of unused text unit (if there is only one of them for a given name) *
     *
     * @param locale
     * @param asset
     * @param textUnitsToImport text units to which the current text units must
     * be added
     */
    void mapTextUnitsToImportWithExistingTextUnits(Locale locale, Asset asset, List<TextUnitForBatchImport> textUnitsToImport) {

        List<TextUnitDTO> textUnitTDOsForLocaleAndAsset = getTextUnitTDOsForLocaleAndAsset(locale, asset);

        Map<Long, TextUnitDTO> tmTextUnitIdToTextUnitDTO = new HashMap<>();
        Map<String, TextUnitDTO> nameToUsedTextUnitDTO = new HashMap<>();
        Multimap<String, TextUnitDTO> nameToUnusedTextUnitDTO = ArrayListMultimap.create();

        logger.debug("Build maps to match text units to import with existing text units");
        for (TextUnitDTO textUnitDTO : textUnitTDOsForLocaleAndAsset) {
            tmTextUnitIdToTextUnitDTO.put(textUnitDTO.getTmTextUnitId(), textUnitDTO);
            if (textUnitDTO.isUsed()) {
                nameToUsedTextUnitDTO.put(textUnitDTO.getName(), textUnitDTO);
            }
            nameToUnusedTextUnitDTO.put(textUnitDTO.getName(), textUnitDTO);
        }

        logger.debug("Start mapping the text units to import with existing text units");
        for (TextUnitForBatchImport textUnitForBatchImport : textUnitsToImport) {

            TextUnitDTO currentTextUnit = tmTextUnitIdToTextUnitDTO.get(textUnitForBatchImport.getTmTextUnitId());

            if (currentTextUnit != null) {
                textUnitForBatchImport.setCurrentTextUnit(currentTextUnit);
                continue;
            }

            currentTextUnit = nameToUsedTextUnitDTO.get(textUnitForBatchImport.getName());

            if (currentTextUnit != null) {
                textUnitForBatchImport.setCurrentTextUnit(currentTextUnit);
                continue;
            }

            if (nameToUnusedTextUnitDTO.get(textUnitForBatchImport.getName()).size() == 1) {
                textUnitForBatchImport.setCurrentTextUnit(nameToUnusedTextUnitDTO.get(textUnitForBatchImport.getName()).iterator().next());
            }
        }
    }

    @Transactional
    void importTextUnitsOfLocaleAndAsset(Locale locale, Asset asset, List<TextUnitForBatchImport> textUnitsToImport) {
        DateTime importTime = new DateTime();
        logger.debug("Start import text units for asset: {} and locale: {}", asset.getPath(), locale.getBcp47Tag());

        for (TextUnitForBatchImport textUnitForBatchImport : textUnitsToImport) {

            TextUnitDTO currentTextUnit = textUnitForBatchImport.getCurrentTextUnit();

            if (currentTextUnit == null) {
                logger.debug("No matching text unit for name: {}", textUnitForBatchImport.getName());
            } else if (textUnitForBatchImport.getContent() == null) {
                logger.error("Content can't be null, skip for name: {}", textUnitForBatchImport.getName());
            } else if (!textUnitForBatchImport.getContent().equals(currentTextUnit.getTarget())) {
                logger.debug("Add translation: {} --> {}", textUnitForBatchImport.getName(), textUnitForBatchImport.getContent());

                TMTextUnitCurrentVariant tmTextUnitCurrentVariant = null;
                if (currentTextUnit.getTmTextUnitCurrentVariantId() != null) {
                    logger.debug("Looking up current variant");
                    tmTextUnitCurrentVariant = tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(currentTextUnit.getLocaleId(), currentTextUnit.getTmTextUnitId());
                }

                tmService.addTMTextUnitCurrentVariantWithResult(
                        tmTextUnitCurrentVariant,
                        asset.getRepository().getTm().getId(),
                        currentTextUnit.getTmTextUnitId(),
                        locale.getId(),
                        textUnitForBatchImport.getContent(),
                        textUnitForBatchImport.getComment(),
                        textUnitForBatchImport.isIncludedInLocalizedFile() ? TMTextUnitVariant.Status.APPROVED : TMTextUnitVariant.Status.TRANSLATION_NEEDED,
                        textUnitForBatchImport.isIncludedInLocalizedFile(),
                        importTime);

            } else {
                logger.debug("Same targets, skip: {}", textUnitForBatchImport.getName());
            }
        }
    }

    List<TextUnitDTO> getTextUnitTDOsForLocaleAndAsset(Locale locale, Asset asset) {

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(asset.getRepository().getId());
        textUnitSearcherParameters.setAssetId(asset.getId());
        textUnitSearcherParameters.setLocaleId(locale.getId());

        return textUnitSearcher.search(textUnitSearcherParameters);
    }

    Map<Locale, Map<Asset, List<TextUnitForBatchImport>>> groupTextUnitsByLocale(List<TextUnitForBatchImport> textUnitBatches) {

        Map<Locale, Map<Asset, List<TextUnitForBatchImport>>> textUnitsForBatchImportByLocale = new HashMap<>();

        for (TextUnitForBatchImport textUnitBatch : textUnitBatches) {

            Map<Asset, List<TextUnitForBatchImport>> textUnitForBatchImportByLocale = textUnitsForBatchImportByLocale.get(textUnitBatch.getLocale());
            if (textUnitForBatchImportByLocale == null) {
                textUnitForBatchImportByLocale = new HashMap<>();
                textUnitsForBatchImportByLocale.put(textUnitBatch.getLocale(), textUnitForBatchImportByLocale);
            }

            List<TextUnitForBatchImport> textUnitForBatchImports = textUnitForBatchImportByLocale.get(textUnitBatch.getAsset());
            if (textUnitForBatchImports == null) {
                textUnitForBatchImports = new ArrayList<>();
                textUnitForBatchImportByLocale.put(textUnitBatch.getAsset(), textUnitForBatchImports);
            }

            textUnitForBatchImports.add(textUnitBatch);
        }

        return textUnitsForBatchImportByLocale;
    }

    void applyIntegrityChecks(Asset asset, List<TextUnitForBatchImport> textUnitsForBatchImport) {

        Set<TextUnitIntegrityChecker> textUnitCheckers = integrityCheckerFactory.getTextUnitCheckers(asset);

        for (TextUnitForBatchImport textUnitForBatchImport : textUnitsForBatchImport) {
            for (TextUnitIntegrityChecker textUnitChecker : textUnitCheckers) {
                
                Preconditions.checkNotNull(textUnitForBatchImport.getCurrentTextUnit(), "Current text unit must be set to apply integrity checks");
             
                try {
                    textUnitChecker.check(textUnitForBatchImport.getCurrentTextUnit().getSource(), textUnitForBatchImport.getContent());
                    textUnitForBatchImport.setIncludedInLocalizedFile(true);
                } catch(IntegrityCheckException ice) {
                    textUnitForBatchImport.setIncludedInLocalizedFile(false);
                }
            }
        }
    }

    List<TextUnitForBatchImport> skipOrConvertToTextUnitBatchs(List<TextUnitDTO> textUnitDTOs) {

        List<TextUnitForBatchImport> textUnitsForBatchImport = new ArrayList<>();

        Map<String, Repository> repositoriesByName = new HashMap<>();
        Map<Repository, Map<String, Asset>> assetsByRepositoryAndPath = new HashMap<>();

        for (TextUnitDTO textUnitDTO : textUnitDTOs) {

            TextUnitForBatchImport textUnitBatch = new TextUnitForBatchImport();

            Repository repository = repositoriesByName.get(textUnitDTO.getRepositoryName());
            if (repository == null) {
                repository = repositoryRepository.findByName(textUnitDTO.getRepositoryName());
                repositoriesByName.put(textUnitDTO.getRepositoryName(), repository);
                assetsByRepositoryAndPath.put(repository, new HashMap());
            }

            if (repository == null) {
                logger.debug("Skip, repository name not valid");
                continue;
            }
            textUnitBatch.setRepository(repository);

            Asset asset = assetsByRepositoryAndPath.get(repository).get(textUnitDTO.getAssetPath());
            if (asset == null) {
                asset = assetRepository.findByPathAndRepositoryId(textUnitDTO.getAssetPath(), repository.getId());
                assetsByRepositoryAndPath.get(repository).put(textUnitDTO.getAssetPath(), asset);
            }

            if (asset == null) {
                logger.debug("Skip, asset path not valid");
                continue;
            }
            textUnitBatch.setAsset(asset);

            Locale locale = localeService.findByBcp47Tag(textUnitDTO.getTargetLocale());
            if (locale == null) {
                logger.debug("Skip, locale not valid");
                continue;
            }

            textUnitBatch.setLocale(locale);
            textUnitBatch.setName(textUnitDTO.getName());
            textUnitBatch.setContent(NormalizationUtils.normalize(textUnitDTO.getTarget()));
            textUnitBatch.setComment(textUnitDTO.getComment());
            textUnitBatch.setIncludedInLocalizedFile(true);
            
            textUnitsForBatchImport.add(textUnitBatch);
        }

        return textUnitsForBatchImport;
    }

}
