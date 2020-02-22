package com.box.l10n.mojito.cli.command.extraction;

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
     * @param extractionDiffsPaths
     * @param diffExtractionName
     * @return
     */
    public boolean hasAddedTextUnits(ExtractionDiffsPaths extractionDiffsPaths, String diffExtractionName) {
        boolean hasAddedTextUnits = extractionDiffsPaths.findAllAssetExtractionDiffPaths(diffExtractionName)
                .anyMatch(path -> {
                    AssetExtractionDiff assetExtractionDiff = objectMapper.readValueUnchecked(path.toFile(), AssetExtractionDiff.class);
                    return !assetExtractionDiff.getAddedTextunits().isEmpty();
                });

        return hasAddedTextUnits;
    }

    /**
     * Computes the difference between 2 local extractions designated by their names.
     *
     * @param currentAssetExtraction
     * @param baseAssetExtraction
     * @param diffExtractionName
     * @param extractionsPaths
     * @param extractionDiffsPaths
     * @throws MissingExtractionDirectoryExcpetion
     */
    public void computeAndWriteDiffs(
            String currentAssetExtraction,
            String baseAssetExtraction,
            String diffExtractionName,
            ExtractionsPaths extractionsPaths,
            ExtractionDiffsPaths extractionDiffsPaths) throws MissingExtractionDirectoryExcpetion {

        checkExtractionDirectoryExists(currentAssetExtraction, extractionsPaths);
        checkExtractionDirectoryExists(baseAssetExtraction, extractionsPaths);

        logger.debug("Process existing files in first extraction");
        Set<Path> assetExtractionPaths1 = Sets.newTreeSet(extractionsPaths.findAllAssetExtractionPaths(currentAssetExtraction));

        assetExtractionPaths1.forEach(assetExtractionPath1 -> {
            String sourceFileMatchPath = extractionsPaths.sourceFileMatchPath(assetExtractionPath1, currentAssetExtraction);
            Path assetExtractionPath2 = extractionsPaths.assetExtractionPath(sourceFileMatchPath, baseAssetExtraction);

            AssetExtraction assetExtraction1 = getAssetExtractionForPath(assetExtractionPath1);
            AssetExtraction assetExtraction2 = getAssetExtractionForPath(assetExtractionPath2);

            AssetExtractionDiff assetExtractionDiff;

            if (assetExtraction2 != null) {
                logger.debug("File in second extraction, compute added and removed text units");
                assetExtractionDiff = computeDiffBetweenExtractions(assetExtraction1, assetExtraction2);
            } else {
                logger.debug("No file in second extraction, just added text units");
                assetExtractionDiff = new AssetExtractionDiff();
                assetExtractionDiff.setAddedTextunits(assetExtraction1.getTextunits());
            }

            writeAssetExtractionDiff(diffExtractionName, extractionDiffsPaths, sourceFileMatchPath, assetExtractionDiff);
        });

        logger.debug("Process files from second extraction that are not in the first extraction, just removed text units");
        Set<Path> assetExtractionPaths2 = Sets.newTreeSet(extractionsPaths.findAllAssetExtractionPaths(baseAssetExtraction));
        assetExtractionPaths2.stream()
                .filter(assetExtractionPath2 -> {
                    String sourceFileMatchPath = extractionsPaths.sourceFileMatchPath(assetExtractionPath2, baseAssetExtraction);
                    Path assetExtractionPath1 = extractionsPaths.assetExtractionPath(sourceFileMatchPath, currentAssetExtraction);
                    return !assetExtractionPaths1.contains(assetExtractionPath1);
                })
                .forEach(assetExtractionPath -> {
                    AssetExtraction assetExtractionForPath = getAssetExtractionForPath(assetExtractionPath);
                    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
                    assetExtractionDiff.setRemovedTextunits(assetExtractionForPath.getTextunits());

                    String sourceFileMatchPath = extractionsPaths.sourceFileMatchPath(assetExtractionPath, baseAssetExtraction);
                    writeAssetExtractionDiff(diffExtractionName, extractionDiffsPaths, sourceFileMatchPath, assetExtractionDiff);
                });
    }

    void writeAssetExtractionDiff(String diffExtractionName, ExtractionDiffsPaths extractionDiffsPaths, String sourceFileMatchPath, AssetExtractionDiff assetExtractionDiff) {
        objectMapper.createDirectoriesAndWrite(
                extractionDiffsPaths.assetExtractionDiffPath(sourceFileMatchPath, diffExtractionName),
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

    AssetExtraction getAssetExtractionForPath(Path assetExtractionPath2) {
        AssetExtraction assetExtraction2 = null;

        if (assetExtractionPath2.toFile().exists()) {
            assetExtraction2 = objectMapper.readValueUnchecked(assetExtractionPath2.toFile(), AssetExtraction.class);
        }
        return assetExtraction2;
    }

    SortedSet<AssetExtractorTextUnit> getAssetExtractorTextUnitsAsSortedSet(List<AssetExtractorTextUnit> assetExtractorTextUnits) {
        SortedSet<AssetExtractorTextUnit> sortedSet = new TreeSet<>(AssetExtractorTextUnitComparators.BY_FILENAME_NAME_SOURCE_COMMENTS);
        sortedSet.addAll(assetExtractorTextUnits);
        return sortedSet;
    }

    void checkExtractionDirectoryExists(String extractionName, ExtractionsPaths extractionsPaths) throws MissingExtractionDirectoryExcpetion {
        Path extractionPath = extractionsPaths.extractionPath(extractionName);

        if (!extractionPath.toFile().exists()) {
            String msg = MessageFormat.format("There is no directory for extraction: {0}, can't compare", extractionName);
            throw new MissingExtractionDirectoryExcpetion(msg);
        }
    }

}
