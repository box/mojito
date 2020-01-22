package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.json.ObjectMapper;
import com.google.common.collect.Sets;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * Computes the difference between 2 local extractions designated by their names.
     *
     * @param extractionName1
     * @param extractionName2
     * @param ExtractionsPaths
     * @throws MissingExtractionDirectoryExcpetion
     */
    public void computeExtractionDiffAndSaveToJsonFile(String extractionName1,
                                                       String extractionName2,
                                                       ExtractionsPaths extractionsPaths) throws MissingExtractionDirectoryExcpetion {
        Diff diff = computeExtractionDiff(extractionName1, extractionName2, extractionsPaths);
        Path diffPath = extractionsPaths.diffPath();
        objectMapper.createDirectoriesAndWrite(diffPath, diff);
    }

    Diff computeExtractionDiff(String extractionName1, String extractionName2, ExtractionsPaths extractionsPaths) throws MissingExtractionDirectoryExcpetion {
        logger.debug("Compute differences between extraction: {} and {}", extractionName1, extractionName2);

        checkExtractionDirectoryExists(extractionName2, extractionsPaths);
        checkExtractionDirectoryExists(extractionName1, extractionsPaths);

        List<Path> assetExtraction1Paths = extractionsPaths.findAllAssetExtractionPaths(extractionName1);
        List<Path> assetExtraction2Paths = extractionsPaths.findAllAssetExtractionPaths(extractionName2);

        Set<String> extraction1SourceFileNames = extractionsPaths.sourceFileMatchPaths(assetExtraction1Paths, extractionName1);
        Set<String> extraction2SourceFileNames = extractionsPaths.sourceFileMatchPaths(assetExtraction2Paths, extractionName2);

        Sets.SetView<String> addedFilenames = Sets.difference(extraction1SourceFileNames, extraction2SourceFileNames);
        Sets.SetView<String> removedFilenames = Sets.difference(extraction2SourceFileNames, extraction1SourceFileNames);

        if (!addedFilenames.isEmpty()) {
            addedFilenames.forEach(p -> logger.debug("Added files: {}", p));
        }

        if (!removedFilenames.isEmpty()) {
            removedFilenames.forEach(p -> logger.debug("Removed files: {}", p));
        }

        logger.debug("Same file names, check the file content");

        Set<TextUnitWithAssetPath> extraction1TextUnits = getTextUnitsFromFile(assetExtraction1Paths, extractionName1, extractionsPaths);
        Set<TextUnitWithAssetPath> extraction2TextUnits = getTextUnitsFromFile(assetExtraction2Paths, extractionName2, extractionsPaths);
        Sets.SetView<TextUnitWithAssetPath> addedTextUnitWithFile = Sets.difference(extraction1TextUnits, extraction2TextUnits);
        Sets.SetView<TextUnitWithAssetPath> removedTextUnitWithFile = Sets.difference(extraction2TextUnits, extraction1TextUnits);

        Diff diff = new Diff();
        diff.setAddedFiles(addedFilenames);
        diff.setRemovedFiles(removedFilenames);
        diff.setAddedTextUnits(addedTextUnitWithFile);
        diff.setRemovedTextUnits(removedTextUnitWithFile);

        return diff;
    }

    void checkExtractionDirectoryExists(String extractionName, ExtractionsPaths extractionsPaths) throws MissingExtractionDirectoryExcpetion {
        Path extractionPath = extractionsPaths.extractionPath(extractionName);

        if (!extractionPath.toFile().exists()) {
            String msg = MessageFormat.format("There is no directory for extraction: {0}, can't compare", extractionName);
            throw new MissingExtractionDirectoryExcpetion(msg);
        }
    }

    Set<TextUnitWithAssetPath> getTextUnitsFromFile(List<Path> assetExtractionPaths, String extractionName, ExtractionsPaths extractionsPaths) {
        return assetExtractionPaths.stream().map(assetExtractionPath -> {
            AssetExtraction assetExtraction = objectMapper.readValueUnchecked(assetExtractionPath.toFile(), AssetExtraction.class);
            return assetExtraction.getTextUnits().stream().map(t -> {
                TextUnitWithAssetPath textUnitWithFile = new TextUnitWithAssetPath();
                textUnitWithFile.setName(t.getName());
                textUnitWithFile.setSource(t.getSource());
                textUnitWithFile.setComments(t.getComments());
                textUnitWithFile.setAssetPath(extractionsPaths.sourceFileMatchPath(assetExtractionPath, extractionName));
                return textUnitWithFile;
            });
        }).flatMap(Function.identity()).collect(
                Collectors.toCollection(
                        () -> new TreeSet<>(TextUnitComparators.BY_FILENAME_NAME_SOURCE_COMMENTS)
                )
        );
    }

}
