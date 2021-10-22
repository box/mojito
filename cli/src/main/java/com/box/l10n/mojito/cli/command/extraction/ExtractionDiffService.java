package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.cli.command.checks.CliChecker;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Service to compute difference between local extractions
 */
@Component
public class ExtractionDiffService {

    static final String DIFF_JSON = "diff.json";

    /**
     * logger
     */
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
        boolean hasAddedTextUnits = extractionDiffPaths.findAllAssetExtractionDiffPaths()
                .anyMatch(path -> {
                    AssetExtractionDiff assetExtractionDiff = objectMapper.readValueUnchecked(path.toFile(), AssetExtractionDiff.class);
                    return !assetExtractionDiff.getAddedTextunits().isEmpty();
                });

        return hasAddedTextUnits;
    }

    /**
     * Compute basic statistics of a diff.
     *
     * @param extractionDiffPaths
     * @return
     * @throws MissingExtractionDirectoryExcpetion
     */
    public ExtractionDiffStatistics computeExtractionDiffStatistics(ExtractionDiffPaths extractionDiffPaths) throws MissingExtractionDirectoryExcpetion {

        ExtractionPaths baseExtractionPaths = extractionDiffPaths.getBaseExtractorPaths();
        ExtractionPaths currentExtractionPaths = extractionDiffPaths.getCurrentExtractorPaths();

        checkExtractionDirectoryExists(baseExtractionPaths);
        checkExtractionDirectoryExists(currentExtractionPaths);

        ExtractionDiffStatistics extractionDiffStatistics = ExtractionDiffStatistics.builder()
                .base(baseExtractionPaths.findAllAssetExtractionPaths().stream()
                        .map(this::getAssetExtractionForPath)
                        .mapToInt(assetExtraction -> assetExtraction.getTextunits().size())
                        .sum())
                .current(currentExtractionPaths.findAllAssetExtractionPaths().stream()
                        .map(this::getAssetExtractionForPath)
                        .mapToInt(assetExtraction -> assetExtraction.getTextunits().size())
                        .sum()
                )
                .build();

        extractionDiffStatistics = extractionDiffPaths.findAllAssetExtractionDiffPaths()
                .map(path -> objectMapper.readValueUnchecked(path.toFile(), AssetExtractionDiff.class))
                .map(assetExtractionDiff -> ExtractionDiffStatistics.builder()
                        .added(assetExtractionDiff.getAddedTextunits().size())
                        .removed(assetExtractionDiff.getRemovedTextunits().size())
                        .build())
                .reduce(extractionDiffStatistics, (c1, c2) -> c1.withAdded(c1.getAdded() + c2.getAdded()).withRemoved(c1.getRemoved() + c2.getRemoved()));

        return extractionDiffStatistics;
    }

    public List<AssetExtractionDiff> computeAssetExtractionDiffs(ExtractionDiffPaths extractionDiffPaths) throws MissingExtractionDirectoryExcpetion {
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        ExtractionPaths baseExtractionPaths = extractionDiffPaths.getBaseExtractorPaths();
        ExtractionPaths currentExtractionPaths = extractionDiffPaths.getCurrentExtractorPaths();

        checkExtractionDirectoryExists(baseExtractionPaths);
        checkExtractionDirectoryExists(currentExtractionPaths);

        logger.debug("Process existing files in the current extraction");
        Set<Path> currentAssetExtractionPaths = currentExtractionPaths.findAllAssetExtractionPaths();

        currentAssetExtractionPaths.forEach(currentAssetExtractionPath -> {
            String sourceFileMatchPath = currentExtractionPaths.sourceFileMatchPath(currentAssetExtractionPath);
            Path baseAssetExtractionPath = baseExtractionPaths.assetExtractionPath(sourceFileMatchPath);

            AssetExtraction currentAssetExtraction = getAssetExtractionForPath(currentAssetExtractionPath);
            AssetExtraction baseAssetExtraction = getAssetExtractionForPath(baseAssetExtractionPath);

            AssetExtractionDiff assetExtractionDiff;

            if (baseAssetExtraction != null) {
                logger.debug("File in base extraction, compute added and removed text units");
                assetExtractionDiff = computeDiffBetweenExtractions(currentAssetExtraction, baseAssetExtraction);
            } else {
                logger.debug("No file in base extraction, just added text units");
                assetExtractionDiff = new AssetExtractionDiff();
                assetExtractionDiff.setAddedTextunits(currentAssetExtraction.getTextunits());
            }

            assetExtractionDiffs.add(assetExtractionDiff);
        });

        logger.debug("Process files from the base that are not in the current extraction, just removed text units");
        Set<Path> baseAssetExtractionPaths = baseExtractionPaths.findAllAssetExtractionPaths();
        baseAssetExtractionPaths.stream()
                .filter(baseAssetExtractionPath -> {
                    String sourceFileMatchPath = baseExtractionPaths.sourceFileMatchPath(baseAssetExtractionPath);
                    Path currentAssetExtractionPath = currentExtractionPaths.assetExtractionPath(sourceFileMatchPath);
                    return !currentAssetExtractionPaths.contains(currentAssetExtractionPath);
                })
                .forEach(assetExtractionPath -> {
                    AssetExtraction assetExtractionForPath = getAssetExtractionForPath(assetExtractionPath);
                    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
                    assetExtractionDiff.setRemovedTextunits(assetExtractionForPath.getTextunits());
                    assetExtractionDiffs.add(assetExtractionDiff);
                });

        return assetExtractionDiffs;
    }


    /**
     * Computes the difference between 2 local extractions designated by their names.
     *
     * @param currentAssetExtraction
     * @param baseAssetExtraction
     * @param diffExtractionName
     * @param extractionDiffPaths
     * @throws MissingExtractionDirectoryExcpetion
     */
    public void computeAndWriteDiffs(ExtractionDiffPaths extractionDiffPaths) throws MissingExtractionDirectoryExcpetion {

        ExtractionPaths baseExtractionPaths = extractionDiffPaths.getBaseExtractorPaths();
        ExtractionPaths currentExtractionPaths = extractionDiffPaths.getCurrentExtractorPaths();

        checkExtractionDirectoryExists(baseExtractionPaths);
        checkExtractionDirectoryExists(currentExtractionPaths);

        logger.debug("Process existing files in the current extraction");
        Set<Path> currentAssetExtractionPaths = currentExtractionPaths.findAllAssetExtractionPaths();

        currentAssetExtractionPaths.forEach(currentAssetExtractionPath -> {
            String sourceFileMatchPath = currentExtractionPaths.sourceFileMatchPath(currentAssetExtractionPath);
            Path baseAssetExtractionPath = baseExtractionPaths.assetExtractionPath(sourceFileMatchPath);

            AssetExtraction currentAssetExtraction = getAssetExtractionForPath(currentAssetExtractionPath);
            AssetExtraction baseAssetExtraction = getAssetExtractionForPath(baseAssetExtractionPath);

            AssetExtractionDiff assetExtractionDiff;

            if (baseAssetExtraction != null) {
                logger.debug("File in base extraction, compute added and removed text units");
                assetExtractionDiff = computeDiffBetweenExtractions(currentAssetExtraction, baseAssetExtraction);
            } else {
                logger.debug("No file in base extraction, just added text units");
                assetExtractionDiff = new AssetExtractionDiff();
                assetExtractionDiff.setAddedTextunits(currentAssetExtraction.getTextunits());
            }

            writeAssetExtractionDiff(extractionDiffPaths, sourceFileMatchPath, assetExtractionDiff);
        });

        logger.debug("Process files from the base that are not in the current extraction, just removed text units");
        Set<Path> baseAssetExtractionPaths = baseExtractionPaths.findAllAssetExtractionPaths();
        baseAssetExtractionPaths.stream()
                .filter(baseAssetExtractionPath -> {
                    String sourceFileMatchPath = baseExtractionPaths.sourceFileMatchPath(baseAssetExtractionPath);
                    Path currentAssetExtractionPath = currentExtractionPaths.assetExtractionPath(sourceFileMatchPath);
                    return !currentAssetExtractionPaths.contains(currentAssetExtractionPath);
                })
                .forEach(assetExtractionPath -> {
                    AssetExtraction assetExtractionForPath = getAssetExtractionForPath(assetExtractionPath);
                    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
                    assetExtractionDiff.setRemovedTextunits(assetExtractionForPath.getTextunits());

                    String sourceFileMatchPath = baseExtractionPaths.sourceFileMatchPath(assetExtractionPath);
                    writeAssetExtractionDiff(extractionDiffPaths, sourceFileMatchPath, assetExtractionDiff);
                });

    }

    void writeAssetExtractionDiff(ExtractionDiffPaths extractionDiffPaths, String sourceFileMatchPath, AssetExtractionDiff assetExtractionDiff) {
        objectMapper.createDirectoriesAndWrite(
                extractionDiffPaths.assetExtractionDiffPath(sourceFileMatchPath),
                assetExtractionDiff);
    }

    AssetExtractionDiff computeDiffBetweenExtractions(AssetExtraction currentAssetExtraction, AssetExtraction baseAssetExtraction) {
        SortedSet<AssetExtractorTextUnit> currentSet = getAssetExtractorTextUnitsAsSortedSet(currentAssetExtraction.getTextunits());
        SortedSet<AssetExtractorTextUnit> baseSet = getAssetExtractorTextUnitsAsSortedSet(baseAssetExtraction.getTextunits());

        List<AssetExtractorTextUnit> added = new ArrayList<>(Sets.difference(currentSet, baseSet));
        List<AssetExtractorTextUnit> removed = new ArrayList<>(Sets.difference(baseSet, currentSet));

        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(added);
        assetExtractionDiff.setRemovedTextunits(removed);
        assetExtractionDiff.setCurrentFilterOptions(currentAssetExtraction.getFilterOptions());
        assetExtractionDiff.setCurrentFilterConfigIdOverride(currentAssetExtraction.getFilterConfigIdOverride());
        return assetExtractionDiff;
    }

    AssetExtraction getAssetExtractionForPath(Path assetExtractionPath) {
        AssetExtraction assetExtraction2 = null;

        if (assetExtractionPath.toFile().exists()) {
            assetExtraction2 = objectMapper.readValueUnchecked(assetExtractionPath.toFile(), AssetExtraction.class);
        }
        return assetExtraction2;
    }

    SortedSet<AssetExtractorTextUnit> getAssetExtractorTextUnitsAsSortedSet(List<AssetExtractorTextUnit> assetExtractorTextUnits) {
        SortedSet<AssetExtractorTextUnit> sortedSet = new TreeSet<>(AssetExtractorTextUnitComparators.BY_FILENAME_NAME_SOURCE_COMMENTS);
        sortedSet.addAll(assetExtractorTextUnits);
        return sortedSet;
    }

    void checkExtractionDirectoryExists(ExtractionPaths extractionPaths) throws MissingExtractionDirectoryExcpetion {
        Path extractionPath = extractionPaths.extractionPath();

        if (!extractionPath.toFile().exists()) {
            String msg = MessageFormat.format("There is no directory for extraction: {0}, can't compare", extractionPaths.getExtractionName());
            throw new MissingExtractionDirectoryExcpetion(msg);
        }
    }

}
