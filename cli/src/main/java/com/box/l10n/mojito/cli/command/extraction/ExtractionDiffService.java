package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import com.ibm.icu.text.MessageFormat;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Service to compute difference between local extractions */
@Component
public class ExtractionDiffService {

  static final String DIFF_JSON = "diff.json";

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ExtractionDiffService.class);

  @Autowired
  @Qualifier("outputIndented")
  ObjectMapper objectMapper;

  /**
   * Check if a diff has added text units in it.
   *
   * @param extractionDiffPaths
   * @param diffExtractionName
   * @return
   */
  public boolean hasAddedTextUnits(ExtractionDiffPaths extractionDiffPaths) {
    boolean hasAddedTextUnits =
        extractionDiffPaths
            .findAllAssetExtractionDiffPaths()
            .anyMatch(
                path -> {
                  AssetExtractionDiff assetExtractionDiff =
                      objectMapper.readValueUnchecked(path.toFile(), AssetExtractionDiff.class);
                  return !assetExtractionDiff.getAddedTextunits().isEmpty();
                });

    return hasAddedTextUnits;
  }

  /**
   * Compute basic statistics of a diff.
   *
   * @param extractionDiffPaths
   * @return
   * @throws MissingExtractionDirectoryException
   */
  public ExtractionDiffStatistics computeExtractionDiffStatistics(
      ExtractionDiffPaths extractionDiffPaths) throws MissingExtractionDirectoryException {

    ExtractionPaths baseExtractionPaths = extractionDiffPaths.getBaseExtractorPaths();
    ExtractionPaths currentExtractionPaths = extractionDiffPaths.getCurrentExtractorPaths();

    checkExtractionDirectoryExists(baseExtractionPaths);
    checkExtractionDirectoryExists(currentExtractionPaths);

    ExtractionDiffStatistics extractionDiffStatistics =
        ExtractionDiffStatistics.builder()
            .base(
                baseExtractionPaths.findAllAssetExtractionPaths().stream()
                    .map(this::getAssetExtractionForPath)
                    .mapToInt(assetExtraction -> assetExtraction.getTextunits().size())
                    .sum())
            .current(
                currentExtractionPaths.findAllAssetExtractionPaths().stream()
                    .map(this::getAssetExtractionForPath)
                    .mapToInt(assetExtraction -> assetExtraction.getTextunits().size())
                    .sum())
            .build();

    extractionDiffStatistics =
        extractionDiffPaths
            .findAllAssetExtractionDiffPaths()
            .map(path -> objectMapper.readValueUnchecked(path.toFile(), AssetExtractionDiff.class))
            .map(
                assetExtractionDiff ->
                    ExtractionDiffStatistics.builder()
                        .added(assetExtractionDiff.getAddedTextunits().size())
                        .addedStrings(
                            assetExtractionDiff.getAddedTextunits().stream()
                                .map(AssetExtractorTextUnit::getSource)
                                .collect(Collectors.toList()))
                        .removed(assetExtractionDiff.getRemovedTextunits().size())
                        .removedStrings(
                            assetExtractionDiff.getRemovedTextunits().stream()
                                .map(AssetExtractorTextUnit::getSource)
                                .collect(Collectors.toList()))
                        .build())
            .reduce(
                extractionDiffStatistics,
                (c1, c2) ->
                    c1.withAdded(c1.getAdded() + c2.getAdded())
                        .withRemoved(c1.getRemoved() + c2.getRemoved())
                        .withAddedStrings(joinLists(c1.getAddedStrings(), c2.getAddedStrings()))
                        .withRemovedStrings(
                            joinLists(c1.getRemovedStrings(), c2.getRemovedStrings())));

    return extractionDiffStatistics;
  }

  public List<AssetExtractionDiff> findAssetExtractionDiffsWithAddedTextUnits(
      ExtractionDiffPaths extractionDiffPaths) throws MissingExtractionDirectoryException {

    ExtractionPaths baseExtractionPaths = extractionDiffPaths.getBaseExtractorPaths();
    ExtractionPaths currentExtractionPaths = extractionDiffPaths.getCurrentExtractorPaths();

    checkExtractionDirectoryExists(baseExtractionPaths);
    checkExtractionDirectoryExists(currentExtractionPaths);

    return extractionDiffPaths
        .findAllAssetExtractionDiffPaths()
        .map(path -> objectMapper.readValueUnchecked(path.toFile(), AssetExtractionDiff.class))
        .filter(assetExtractionDiff -> !assetExtractionDiff.getAddedTextunits().isEmpty())
        .collect(Collectors.toList());
  }

  /**
   * Computes the difference between 2 local extractions designated by their names.
   *
   * @param currentAssetExtraction
   * @param baseAssetExtraction
   * @param diffExtractionName
   * @param extractionDiffPaths
   * @throws MissingExtractionDirectoryException
   */
  public void computeAndWriteDiffs(ExtractionDiffPaths extractionDiffPaths)
      throws MissingExtractionDirectoryException {

    ExtractionPaths baseExtractionPaths = extractionDiffPaths.getBaseExtractorPaths();
    ExtractionPaths currentExtractionPaths = extractionDiffPaths.getCurrentExtractorPaths();

    checkExtractionDirectoryExists(baseExtractionPaths);
    checkExtractionDirectoryExists(currentExtractionPaths);

    logger.debug("Process existing files in the current extraction");
    Set<Path> currentAssetExtractionPaths = currentExtractionPaths.findAllAssetExtractionPaths();

    currentAssetExtractionPaths.forEach(
        currentAssetExtractionPath -> {
          String sourceFileMatchPath =
              currentExtractionPaths.sourceFileMatchPath(currentAssetExtractionPath);
          Path baseAssetExtractionPath =
              baseExtractionPaths.assetExtractionPath(sourceFileMatchPath);

          AssetExtraction currentAssetExtraction =
              getAssetExtractionForPath(currentAssetExtractionPath);
          AssetExtraction baseAssetExtraction = getAssetExtractionForPath(baseAssetExtractionPath);

          AssetExtractionDiff assetExtractionDiff;

          if (baseAssetExtraction != null) {
            logger.debug("File in base extraction, compute added and removed text units");
            assetExtractionDiff =
                computeDiffBetweenExtractions(currentAssetExtraction, baseAssetExtraction);
          } else {
            logger.debug("No file in base extraction, just added text units");
            assetExtractionDiff = new AssetExtractionDiff();
            assetExtractionDiff.setAddedTextunits(currentAssetExtraction.getTextunits());
          }

          writeAssetExtractionDiff(extractionDiffPaths, sourceFileMatchPath, assetExtractionDiff);
        });

    logger.debug(
        "Process files from the base that are not in the current extraction, just removed text units");
    Set<Path> baseAssetExtractionPaths = baseExtractionPaths.findAllAssetExtractionPaths();
    baseAssetExtractionPaths.stream()
        .filter(
            baseAssetExtractionPath -> {
              String sourceFileMatchPath =
                  baseExtractionPaths.sourceFileMatchPath(baseAssetExtractionPath);
              Path currentAssetExtractionPath =
                  currentExtractionPaths.assetExtractionPath(sourceFileMatchPath);
              return !currentAssetExtractionPaths.contains(currentAssetExtractionPath);
            })
        .forEach(
            assetExtractionPath -> {
              AssetExtraction assetExtractionForPath =
                  getAssetExtractionForPath(assetExtractionPath);
              AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
              assetExtractionDiff.setRemovedTextunits(assetExtractionForPath.getTextunits());

              String sourceFileMatchPath =
                  baseExtractionPaths.sourceFileMatchPath(assetExtractionPath);
              writeAssetExtractionDiff(
                  extractionDiffPaths, sourceFileMatchPath, assetExtractionDiff);
            });
  }

  void writeAssetExtractionDiff(
      ExtractionDiffPaths extractionDiffPaths,
      String sourceFileMatchPath,
      AssetExtractionDiff assetExtractionDiff) {
    objectMapper.createDirectoriesAndWrite(
        extractionDiffPaths.assetExtractionDiffPath(sourceFileMatchPath), assetExtractionDiff);
  }

  AssetExtractionDiff computeDiffBetweenExtractions(
      AssetExtraction currentAssetExtraction, AssetExtraction baseAssetExtraction) {
    SortedSet<AssetExtractorTextUnit> currentSet =
        getAssetExtractorTextUnitsAsSortedSet(currentAssetExtraction.getTextunits());
    SortedSet<AssetExtractorTextUnit> baseSet =
        getAssetExtractorTextUnitsAsSortedSet(baseAssetExtraction.getTextunits());

    List<AssetExtractorTextUnit> added = new ArrayList<>(Sets.difference(currentSet, baseSet));
    List<AssetExtractorTextUnit> removed = new ArrayList<>(Sets.difference(baseSet, currentSet));

    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(added);
    assetExtractionDiff.setRemovedTextunits(removed);
    assetExtractionDiff.setCurrentFilterOptions(currentAssetExtraction.getFilterOptions());
    assetExtractionDiff.setCurrentFilterConfigIdOverride(
        currentAssetExtraction.getFilterConfigIdOverride());
    return assetExtractionDiff;
  }

  AssetExtraction getAssetExtractionForPath(Path assetExtractionPath) {
    AssetExtraction assetExtraction2 = null;

    if (assetExtractionPath.toFile().exists()) {
      assetExtraction2 =
          objectMapper.readValueUnchecked(assetExtractionPath.toFile(), AssetExtraction.class);
    }
    return assetExtraction2;
  }

  SortedSet<AssetExtractorTextUnit> getAssetExtractorTextUnitsAsSortedSet(
      List<AssetExtractorTextUnit> assetExtractorTextUnits) {
    SortedSet<AssetExtractorTextUnit> sortedSet =
        new TreeSet<>(AssetExtractorTextUnitComparators.BY_FILENAME_NAME_SOURCE_COMMENTS);
    sortedSet.addAll(assetExtractorTextUnits);
    return sortedSet;
  }

  void checkExtractionDirectoryExists(ExtractionPaths extractionPaths)
      throws MissingExtractionDirectoryException {
    Path extractionPath = extractionPaths.extractionPath();

    if (!extractionPath.toFile().exists()) {
      String msg =
          MessageFormat.format(
              "There is no directory for extraction: {0}, can't compare",
              extractionPaths.getExtractionName());
      throw new MissingExtractionDirectoryException(msg);
    }
  }

  protected <T> List<T> joinLists(List<T> list1, List<T> list2) {
    List<T> joinedList = new ArrayList<>();
    if (list1 != null && list1.size() > 0) {
      joinedList.addAll(list1);
    }
    if (list2 != null && list2.size() > 0) {
      joinedList.addAll(list2);
    }
    return joinedList;
  }
}
