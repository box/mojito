package com.box.l10n.mojito.service.appender;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import joptsimple.internal.Strings;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to append text units from translated branches under the assets repository to the source
 * asset if the --append-branch-text-units <id> flag is applied to the pull step. Text units in the
 * last push run are not appended as they already exist in the asset. Default append limit of 1000
 * exists to protect against any significant memory increases, this can be configured in the
 * application.properties.
 *
 * @author mattwilshire
 */
@Service
public class AssetAppenderService {

  static Logger logger = LoggerFactory.getLogger(AssetAppenderService.class);

  @Value("${l10n.asset-appender.default.limit:1000}")
  protected int DEFAULT_APPEND_LIMIT;

  private final AssetAppenderFactory assetAppenderFactory;
  private final BranchRepository branchRepository;
  private final BranchStatisticService branchStatisticService;
  private final PushRunRepository pushRunRepository;
  private final AppendedAssetBlobStorage appendedAssetBlobStorage;
  private final MeterRegistry meterRegistry;
  private final AssetAppenderConfig assetAppenderConfig;

  @Autowired
  public AssetAppenderService(
      AssetAppenderFactory assetAppenderFactory,
      BranchRepository branchRepository,
      BranchStatisticService branchStatisticService,
      PushRunRepository pushRunRepository,
      AppendedAssetBlobStorage appendedAssetBlobStorage,
      MeterRegistry meterRegistry,
      AssetAppenderConfig assetAppenderConfig) {
    this.assetAppenderFactory = assetAppenderFactory;
    this.branchRepository = branchRepository;
    this.branchStatisticService = branchStatisticService;
    this.pushRunRepository = pushRunRepository;
    this.appendedAssetBlobStorage = appendedAssetBlobStorage;
    this.meterRegistry = meterRegistry;
    this.assetAppenderConfig = assetAppenderConfig;
  }

  /**
   * Fetches text units under qualifying branches for appending, passes them through the equivalent
   * appender and returns the source asset.
   *
   * @param asset
   * @param appendJobId
   * @return Source content with text units appended from translated branches under the Assets
   *     repository.
   */
  public String appendBranchTextUnitsToSource(
      Asset asset, String appendJobId, String sourceContent) {

    if (Strings.isNullOrEmpty(appendJobId)) {
      logger.error(
          "Attempted to append branch text units with no append text unit job id supplied, returning asset source content.");
      return sourceContent;
    }

    // Use the appended source in the cache if it exists - this can happen if the pull command was
    // executed without the --parallel flag as the first localize asset request for the first
    // locale did the asset appending already. This is important to avoid the appender service being
    // called for each locale which would emit duplicate metrics and writes to the blob storage
    // again.
    Optional<String> cachedAppendedSource = appendedAssetBlobStorage.getAppendedSource(appendJobId);
    if (cachedAppendedSource.isPresent()) {
      return cachedAppendedSource.get();
    }

    String extension = FilenameUtils.getExtension(asset.getPath()).toLowerCase();
    Optional<AbstractAssetAppender> assetAppender =
        assetAppenderFactory.fromExtension(extension, sourceContent);

    if (assetAppender.isEmpty()) {
      logger.error(
          "Attempted to append branch text units to a source asset with an extension that did not map to a valid asset appender. No appending has taken place, returning original source asset.");
      return sourceContent;
    }

    AbstractAssetAppender appender = assetAppender.get();

    HashSet<Long> lastPushRunTextUnits =
        new HashSet<>(
            pushRunRepository.getAllTextUnitIdsFromLastPushRunByRepositoryId(
                asset.getRepository().getId()));

    // Fetch all branches to be appended, sorted by translated date in ascending order to ensure
    // older translated branches make it in
    List<Branch> branchesToAppend =
        branchRepository.findBranchesForAppending(asset.getRepository());

    List<Branch> appendedBranches = new ArrayList<>();

    int appendTextUnitLimit = DEFAULT_APPEND_LIMIT;

    // Retrieve the repo configured append limit if it exists
    if (assetAppenderConfig.getAssetAppender().containsKey(asset.getRepository().getName()))
      appendTextUnitLimit =
          assetAppenderConfig.getAssetAppender().get(asset.getRepository().getName()).getLimit();

    int appendedTextUnitCount = 0;
    for (Branch branch : branchesToAppend) {
      List<TextUnitDTO> textUnitsToAppend = branchStatisticService.getTextUnitDTOsForBranch(branch);

      // Filter text units by removing ones in the last push run
      textUnitsToAppend =
          textUnitsToAppend.stream()
              .filter(tu -> !lastPushRunTextUnits.contains(tu.getTmTextUnitId()))
              .toList();

      // Are we going to go over the hard limit ? If yes emit metrics and break out.
      if (appendedTextUnitCount + textUnitsToAppend.size() > appendTextUnitLimit) {
        int countTextUnitsFailedToAppend =
            branchesToAppend.stream()
                .skip(appendedBranches.size())
                .map(b -> branchStatisticService.getTextUnitDTOsForBranch(b).size())
                .mapToInt(Integer::intValue)
                .sum();

        logger.warn(
            "Asset text unit appending limit reached for asset '{}' in repository '{}' with job id '{}'. A total of {}/{} branches were appended to the asset. '{}' text units were appended to the asset successfully, whilst '{}' text units were not appended.",
            asset.getPath(),
            asset.getRepository().getName(),
            appendJobId,
            appendedBranches.size(),
            branchesToAppend.size(),
            appendedTextUnitCount,
            countTextUnitsFailedToAppend);

        // Log the amount of text units & branches we missed
        meterRegistry
            .counter(
                "AssetAppenderService.appendBranchTextUnitsToSource.exceededAppendLimitTextUnitCount",
                Tags.of("repository", asset.getRepository().getName(), "asset", asset.getPath()))
            .increment(countTextUnitsFailedToAppend);

        meterRegistry
            .counter(
                "AssetAppenderService.appendBranchTextUnitsToSource.exceededAppendLimitBranchCount",
                Tags.of("repository", asset.getRepository().getName(), "asset", asset.getPath()))
            .increment(branchesToAppend.size() - appendedBranches.size());
        break;
      }

      // Append the text units
      appender.appendTextUnits(textUnitsToAppend);

      appendedTextUnitCount += textUnitsToAppend.size();
      appendedBranches.add(branch);
    }

    String appendedAssetContent = appender.getAssetContent();

    logger.info(
        "Appended '{}' branches ('{}' text units) to asset '{}' for repository '{}' with append job id of '{}'.",
        appendedBranches.size(),
        appendedTextUnitCount,
        asset.getPath(),
        asset.getRepository().getName(),
        appendJobId);

    saveResults(appendJobId, appendedAssetContent, appendedBranches);

    meterRegistry
        .counter(
            "AssetAppenderService.appendBranchTextUnitsToSource.appendedTextUnitCount",
            Tags.of("repository", asset.getRepository().getName(), "asset", asset.getPath()))
        .increment(appendedTextUnitCount);

    meterRegistry
        .counter(
            "AssetAppenderService.appendBranchTextUnitsToSource.appendedBranchCount",
            Tags.of("repository", asset.getRepository().getName(), "asset", asset.getPath()))
        .increment(appendedBranches.size());

    return appendedAssetContent;
  }

  /**
   * Save results to block storage : S3 / DB. The branches will be used in the commit step to link
   * the branch to the commit that landed the appended text units.
   *
   * @param appendJobId Job id supplied by the -abtu param at the pull step.
   * @param appendedSourceContent Source content with appended text units.
   * @param appendedBranches The branches that were appended.
   */
  private void saveResults(
      String appendJobId, String appendedSourceContent, List<Branch> appendedBranches) {
    // Save the source content to blob storage
    appendedAssetBlobStorage.saveAppendedSource(appendJobId, appendedSourceContent);

    // Save branches to blob storage, will be serialized to a JSON string
    appendedAssetBlobStorage.saveAppendedBranches(
        appendJobId, appendedBranches.stream().map(BaseEntity::getId).toList());
  }
}
