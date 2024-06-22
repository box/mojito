package com.box.l10n.mojito.service.thirdparty;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.thirdparty.ThirdPartySync;
import com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.image.ImageService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
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
import java.util.*;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author jeanaurambault
 */
@Component
public class ThirdPartyService {

  static Logger logger = LoggerFactory.getLogger(ThirdPartyService.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

  @Autowired AssetRepository assetRepository;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TextUnitBatchMatcher textUnitBatchMatcher;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired ScreenshotRepository screenshotRepository;

  @Autowired ThirdPartyScreenshotRepository thirdPartyScreenshotRepository;

  @Autowired LocaleMappingHelper localeMappingHelper;

  @Autowired TextUnitDTOsCacheService textUnitDTOsCacheService;

  @Autowired LocaleService localeService;

  @Autowired ImageService imageService;

  @Autowired ThirdPartyTMS thirdPartyTMS;

  @Value("${l10n.thirdPartyService.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String schedulerName;

  public void removeImage(String projectId, String imageId) {
    logger.debug(
        "remove image (screenshot) from Smartling, project id: {}, imageId: {}",
        projectId,
        imageId);
    thirdPartyTMS.removeImage(projectId, imageId);
  }

  public PollableFuture<Void> asyncSyncMojitoWithThirdPartyTMS(ThirdPartySync thirdPartySync) {
    ThirdPartySyncJobInput thirdPartySyncJobInput = new ThirdPartySyncJobInput();

    thirdPartySyncJobInput.setRepositoryId(thirdPartySync.getRepositoryId());
    thirdPartySyncJobInput.setThirdPartyProjectId(thirdPartySync.getProjectId());
    thirdPartySyncJobInput.setActions(thirdPartySync.getActions());
    thirdPartySyncJobInput.setPluralSeparator(thirdPartySync.getPluralSeparator());
    thirdPartySyncJobInput.setLocaleMapping(thirdPartySync.getLocaleMapping());
    thirdPartySyncJobInput.setSkipTextUnitsWithPattern(
        thirdPartySync.getSkipTextUnitsWithPattern());
    thirdPartySyncJobInput.setSkipAssetsWithPathPattern(
        thirdPartySync.getSkipAssetsWithPathPattern());
    thirdPartySyncJobInput.setIncludeTextUnitsWithPattern(
        thirdPartySync.getIncludeTextUnitsWithPattern());
    thirdPartySyncJobInput.setOptions(thirdPartySync.getOptions());

    return quartzPollableTaskScheduler.scheduleJobWithCustomTimeout(
        ThirdPartySyncJob.class,
        thirdPartySyncJobInput,
        schedulerName,
        thirdPartySync.getTimeout());
  }

  void syncMojitoWithThirdPartyTMS(
      Long repositoryId,
      String thirdPartyProjectId,
      List<ThirdPartySyncAction> actions,
      String pluralSeparator,
      String localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitsWithPattern,
      List<String> options,
      PollableTask currentTask) {
    logger.debug(
        "Thirdparty TMS Sync: repositoryId={} thirdPartyProjectId={} "
            + "actions={} pluralSeparator={} localeMapping={} "
            + "skipTextUnitsWithPattern={} skipAssetsWithPattern={} includeTextUnitsWithPattern={}"
            + "options={}",
        repositoryId,
        thirdPartyProjectId,
        actions,
        pluralSeparator,
        localeMapping,
        skipTextUnitsWithPattern,
        skipAssetsWithPathPattern,
        includeTextUnitsWithPattern,
        options);

    Repository repository = repositoryRepository.findById(repositoryId).orElse(null);

    if (actions.contains(ThirdPartySyncAction.PUSH)) {
      push(
          thirdPartyProjectId,
          pluralSeparator,
          skipTextUnitsWithPattern,
          skipAssetsWithPathPattern,
          options,
          repository,
          currentTask);
    }
    if (actions.contains(ThirdPartySyncAction.PUSH_TRANSLATION)) {
      pushTranslations(
          thirdPartyProjectId,
          pluralSeparator,
          localeMapping,
          skipTextUnitsWithPattern,
          skipAssetsWithPathPattern,
          includeTextUnitsWithPattern,
          options,
          repository,
          currentTask);
    }
    if (actions.contains(ThirdPartySyncAction.PULL_SOURCE)) {
      pullSource(thirdPartyProjectId, options, repository, localeMapping, currentTask);
    }
    if (actions.contains(ThirdPartySyncAction.PULL)) {
      pull(
          thirdPartyProjectId,
          pluralSeparator,
          localeMapping,
          skipTextUnitsWithPattern,
          skipAssetsWithPathPattern,
          options,
          repository,
          schedulerName,
          currentTask);
    }
    if (actions.contains(ThirdPartySyncAction.MAP_TEXTUNIT)) {
      mapMojitoAndThirdPartyTextUnits(
          repository, thirdPartyProjectId, pluralSeparator, options, currentTask);
    }
    if (actions.contains(ThirdPartySyncAction.PUSH_SCREENSHOT)) {
      uploadScreenshotsAndCreateMappings(repository, thirdPartyProjectId, currentTask);
    }
  }

  @Pollable(message = "Push source strings to third party service.")
  private void push(
      String thirdPartyProjectId,
      String pluralSeparator,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> options,
      Repository repository,
      @ParentTask PollableTask currentTask) {
    thirdPartyTMS.push(
        repository,
        thirdPartyProjectId,
        pluralSeparator,
        skipTextUnitsWithPattern,
        skipAssetsWithPathPattern,
        options);
  }

  @Pollable(message = "Push translations to third party service.")
  private void pushTranslations(
      String thirdPartyProjectId,
      String pluralSeparator,
      String localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitsWithPattern,
      List<String> options,
      Repository repository,
      @ParentTask PollableTask currentTask) {
    thirdPartyTMS.pushTranslations(
        repository,
        thirdPartyProjectId,
        pluralSeparator,
        parseLocaleMapping(localeMapping),
        skipTextUnitsWithPattern,
        skipAssetsWithPathPattern,
        includeTextUnitsWithPattern,
        options);
  }

  @Pollable(message = "Trigger translations pull from third party service.")
  private PollableFuture<Void> pull(
      String thirdPartyProjectId,
      String pluralSeparator,
      String localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> options,
      Repository repository,
      String schedulerName,
      @ParentTask PollableTask currentTask) {
    return thirdPartyTMS.pull(
        repository,
        thirdPartyProjectId,
        pluralSeparator,
        parseLocaleMapping(localeMapping),
        skipTextUnitsWithPattern,
        skipAssetsWithPathPattern,
        options,
        schedulerName,
        currentTask);
  }

  @Pollable(message = "Pull source text units from third party service.")
  private void pullSource(
      String thirdPartyProjectId,
      List<String> options,
      Repository repository,
      String localeMapping,
      @ParentTask PollableTask currentTask) {
    thirdPartyTMS.pullSource(
        repository, thirdPartyProjectId, options, parseLocaleMapping(localeMapping));
  }

  @Pollable(message = "Map Mojito and third party text units.")
  void mapMojitoAndThirdPartyTextUnits(
      Repository repository,
      String projectId,
      String pluralSeparator,
      List<String> options,
      @ParentTask PollableTask currentTask) {
    logger.debug(
        "Map text units from repository: {} with and projectId: {}",
        repository.getName(),
        projectId);

    logger.debug("Get the text units of the third party TMS");
    List<ThirdPartyTextUnit> thirdPartyTextUnits =
        thirdPartyTMS.getThirdPartyTextUnits(repository, projectId, options);

    logger.debug("Batch the third party text units by asset");
    LoadingCache<String, Optional<Asset>> assetCache = getAssetCache(repository);
    Map<Asset, List<ThirdPartyTextUnit>> thirdPartyTextUnitsByAsset =
        thirdPartyTextUnits.stream()
            .collect(
                groupingBy(
                    o ->
                        assetCache
                            .getUnchecked(o.getAssetPath())
                            .orElseThrow(
                                () ->
                                    new RuntimeException(
                                        "Trying to map a third party text unit for an asset (%s) that does not exist in the repository"
                                            .formatted(o.getAssetPath()))),
                    LinkedHashMap::new,
                    toList()));

    logger.debug(
        "Perform mapping by asset (exclude null asset, that could appear if asset path didn't match)");
    thirdPartyTextUnitsByAsset.entrySet().stream()
        .filter(e -> e.getKey() != null)
        .forEach(
            e -> mapThirdPartyTextUnitsToTextUnitDTOs(e.getKey(), e.getValue(), pluralSeparator));
  }

  void mapThirdPartyTextUnitsToTextUnitDTOs(
      Asset asset, List<ThirdPartyTextUnit> thirdPartyTextUnitsToMap, String pluralSeparator) {
    logger.debug("Map third party text units to text unit DTOs for asset: {}", asset.getId());
    Set<Long> alreadyMappedTmTextUnitId =
        thirdPartyTextUnitRepository.findTmTextUnitIdsByAsset(asset);

    boolean allWithTmTextUnitId =
        thirdPartyTextUnitsToMap.stream()
            .map(ThirdPartyTextUnit::getTmTextUnitId)
            .allMatch(Objects::nonNull);

    ImmutableList<TextUnitDTO> notMappedTextUnitDTOs;

    if (allWithTmTextUnitId) {
      logger.info("No need to map by name, put empty candidate list");
      notMappedTextUnitDTOs = ImmutableList.of();
    } else {
      logger.debug("Get all text units of the asset that are not mapped yet");
      notMappedTextUnitDTOs =
          textUnitDTOsCacheService
              .getTextUnitDTOsForAssetAndLocale(
                  asset.getId(), localeService.getDefaultLocale().getId(), true, UpdateType.ALWAYS)
              .stream()
              .filter(
                  textUnitDTO -> !alreadyMappedTmTextUnitId.contains(textUnitDTO.getTmTextUnitId()))
              .collect(ImmutableList.toImmutableList());
    }

    ImmutableMap<ThirdPartyTextUnit, ImmutableList<Long>> thirdPartyTextUnitToTmTextUnitIdMap =
        thirdPartyTextUnitsToMap.stream()
            .filter(t -> !alreadyMappedTmTextUnitId.contains(t.getTmTextUnitId()))
            .collect(
                ImmutableMap.toImmutableMap(
                    Function.identity(),
                    t ->
                        t.getTmTextUnitId() != null
                            ? ImmutableList.of(t.getTmTextUnitId())
                            : textUnitBatchMatcher
                                .matchByNameAndPluralPrefix(notMappedTextUnitDTOs, pluralSeparator)
                                .apply(t)
                                .stream()
                                .map(TextUnitDTO::getTmTextUnitId)
                                .collect(ImmutableList.toImmutableList())));

    saveMojitoToThirdPartyTextUnitMapping(asset, thirdPartyTextUnitToTmTextUnitIdMap);
  }

  void saveMojitoToThirdPartyTextUnitMapping(
      Asset asset,
      ImmutableMap<ThirdPartyTextUnit, ImmutableList<Long>> thirdPartyTextUnitToTmTextUnitIdMap) {

    HashMap<Long, String> tmTextUnitAlreadySaved = new LinkedHashMap<>();

    logger.debug("Create the entities for the mapping mojito to third party ");
    List<com.box.l10n.mojito.entity.ThirdPartyTextUnit> thirdPartyTextUnits =
        thirdPartyTextUnitToTmTextUnitIdMap.entrySet().stream()
            .flatMap(
                e -> {
                  ThirdPartyTextUnit thirdPartyTextUnitForMapping = e.getKey();
                  return e.getValue().stream()
                      .map(
                          tmTextUnitId -> {
                            logger.debug(
                                "Create entity third party text unit : {}, tm textunit: {}",
                                thirdPartyTextUnitForMapping.getId(),
                                tmTextUnitId);
                            com.box.l10n.mojito.entity.ThirdPartyTextUnit thirdPartyTextUnit =
                                new com.box.l10n.mojito.entity.ThirdPartyTextUnit();
                            thirdPartyTextUnit.setThirdPartyId(
                                thirdPartyTextUnitForMapping.getId());
                            thirdPartyTextUnit.setAsset(asset);
                            TMTextUnit tmTextUnit = tmTextUnitRepository.getOne(tmTextUnitId);

                            if (tmTextUnitAlreadySaved.containsKey(tmTextUnitId)) {
                              logger.warn(
                                  "There shouldn't be two matches, skip tmTextUnitId: {}, thirdPartyTextUnit: {} (previous: {})",
                                  tmTextUnitId,
                                  thirdPartyTextUnit.getThirdPartyId(),
                                  tmTextUnitAlreadySaved.get(tmTextUnitId));
                              return null;
                            } else {
                              tmTextUnitAlreadySaved.put(
                                  tmTextUnitId, thirdPartyTextUnitForMapping.getId());
                            }

                            thirdPartyTextUnit.setTmTextUnit(tmTextUnit);
                            return thirdPartyTextUnit;
                          });
                })
            .filter(Objects::nonNull)
            .collect(toList());

    logger.debug("Save {} entities", thirdPartyTextUnits.size());
    thirdPartyTextUnitRepository.saveAll(thirdPartyTextUnits);
  }

  @Pollable(message = "Upload screenshots to third party service and create mappings.")
  void uploadScreenshotsAndCreateMappings(
      Repository repository, String projectId, @ParentTask PollableTask currentTask) {
    logger.debug("Get the screenshot that are not yet mapped");
    screenshotRepository
        .findUnmappedScreenshots(repository)
        .forEach(
            screenshot -> {
              getImage(screenshot)
                  .map(
                      image -> {
                        uploadImageWithMappings(projectId, screenshot, image);
                        return image;
                      })
                  .orElseGet(
                      () -> {
                        logger.debug(
                            "Couldn't not fetch the image, ignore screenshot: {}",
                            screenshot.getId());
                        return null;
                      });
            });
  }

  /**
   * We upload the image to the third party TMS only if it has some text unit mappings. Else it is
   * not really useful and it will also prevent sending images before the source text unit were
   * processed and mapped since we send the images only once.
   *
   * @param projectId
   * @param screenshot
   * @param image
   */
  void uploadImageWithMappings(String projectId, Screenshot screenshot, Image image) {
    logger.debug(
        "uploadImageWithMappings, project id: {}, screenshot: {}", projectId, screenshot.getName());
    List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits =
        buildBaseThirdPartyImageToTextUnitsWithoutImageId(screenshot);

    if (!thirdPartyImageToTextUnits.isEmpty()) {
      logger.debug("There are text units to be mapped, upload image: {}", image.getName());

      try {
        ThirdPartyTMSImage thirdPartyTMSImage =
            thirdPartyTMS.uploadImage(projectId, image.getName(), image.getContent());

        ThirdPartyScreenshot thirdPartyScreenshot = new ThirdPartyScreenshot();
        thirdPartyScreenshot.setScreenshot(screenshot);
        thirdPartyScreenshot.setThirdPartyId(thirdPartyTMSImage.getId());

        logger.debug("Save screenshot mapping");
        thirdPartyScreenshotRepository.save(thirdPartyScreenshot);

        logger.debug("Set the image id on the mappings");
        thirdPartyImageToTextUnits.stream()
            .forEach(t -> t.setImageId(thirdPartyScreenshot.getThirdPartyId()));

        thirdPartyTMS.createImageToTextUnitMappings(projectId, thirdPartyImageToTextUnits);
      } catch (UnsupportedImageFormatException e) {
        logger.warn(e.getMessage());
      }

    } else {
      logger.debug(
          "No text units to be mapped so we don't upload the image (might be late to perform the mapping and/or it"
              + "is not useful to send the image)");
    }
  }

  /**
   * Builds the base list of ThirdPartyImageToTextUnit for a screenshot without the imageId.
   *
   * <p>We want to upload image only if we have mapped text units (if not it is probably because the
   * sync is delayed or failed and so there is no point uploading the image) so we need to build
   * that list first even though the imageId is not available yet. The imageId will be set after the
   * image has been uploaded
   *
   * @param screenshot
   * @return
   */
  List<ThirdPartyImageToTextUnit> buildBaseThirdPartyImageToTextUnitsWithoutImageId(
      Screenshot screenshot) {
    return screenshot.getScreenshotTextUnits().stream()
        .map(
            screenshotTextUnit -> {
              logger.debug("Processing screenshotTextUnit: {}", screenshotTextUnit.getId());
              TMTextUnit tmTextUnit = screenshotTextUnit.getTmTextUnit();

              return Optional.ofNullable(thirdPartyTextUnitRepository.findByTmTextUnit(tmTextUnit))
                  .map(
                      thirdPartyTextUnit -> {
                        logger.debug(
                            "thirdPartyTextUnit: {}, {}, {}",
                            screenshot.getName(),
                            thirdPartyTextUnit.getTmTextUnit().getId(),
                            thirdPartyTextUnit.getThirdPartyId());

                        ThirdPartyImageToTextUnit thirdPartyImageToTextUnit =
                            new ThirdPartyImageToTextUnit();
                        thirdPartyImageToTextUnit.setTextUnitId(
                            thirdPartyTextUnit.getThirdPartyId());
                        return thirdPartyImageToTextUnit;
                      })
                  .orElseGet(
                      () -> {
                        logger.debug(
                            "No third party text unit for tmTextUnit, mapping must have failed, skipping");
                        return null;
                      });
            })
        .filter(Objects::nonNull)
        .sorted(comparing(ThirdPartyImageToTextUnit::getTextUnitId))
        .collect(toList());
  }

  /**
   * For now, we just support images stored in Mojito DB.
   *
   * <p>If stored somewhere else, we'd have to fetch them with http request, etc.
   *
   * @param screenshot
   * @return
   */
  Optional<Image> getImage(Screenshot screenshot) {
    logger.debug(
        "Get image for screenhost: {} and src: {}", screenshot.getId(), screenshot.getSrc());

    if (screenshot.getSrc() == null) {
      logger.warn("Screenshot src is null for screenshot id: {}", screenshot.getId());
    }

    Optional<Image> image =
        Optional.ofNullable(screenshot.getSrc())
            .map(src -> src.replaceAll("^api/images/", ""))
            .flatMap(name -> imageService.getImage(name));

    return image;
  }

  LoadingCache<String, Optional<Asset>> getAssetCache(Repository repository) {
    logger.debug("Build asset cache");
    return CacheBuilder.newBuilder()
        .build(
            CacheLoader.from(
                assetPath -> {
                  Asset asset =
                      assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());
                  return Optional.ofNullable(asset);
                }));
  }

  Map<String, String> parseLocaleMapping(String input) {
    return Optional.ofNullable(
            localeMappingHelper.getInverseLocaleMapping(Strings.emptyToNull(input)))
        .orElseGet(Collections::emptyMap);
  }
}
