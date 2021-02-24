package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyScreenshot;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.thirdparty.ThirdPartySync;
import com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.image.ImageService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.screenshot.ScreenshotRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TextUnitBatchMatcher;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @author jeanaurambault
 */
@Component
public class ThirdPartyService {

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
    LocaleMappingHelper localeMappingHelper;

    @Autowired
    TextUnitDTOsCacheService textUnitDTOsCacheService;

    @Autowired
    LocaleService localeService;

    @Autowired
    ImageService imageService;

    @Autowired
    ThirdPartyTMS thirdPartyTMS;

    public PollableFuture<Void> asyncSyncMojitoWithThirdPartyTMS(ThirdPartySync thirdPartySync) {
        ThirdPartySyncJobInput thirdPartySyncJobInput = new ThirdPartySyncJobInput();

        thirdPartySyncJobInput.setRepositoryId(thirdPartySync.getRepositoryId());
        thirdPartySyncJobInput.setThirdPartyProjectId(thirdPartySync.getProjectId());
        thirdPartySyncJobInput.setActions(thirdPartySync.getActions());
        thirdPartySyncJobInput.setPluralSeparator(thirdPartySync.getPluralSeparator());
        thirdPartySyncJobInput.setLocaleMapping(thirdPartySync.getLocaleMapping());
        thirdPartySyncJobInput.setSkipTextUnitsWithPattern(thirdPartySync.getSkipTextUnitsWithPattern());
        thirdPartySyncJobInput.setSkipAssetsWithPathPattern(thirdPartySync.getSkipAssetsWithPathPattern());
        thirdPartySyncJobInput.setOptions(thirdPartySync.getOptions());

        return quartzPollableTaskScheduler.scheduleJob(ThirdPartySyncJob.class, thirdPartySyncJobInput);
    }

    void syncMojitoWithThirdPartyTMS(Long repositoryId,
                                     String thirdPartyProjectId,
                                     List<ThirdPartySyncAction> actions,
                                     String pluralSeparator,
                                     String localeMapping,
                                     String skipTextUnitsWithPattern,
                                     String skipAssetsWithPathPattern,
                                     List<String> options) {
        logger.debug("Thirdparty TMS Sync: repositoryId={} thirdPartyProjectId={} " +
                "actions={} pluralSeparator={} localeMapping={} " +
                "skipTextUnitsWithPattern={} skipAssetsWithPattern={} " +
                "options={}", repositoryId, thirdPartyProjectId, actions,
                pluralSeparator, localeMapping, skipTextUnitsWithPattern,
                skipAssetsWithPathPattern, options);

        Repository repository = repositoryRepository.findById(repositoryId).orElse(null);

        if (actions.contains(ThirdPartySyncAction.PUSH)) {
            thirdPartyTMS.push(repository, thirdPartyProjectId, pluralSeparator,
                    skipTextUnitsWithPattern, skipAssetsWithPathPattern, options);
        }
        if (actions.contains(ThirdPartySyncAction.PUSH_TRANSLATION)) {
            thirdPartyTMS.pushTranslations(repository, thirdPartyProjectId, pluralSeparator,
                    parseLocaleMapping(localeMapping),
                    skipTextUnitsWithPattern, skipAssetsWithPathPattern, options);
        }
        if (actions.contains(ThirdPartySyncAction.PULL)) {
            thirdPartyTMS.pull(repository, thirdPartyProjectId, pluralSeparator,
                    parseLocaleMapping(localeMapping),
                    skipTextUnitsWithPattern, skipAssetsWithPathPattern, options);
        }
        if (actions.contains(ThirdPartySyncAction.MAP_TEXTUNIT)) {
            mapMojitoAndThirdPartyTextUnits(repository, thirdPartyProjectId, pluralSeparator);
        }
        if (actions.contains(ThirdPartySyncAction.PUSH_SCREENSHOT)) {
            uploadScreenshotsAndCreateMappings(repository, thirdPartyProjectId);
        }
    }

    void mapMojitoAndThirdPartyTextUnits(Repository repository, String projectId, String pluralSeparator) {
        logger.debug("Map text units from repository: {} with and projectId: {}", repository.getName(), projectId);

        logger.debug("Get the text units of the third party TMS");
        List<ThirdPartyTextUnit> thirdPartyTextUnits = thirdPartyTMS.getThirdPartyTextUnits(repository, projectId);

        logger.debug("Batch the third party text units by asset");
        LoadingCache<String, Optional<Asset>> assetCache = getAssetCache(repository);
        Map<Asset, List<ThirdPartyTextUnit>> thirdPartyTextUnitsByAsset = thirdPartyTextUnits.stream().
                collect(groupingBy(o -> assetCache.getUnchecked(o.getAssetPath()).orElse(null), LinkedHashMap::new, toList()));

        logger.debug("Perform mapping by asset (exclude null asset, that could appear if asset path didn't match)");
        thirdPartyTextUnitsByAsset.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .forEach(e -> mapThirdPartyTextUnitsToTextUnitDTOs(e.getKey(), e.getValue(), pluralSeparator));
    }

    void mapThirdPartyTextUnitsToTextUnitDTOs(Asset asset, List<ThirdPartyTextUnit> thirdPartyTextUnitsToMap, String pluralSeparator) {
        logger.debug("Map third party text units to text unit DTOs for asset: {}", asset.getId());
        Set<Long> alreadyMappedTmTextUnitId = thirdPartyTextUnitRepository.findTmTextUnitIdsByAsset(asset);

        logger.debug("Get all text units of the asset that are not mapped yet");
        ImmutableList<TextUnitDTO> notMappedTextUnitDTOs = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocale(asset.getId(),
                localeService.getDefaultLocale().getId(), true, UpdateType.ALWAYS).stream()
                .filter(textUnitDTO -> !alreadyMappedTmTextUnitId.contains(textUnitDTO.getTmTextUnitId()))
                .collect(ImmutableList.toImmutableList());

        ImmutableMap<ThirdPartyTextUnit, List<TextUnitDTO>> thirdPartyTextUnitToMojitoMap = thirdPartyTextUnitsToMap.stream()
                .collect(ImmutableMap.toImmutableMap(
                        Function.identity(),
                        textUnitBatchMatcher.matchByNameAndPluralPrefix(notMappedTextUnitDTOs, pluralSeparator)::apply
                ));

        saveMojitoToThirdParthTextUnitMapping(asset, thirdPartyTextUnitToMojitoMap);
    }

    void saveMojitoToThirdParthTextUnitMapping(Asset asset, ImmutableMap<ThirdPartyTextUnit, List<TextUnitDTO>> thirdPartyTextUnitToMojitoMap) {

        logger.debug("Create the entities for the mapping mojito to third party ");

        List<com.box.l10n.mojito.entity.ThirdPartyTextUnit> thirdPartyTextUnits = thirdPartyTextUnitToMojitoMap.entrySet().stream().flatMap(e -> {
            ThirdPartyTextUnit thirdPartyTextUnitForMapping = e.getKey();
            return e.getValue().stream().map(textUnitDTO -> {
                logger.debug("Create entity third party text unit : {}, tm textunit: {}", thirdPartyTextUnitForMapping.getId(), textUnitDTO.getTmTextUnitId());
                com.box.l10n.mojito.entity.ThirdPartyTextUnit thirdPartyTextUnit = new com.box.l10n.mojito.entity.ThirdPartyTextUnit();
                thirdPartyTextUnit.setThirdPartyId(thirdPartyTextUnitForMapping.getId());
                thirdPartyTextUnit.setAsset(asset);
                TMTextUnit tmTextUnit = tmTextUnitRepository.getOne(textUnitDTO.getTmTextUnitId());
                thirdPartyTextUnit.setTmTextUnit(tmTextUnit);
                return thirdPartyTextUnit;
            });
        }).collect(toList());

        logger.debug("Save {} entities", thirdPartyTextUnits.size());
        thirdPartyTextUnitRepository.saveAll(thirdPartyTextUnits);
    }

    void uploadScreenshotsAndCreateMappings(Repository repository, String projectId) {
        logger.debug("Get the screenshot that are not yet mapped");
        screenshotRepository.findUnmappedScreenshots(repository).forEach(screenshot -> {
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

    LoadingCache<String, Optional<Asset>> getAssetCache(Repository repository) {
        logger.debug("Build asset cache");
        return CacheBuilder.newBuilder().build(
                CacheLoader.from(assetPath -> {
                    Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());
                    return Optional.ofNullable(asset);
                })
        );
    }

    Map<String, String> parseLocaleMapping(String input) {
        return Optional.ofNullable(localeMappingHelper.getInverseLocaleMapping(Strings.emptyToNull(input)))
                       .orElseGet(Collections::emptyMap);
    }
}


