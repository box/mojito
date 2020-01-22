package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyScreenshot;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.image.ImageService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.screenshot.ScreenshotRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TextUnitBatchMatcher;
import com.box.l10n.mojito.service.tm.TextUnitForBatchMatcher;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.utils.MergeFunctions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @author jeanaurambault
 */
@Component
public class ThirdPartyService {

    public enum Action {
        PUSH,
        PUSH_TRANSLATION,
        PULL,
        MAP_TEXTUNIT,
        PUSH_SCREENSHOT
    }

    static Logger logger = LoggerFactory.getLogger(ThirdPartyService.class);

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TextUnitBatchMatcher textUnitBatchMatcher;

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    @Autowired
    ScreenshotRepository screenshotRepository;

    @Autowired
    ThirdPartyScreenshotRepository thirdPartyScreenshotRepository;

    @Autowired
    ImageService imageService;

    @Autowired
    ThirdPartyTMS thirdPartyTMS;

    public PollableFuture asyncSyncMojitoWithThirdPartyTMS(Long repositoryId, String thirdPartyProjectId, List<Action> actions, String pluralSeparator, Map<String, String> localeMappings, List<String> options) {
        ThirdPartySyncJobInput thirdPartySyncJobInput = new ThirdPartySyncJobInput();

        thirdPartySyncJobInput.setRepositoryId(repositoryId);
        thirdPartySyncJobInput.setThirdPartyProjectId(thirdPartyProjectId);
        thirdPartySyncJobInput.setActions(actions);
        thirdPartySyncJobInput.setPluralSeparator(pluralSeparator);
        thirdPartySyncJobInput.setLocaleMappings(localeMappings);
        thirdPartySyncJobInput.setOptions(options);

        return quartzPollableTaskScheduler.scheduleJob(ThirdPartySyncJob.class, thirdPartySyncJobInput);
    }

    void syncMojitoWithThirdPartyTMS(Long repositoryId, String thirdPartyProjectId, List<Action> actions, String pluralSeparator, Map<String, String> localeMappings, List<String> options) {
        logger.debug("thirdparty TMS: {}", thirdPartyTMS);

        Repository repository = repositoryRepository.findOne(repositoryId);

        if (actions.contains(Action.PUSH)) {
            push(repository, thirdPartyProjectId, pluralSeparator, options);
        }
        if (actions.contains(Action.PUSH_TRANSLATION)) {
            pushTranslation(repository, thirdPartyProjectId, pluralSeparator, options, localeMappings);
        }
        if (actions.contains(Action.PULL)) {
            thirdPartyTMS.syncTranslations(repository, thirdPartyProjectId, pluralSeparator, options, localeMappings);
        }
        if (actions.contains(Action.MAP_TEXTUNIT)) {
            mapMojitoAndThirdPartyTextUnits(repository, thirdPartyProjectId);
        }
        if (actions.contains(Action.PUSH_SCREENSHOT)) {
            uploadScreenshotsAndCreateMappings(repository, thirdPartyProjectId);
        }
    }

    static final int BATCH_SIZE = 5004;

    void push(Repository repository, String thirdPartyProjectId, String pluralSeparator, List<String> options) {

        List<TextUnitDTO> textUnitDTOList;
        List<TextUnitDTO> batchTextUnitDTOList = new ArrayList<>(BATCH_SIZE);

        logger.info("Synchronize Mojito repository: {} with project: {}", repository.getName(), thirdPartyProjectId);

        int batchNumber = 0;
        textUnitDTOList = getSingularTextUnitList(repository, "en");
        for (TextUnitDTO textUnitDTO : textUnitDTOList) {
            batchTextUnitDTOList.add(textUnitDTO);

            if (batchTextUnitDTOList.size() % BATCH_SIZE == 0) {
                thirdPartyTMS.syncSources(repository, thirdPartyProjectId,
                        batchTextUnitDTOList, pluralSeparator, options, batchNumber++, true);
                batchTextUnitDTOList.clear();
            }
        }
        thirdPartyTMS.syncSources(repository, thirdPartyProjectId,
                batchTextUnitDTOList, pluralSeparator, options, batchNumber, true);
        batchTextUnitDTOList.clear();

        batchNumber = 0;
        textUnitDTOList = getPluralTextUnitList(repository, "en");
        for (TextUnitDTO textUnitDTO : textUnitDTOList) {
            batchTextUnitDTOList.add(textUnitDTO);

            if (batchTextUnitDTOList.size() % BATCH_SIZE == 0) {
                thirdPartyTMS.syncSources(repository, thirdPartyProjectId,
                        batchTextUnitDTOList, pluralSeparator, options, batchNumber++, false);
                batchTextUnitDTOList.clear();
            }
        }
        thirdPartyTMS.syncSources(repository, thirdPartyProjectId,
                batchTextUnitDTOList, pluralSeparator, options, batchNumber, false);
    }

    void pushTranslation(Repository repository, String thirdPartyProjectId, String pluralSeparator, List<String> options, Map<String, String> localeMappings) {

        List<TextUnitDTO> textUnitDTOList;
        List<TextUnitDTO> batchTextUnitDTOList = new ArrayList<>(BATCH_SIZE);

        for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
            String locale = repositoryLocale.getLocale().getBcp47Tag();

            int batchNumber = 0;
            textUnitDTOList = getSingularTextUnitList(repository, locale);
            for (TextUnitDTO textUnitDTO : textUnitDTOList) {
                batchTextUnitDTOList.add(textUnitDTO);

                if (batchTextUnitDTOList.size() % BATCH_SIZE == 0) {
                    thirdPartyTMS.uploadLocalizedFiles(repository, thirdPartyProjectId, locale,
                            batchTextUnitDTOList, pluralSeparator, options, localeMappings, batchNumber++, true);
                    batchTextUnitDTOList.clear();
                }
            }
            thirdPartyTMS.syncSources(repository, thirdPartyProjectId,
                    batchTextUnitDTOList, pluralSeparator, options, batchNumber, true);
            batchTextUnitDTOList.clear();

            batchNumber = 0;
            textUnitDTOList = getPluralTextUnitList(repository, locale);
            for (TextUnitDTO textUnitDTO : textUnitDTOList) {
                batchTextUnitDTOList.add(textUnitDTO);

                if (batchTextUnitDTOList.size() % BATCH_SIZE == 0) {
                    TextUnitSearcherParameters pluralTextUnitSearcherParameters = new TextUnitSearcherParameters()
                            .setRepositoryNames(Collections.singletonList(repository.getName()))
                            .setLocaleTags(Collections.singletonList(locale))
                            .setDoNotTranslateFilter(false)
                            .setSearchType(SearchType.EXACT)
                            .setPluralFormOther(textUnitDTOList.get(textUnitDTOList.size() - 1).getPluralFormOther());

                    Set<String> pluralFormSet = textUnitSearcher.search(pluralTextUnitSearcherParameters).stream()
                            .map(TextUnitDTO::getPluralForm)
                            .collect(Collectors.toSet());

                    textUnitDTOList = textUnitDTOList.stream()
                            .filter(item -> pluralFormSet.contains(item.getPluralForm()))
                            .collect(Collectors.toList());

                    thirdPartyTMS.uploadLocalizedFiles(repository, thirdPartyProjectId, locale,
                            batchTextUnitDTOList, pluralSeparator, options, localeMappings, batchNumber++,false);
                    batchTextUnitDTOList.clear();
                }
            }
            thirdPartyTMS.uploadLocalizedFiles(repository, thirdPartyProjectId, locale,
                    batchTextUnitDTOList, pluralSeparator, options, localeMappings, batchNumber,false);
        }
    }

    List<TextUnitDTO> getSingularTextUnitList(Repository repository, String locale) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters()
                .setRepositoryNames(Collections.singletonList(repository.getName()))
                .setLocaleTags(Collections.singletonList(locale))
                .setDoNotTranslateFilter(false)
                .setPluralFormsExcluded(true)
                .setSearchType(SearchType.ILIKE);

        return textUnitSearcher.search(textUnitSearcherParameters);
    }

    List<TextUnitDTO> getPluralTextUnitList(Repository repository, String locale) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters()
                .setRepositoryNames(Collections.singletonList(repository.getName()))
                .setLocaleTags(Collections.singletonList(locale))
                .setDoNotTranslateFilter(false)
                .setPluralFormsFiltered(false)
                .setPluralFormsExcluded(false)
                .setPluralFormOther("%")
                .setSearchType(SearchType.ILIKE);

        return textUnitSearcher.search(textUnitSearcherParameters);
    }

    void mapMojitoAndThirdPartyTextUnits(Repository repository, String projectId) {
        List<ThirdPartyTextUnit> thirdPartyTextUnits = thirdPartyTMS.getThirdPartyTextUnits(repository, projectId);

        LoadingCache<String, Optional<Asset>> assetCache = getAssetCache(repository);

        logger.debug("Get the third party ids that have been already mapped");
        Set<String> thirdPartyTextUnitIdsAlreadyMapped = thirdPartyTextUnitRepository.findThirdPartyIdsByRepository(repository);

        logger.debug("Batch by asset to optimize the import and exclude already mapped");
        Map<Asset, List<ThirdPartyTextUnit>> notMappedGroupedByAsset = thirdPartyTextUnits.stream().
                filter(t -> !thirdPartyTextUnitIdsAlreadyMapped.contains(t.getId())).
                collect(groupingBy(o -> assetCache.getUnchecked(o.getAssetPath()).orElse(null), LinkedHashMap::new, toList()));

        logger.debug("Perform mapping by asset (exclude null assset, that could appear if asset path didn't match)");
        notMappedGroupedByAsset.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .forEach(e -> mapThirdPartyTextUnitsToTextUnitDTOs(e.getKey(), e.getValue()));
    }

    /**
     * Plural forms are grouped together, that assume the third party TMS has a single entry for plural strings. This
     * will need to be changed to support 1:1 mapping for third party TMS that have multiple text units as Mojito
     *
     * @param asset
     * @param thirdPartyTextUnitsToMap
     */
    void mapThirdPartyTextUnitsToTextUnitDTOs(Asset asset, List<ThirdPartyTextUnit> thirdPartyTextUnitsToMap) {
        logger.debug("Map third party text units to text unit DTOs for asset: {}", asset.getId());
        List<TextUnitDTO> notMappedTextUnitTDOsForAsset = getNotMappedTextUnitTDOsForAsset(asset);

        Function<TextUnitForBatchMatcher, List<TextUnitDTO>> matchByNameAndPluralPrefix = textUnitBatchMatcher.matchByNameAndPluralPrefix(notMappedTextUnitTDOsForAsset);

        Map<ThirdPartyTextUnit, List<TextUnitDTO>> thirdPartyTextUnitToMojitoMap = thirdPartyTextUnitsToMap.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        tptu -> matchByNameAndPluralPrefix.apply(tptu),
                        MergeFunctions.keepLast(),
                        LinkedHashMap::new
                ));

        saveMojitoToThirdParthTextUnitMapping(asset, thirdPartyTextUnitToMojitoMap);
    }

    void saveMojitoToThirdParthTextUnitMapping(Asset asset, Map<ThirdPartyTextUnit, List<TextUnitDTO>> thirdPartyTextUnitToMojitoMap) {

        logger.debug("Create the entities for the mapping mojito to third party ");

        List<com.box.l10n.mojito.entity.ThirdPartyTextUnit> thirdPartyTextUnits = thirdPartyTextUnitToMojitoMap.entrySet().stream().flatMap(e -> {
            ThirdPartyTextUnit thirdPartyTextUnitForMapping = e.getKey();
            return e.getValue().stream().map(textUnitDTO -> {
                logger.debug("Create entity third party: {}, tm textunit: {}", thirdPartyTextUnitForMapping.getId(), textUnitDTO.getTmTextUnitId());
                com.box.l10n.mojito.entity.ThirdPartyTextUnit thirdPartyTextUnit = new com.box.l10n.mojito.entity.ThirdPartyTextUnit();
                thirdPartyTextUnit.setThirdPartyId(thirdPartyTextUnitForMapping.getId());
                thirdPartyTextUnit.setAsset(asset);
                TMTextUnit tmTextUnit = tmTextUnitRepository.getOne(textUnitDTO.getTmTextUnitId());
                thirdPartyTextUnit.setTmTextUnit(tmTextUnit);
                return thirdPartyTextUnit;
            });
        }).collect(toList());

        logger.debug("Save {} entities", thirdPartyTextUnits.size());
        thirdPartyTextUnitRepository.save(thirdPartyTextUnits);
    }

    void uploadScreenshotsAndCreateMappings(Repository repository, String projectId) {
        logger.debug("Get the screenshot that are not yet mapped");
        screenshotRepository.findUnmappedScreenshots(repository).stream().forEach(screenshot -> {
            getImage(screenshot).map(image -> {
                uploadImageWithMappings(projectId, screenshot, image);
                return image;
            }).orElseGet(() -> {
                logger.debug("Couldn't not fetch the image, ignore screenshot: {}", screenshot.getId());
                return null;
            });
        });
    }

    /**
     * We upload the image to the third party TMS only if it has some text unit mappings. Else it is not really
     * useful and it will also prevent sending images before the source text unit were processed and mapped since we
     * send the images only once.
     *
     * @param projectId
     * @param screenshot
     * @param image
     */
    void uploadImageWithMappings(String projectId, Screenshot screenshot, Image image) {
        logger.debug("uploadImageWithMappings, project id: {}, screenshot: {}", projectId, screenshot.getName());
        List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits = buildBaseThirdPartyImageToTextUnitsWithoutImageId(screenshot);

        if (!thirdPartyImageToTextUnits.isEmpty()) {
            logger.debug("There are text units to be mapped, upload image: {}", image.getName());

            ThirdPartyTMSImage thirdPartyTMSImage = thirdPartyTMS.uploadImage(projectId, image.getName(), image.getContent());

            ThirdPartyScreenshot thirdPartyScreenshot = new ThirdPartyScreenshot();
            thirdPartyScreenshot.setScreenshot(screenshot);
            thirdPartyScreenshot.setThirdPartyId(thirdPartyTMSImage.getId());

            logger.debug("Save screenshot mapping");
            thirdPartyScreenshotRepository.save(thirdPartyScreenshot);

            logger.debug("Set the image id on the mappings");
            thirdPartyImageToTextUnits.stream().forEach(t -> t.setImageId(thirdPartyScreenshot.getThirdPartyId()));

            thirdPartyTMS.createImageToTextUnitMappings(projectId, thirdPartyImageToTextUnits);
        } else {
            logger.debug("No text units to be mapped so we don't upload the image (might be late to perform the mapping and/or it" +
                    "is not usefull to send the image)");
        }
    }

    /**
     * Builds the base list of ThirdPartyImageToTextUnit for a screenshot without the imageId.
     * <p>
     * We want to upload image only if we have mapped text units (if not it is probably because the sync is delayed or
     * failed and so there is no point uploading the image) so we need to build that list first eventhough the imageId
     * is not available yet. The imageId will be set after the image has been uploaded
     *
     * @param screenshot
     * @return
     */
    List<ThirdPartyImageToTextUnit> buildBaseThirdPartyImageToTextUnitsWithoutImageId(Screenshot screenshot) {
        return screenshot.getScreenshotTextUnits().stream().map(screenshotTextUnit -> {
            logger.debug("Processing screenshotTextUnit: {}", screenshotTextUnit.getId());
            TMTextUnit tmTextUnit = screenshotTextUnit.getTmTextUnit();

            return Optional.ofNullable(thirdPartyTextUnitRepository.findByTmTextUnit(tmTextUnit))
                    .map(thirdPartyTextUnit -> {
                        logger.debug("thirdPartyTextUnit: {}, {}, {}",
                                screenshot.getName(),
                                thirdPartyTextUnit.getTmTextUnit().getId(),
                                thirdPartyTextUnit.getThirdPartyId());

                        ThirdPartyImageToTextUnit thirdPartyImageToTextUnit = new ThirdPartyImageToTextUnit();
                        thirdPartyImageToTextUnit.setTextUnitId(thirdPartyTextUnit.getThirdPartyId());
                        return thirdPartyImageToTextUnit;
                    }).orElseGet(() -> {
                        logger.debug("No third party text unit for tmTextUnit, mapping must have failed, skipping");
                        return null;
                    });
        }).filter(Objects::nonNull).sorted(comparing(ThirdPartyImageToTextUnit::getTextUnitId)).collect(toList());
    }

    /**
     * For now, we just support images stored in Mojito DB.
     * <p>
     * If stored somewhere else, we'd have to fetch them with http request, etc.
     *
     * @param screenshot
     * @return
     */
    Optional<Image> getImage(Screenshot screenshot) {
        logger.debug("Get image for screenhost: {} and src: {}", screenshot.getId(), screenshot.getSrc());

        if (screenshot.getSrc() == null) {
            logger.warn("Screenshot src is null for screenshot id: {}", screenshot.getId());
        }

        Optional<Image> image = Optional.ofNullable(screenshot.getSrc())
                .map(src -> src.replaceAll("^api/images/", ""))
                .map(name -> imageService.getImage(name));

        return image;
    }

    /**
     * Get TextUnitDTOs of the asset that are not mapped yet and are candidate for mapping.
     *
     * Already mapped entires can't be remapped since it would cause a containst issue in
     * {@link com.box.l10n.mojito.entity.ThirdPartyTextUnit#getTmTextUnit()}
     *
     * @param asset
     * @return
     */
    List<TextUnitDTO> getNotMappedTextUnitTDOsForAsset(Asset asset) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(asset.getRepository().getId());
        textUnitSearcherParameters.setAssetId(asset.getId());
        textUnitSearcherParameters.setForRootLocale(true);
        textUnitSearcherParameters.setPluralFormsFiltered(false);
        List<TextUnitDTO> all = textUnitSearcher.search(textUnitSearcherParameters);

        HashSet<Long> alreadyMapped = thirdPartyTextUnitRepository.findTmTextUnitIdsByAsset(asset);
        List<TextUnitDTO> notMapped = all.stream().filter(t -> !alreadyMapped.contains(t.getTmTextUnitId())).collect(toList());

        return notMapped;
    }

    LoadingCache<String, Optional<Asset>> getAssetCache(Repository repository) {
        logger.debug("Build asset cache");
        return CacheBuilder.newBuilder().build(
                CacheLoader.from(assetPath -> {
                    Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());
                    return Optional.ofNullable(asset);
                })
        );
    }
}


