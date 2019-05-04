package com.box.l10n.mojito.service.tm.importer;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant.Status;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.ImportTextUnitJob;
import com.box.l10n.mojito.service.asset.ImportTextUnitJobInput;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.APPROVED;


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

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    /**
     * Imports a batch of text units.
     * <p>
     * Assumes the text units have the following mandatory attributes:
     * repository name, target locale, asset path, name, target
     * <p>
     * Optional attribute: tm text unit id, comment, status, includedInFile
     * <p>
     * If mandatory attributes are missing the text unit will be skipped
     * <p>
     * Integrity checks are applied and will determine the {@link Status}. Any string that passes the
     * integrity check will be imported as approved. If it doesn't pass the test it will be need translation or
     * the previous status when target is the same and the option is integrityCheckKeepStatusIfFailedAndSameTarget is
     * passed
     *
     * @param textUnitDTOs text units to import
     * @return
     */
    public PollableFuture asyncImportTextUnits(List<TextUnitDTO> textUnitDTOs,
                                               boolean integrityCheckSkipped,
                                               boolean integrityCheckKeepStatusIfFailedAndSameTarget) {

        ImportTextUnitJobInput importTextUnitJobInput = new ImportTextUnitJobInput();
        importTextUnitJobInput.setTextUnitDTOs(textUnitDTOs);
        importTextUnitJobInput.setIntegrityCheckSkipped(integrityCheckSkipped);
        importTextUnitJobInput.setIntegrityCheckKeepStatusIfFailedAndSameTarget(integrityCheckKeepStatusIfFailedAndSameTarget);

        return quartzPollableTaskScheduler.scheduleJob(ImportTextUnitJob.class, importTextUnitJobInput);
    }

    public PollableFuture importTextUnits(List<TextUnitDTO> textUnitDTOs,
                                          boolean integrityCheckSkipped,
                                          boolean integrityCheckKeepStatusIfFailedAndSameTarget) {

        logger.debug("Import {} text units", textUnitDTOs.size());
        List<TextUnitForBatchImport> skipOrConvertToTextUnitBatch = skipOrConvertToTextUnitBatchs(textUnitDTOs);
        Map<Locale, Map<Asset, List<TextUnitForBatchImport>>> textUnitsByLocales = groupTextUnitsByLocale(skipOrConvertToTextUnitBatch);

        for (Map.Entry<Locale, Map<Asset, List<TextUnitForBatchImport>>> textUnitsByLocale : textUnitsByLocales.entrySet()) {
            for (Map.Entry<Asset, List<TextUnitForBatchImport>> textUnitsByAsset : textUnitsByLocale.getValue().entrySet()) {

                Locale locale = textUnitsByLocale.getKey();
                Asset asset = textUnitsByAsset.getKey();
                List<TextUnitForBatchImport> textUnitsForBatchImport = textUnitsByAsset.getValue();

                mapTextUnitsToImportWithExistingTextUnits(locale, asset, textUnitsForBatchImport);

                if (!integrityCheckSkipped) {
                    applyIntegrityChecks(asset, textUnitsForBatchImport, integrityCheckKeepStatusIfFailedAndSameTarget);
                }

                importTextUnitsOfLocaleAndAsset(locale, asset, textUnitsForBatchImport);
            }
        }

        return new PollableFutureTaskResult();
    }

    /**
     * Maps text units to import with existing text units by first looking up
     * the tm text unit id then the name of used text unit and finally the name
     * of unused text unit (if there is only one of them for a given name)
     *
     * @param locale
     * @param asset
     * @param textUnitsToImport text units to which the current text units must be added
     */
    void mapTextUnitsToImportWithExistingTextUnits(Locale locale, Asset asset, List<TextUnitForBatchImport> textUnitsToImport) {

        List<TextUnitDTO> textUnitTDOsForLocaleAndAsset = getTextUnitTDOsForLocaleAndAsset(locale, asset);

        Map<Long, TextUnitDTO> tmTextUnitIdToTextUnitDTO = new HashMap<>();
        ArrayListMultimap<String, TextUnitDTO> nameToUsedTextUnitDTO = ArrayListMultimap.create();
        ArrayListMultimap<String, TextUnitDTO> nameToUnusedTextUnitDTO = ArrayListMultimap.create();
        Set<Long> mappedTmTextUnitIds = new HashSet<>();

        logger.debug("Build maps to match text units to import with existing text units");
        for (TextUnitDTO textUnitDTO : textUnitTDOsForLocaleAndAsset) {
            tmTextUnitIdToTextUnitDTO.put(textUnitDTO.getTmTextUnitId(), textUnitDTO);
            if (textUnitDTO.isUsed()) {
                nameToUsedTextUnitDTO.put(textUnitDTO.getName(), textUnitDTO);
            } else {
                nameToUnusedTextUnitDTO.put(textUnitDTO.getName(), textUnitDTO);
            }
        }

        logger.debug("Start mapping the text units to import with existing text units");
        for (TextUnitForBatchImport textUnitForBatchImport : textUnitsToImport) {

            TextUnitDTO currentTextUnit = tmTextUnitIdToTextUnitDTO.get(textUnitForBatchImport.getTmTextUnitId());

            if (currentTextUnit != null) {
                logger.debug("Got match by tmTextUnitId: {}", textUnitForBatchImport.getTmTextUnitId());
            } else {
                List<TextUnitDTO> textUnitDTOSByName = nameToUsedTextUnitDTO.get(textUnitForBatchImport.getName());

                if (!textUnitDTOSByName.isEmpty()) {
                    if (textUnitDTOSByName.size() == 1) {
                        logger.debug("Unique match by name: {} and used", textUnitForBatchImport.getName());
                    } else {
                        logger.debug("There are multiple matches by name: {} and used, this will randomly select where the translation is " +
                                "added and must be avoided by providing tmTextUnitId in the import. Do this to not fail the" +
                                "import. Duplicated names can easily happen when working with branches (while without it should not)", textUnitForBatchImport.getName());
                    }

                    currentTextUnit = textUnitDTOSByName.remove(0);
                }

                if (currentTextUnit == null) {
                    List<TextUnitDTO> textUnitDTOSByNameUnused = nameToUnusedTextUnitDTO.get(textUnitForBatchImport.getName());

                    if (textUnitDTOSByNameUnused.size() == 1) {
                        logger.debug("Unique match by name: {} and unused", textUnitForBatchImport.getName());
                        currentTextUnit = textUnitDTOSByNameUnused.get(0);
                    }
                }
            }

            if (currentTextUnit != null) {
                if (mappedTmTextUnitIds.contains(currentTextUnit.getTmTextUnitId())) {
                    logger.debug("Text unit ({}) is already mapped, can't used it", currentTextUnit.getName());
                } else {
                    textUnitForBatchImport.setCurrentTextUnit(currentTextUnit);
                    mappedTmTextUnitIds.add(currentTextUnit.getTmTextUnitId());
                }
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
                logger.debug("Content can't be null, skip for name: {}", textUnitForBatchImport.getName());
            } else if (isUpdateNeeded(textUnitForBatchImport)) {
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
                        textUnitForBatchImport.getStatus(),
                        textUnitForBatchImport.isIncludedInLocalizedFile(),
                        importTime);

            } else {
                logger.debug("Update not needed, skip: {}", textUnitForBatchImport.getName());
            }
        }
    }

    boolean isUpdateNeeded(TextUnitForBatchImport textUnitForBatchImport) {

        TextUnitDTO currentTextUnit = textUnitForBatchImport.getCurrentTextUnit();

        return currentTextUnit.getTarget() == null || tmService.isUpdateNeededForTmTextUnitVariant(
                currentTextUnit.getStatus(),
                DigestUtils.md5Hex(currentTextUnit.getTarget()),
                currentTextUnit.isIncludedInLocalizedFile(),
                currentTextUnit.getComment(),
                textUnitForBatchImport.getStatus(),
                DigestUtils.md5Hex(textUnitForBatchImport.getContent()),
                textUnitForBatchImport.isIncludedInLocalizedFile(),
                textUnitForBatchImport.getComment());
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

    void applyIntegrityChecks(
            Asset asset,
            List<TextUnitForBatchImport> textUnitsForBatchImport,
            boolean keepStatusIfCheckFailedAndSameTarget) {

        Set<TextUnitIntegrityChecker> textUnitCheckers = integrityCheckerFactory.getTextUnitCheckers(asset);

        for (TextUnitForBatchImport textUnitForBatchImport : textUnitsForBatchImport) {

            TextUnitDTO currentTextUnit = textUnitForBatchImport.getCurrentTextUnit();

            if (currentTextUnit == null) {
                continue;
            }

            textUnitForBatchImport.setIncludedInLocalizedFile(true);
            textUnitForBatchImport.setStatus(APPROVED);

            for (TextUnitIntegrityChecker textUnitChecker : textUnitCheckers) {
                try {
                    textUnitChecker.check(currentTextUnit.getSource(), textUnitForBatchImport.getContent());
                } catch (IntegrityCheckException ice) {

                    boolean hasSameTarget = textUnitForBatchImport.getContent().equals(currentTextUnit.getTarget());

                    if (hasSameTarget && keepStatusIfCheckFailedAndSameTarget) {
                        textUnitForBatchImport.setIncludedInLocalizedFile(currentTextUnit.isIncludedInLocalizedFile());
                        textUnitForBatchImport.setStatus(currentTextUnit.getStatus());
                    } else {
                        textUnitForBatchImport.setIncludedInLocalizedFile(false);
                        textUnitForBatchImport.setStatus(Status.TRANSLATION_NEEDED);
                    }

                    break;
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

            if (textUnitDTO.getTarget() == null) {
                logger.debug("Skip: {}, target is null", textUnitDTO.getName());
                continue;
            }

            textUnitBatch.setLocale(locale);
            textUnitBatch.setTmTextUnitId(textUnitDTO.getTmTextUnitId());
            textUnitBatch.setName(textUnitDTO.getName());
            textUnitBatch.setContent(NormalizationUtils.normalize(textUnitDTO.getTarget()));
            textUnitBatch.setComment(textUnitDTO.getComment());
            textUnitBatch.setIncludedInLocalizedFile(textUnitDTO.isIncludedInLocalizedFile());
            textUnitBatch.setStatus(textUnitDTO.getStatus() == null ? APPROVED : textUnitDTO.getStatus());

            textUnitsForBatchImport.add(textUnitBatch);
        }

        return textUnitsForBatchImport;
    }

}
