package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.CommandHelper;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.io.Files;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.extractor.AssetExtractor;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ExtractionService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ExtractionService.class);

  @Autowired
  @Qualifier("outputIndented")
  ObjectMapper objectMapper;

  @Autowired CommandHelper commandHelper;

  @Autowired AssetExtractor assetExtractor;

  public void fileMatchToAssetExtractionAndSaveToJsonFile(
      ExtractionPaths extractionPaths,
      List<String> filterOptions,
      FilterConfigIdOverride filterConfigIdOverride,
      FileMatch sourceFileMatch)
      throws CommandException {

    AssetExtraction assetExtraction =
        fileMatchToAssetExtraction(
            extractionPaths.getExtractionName(),
            sourceFileMatch,
            filterOptions,
            filterConfigIdOverride);
    Path assetExtractionPath = extractionPaths.assetExtractionPath(sourceFileMatch.getSourcePath());
    objectMapper.createDirectoriesAndWrite(assetExtractionPath, assetExtraction);
  }

  public void recreateExtractionDirectory(ExtractionPaths extractionPaths) {
    logger.debug(
        "Delete the extraction directory for name: {}", extractionPaths.getExtractionName());
    Path path = extractionPaths.extractionPath();
    Files.deleteRecursivelyIfExists(path);
    Files.createDirectories(path);
  }

  AssetExtraction fileMatchToAssetExtraction(
      String extractionName,
      FileMatch sourceFileMatch,
      List<String> filterOptions,
      FilterConfigIdOverride filterConfigIdOverride)
      throws CommandException {
    List<AssetExtractorTextUnit> assetExtractorTextUnits =
        getExtractionTextUnitsForSourceFileMatch(sourceFileMatch, filterOptions);

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setTextunits(assetExtractorTextUnits);
    assetExtraction.setName(extractionName);
    assetExtraction.setFilterOptions(filterOptions);
    assetExtraction.setFilterConfigIdOverride(filterConfigIdOverride);

    return assetExtraction;
  }

  List<AssetExtractorTextUnit> getExtractionTextUnitsForSourceFileMatch(
      FileMatch sourceFileMatch, List<String> filterOptions) {
    String sourcePath = sourceFileMatch.getSourcePath();
    String assetContent = commandHelper.getFileContentWithXcodePatch(sourceFileMatch);
    FilterConfigIdOverride filterConfigIdOverride =
        sourceFileMatch.getFileType().getFilterConfigIdOverride();

    try {
      return assetExtractor.getAssetExtractorTextUnitsForAsset(
          sourcePath, assetContent, filterConfigIdOverride, filterOptions, null);
    } catch (UnsupportedAssetFilterTypeException uasft) {
      throw new RuntimeException("Source file match must be for a supported file type", uasft);
    }
  }
}
