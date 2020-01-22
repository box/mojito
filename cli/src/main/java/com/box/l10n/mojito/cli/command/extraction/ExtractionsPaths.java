package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.io.Files;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains logic to generate paths for local extractions and convert them back to original filenames
 */
public class ExtractionsPaths {

    public static final String DEFAULT_OUTPUT_DIRECTORY = ".mojito/extractions";
    public static final String DEFAULT_DIFF_FILENAME = "diff.json";
    static final String JSON_FILE_EXTENSION = ".json";

    String inputDirectory;
    String outputDirectory;
    String diffFileName;

    public ExtractionsPaths(String outputDirectory) {
        this(outputDirectory, null, null);
    }

    public ExtractionsPaths(String outputDirectory, String inputDirectory, String diffFileName) {
        Preconditions.checkNotNull(outputDirectory);
        this.outputDirectory = outputDirectory;
        this.inputDirectory = MoreObjects.firstNonNull(inputDirectory, outputDirectory);
        this.diffFileName = MoreObjects.firstNonNull(diffFileName, DEFAULT_DIFF_FILENAME);
    }

    public Path inputDirectory() {
        return Paths.get(inputDirectory);
    }

    public Path outputDirectory() {
        return Paths.get(outputDirectory);
    }

    public Path diffPath() {
        return Paths.get(outputDirectory, diffFileName);
    }

    Path extractionPath(String extractionName) {
        return Paths.get(outputDirectory, extractionName);
    }

    Path assetExtractionPath(FileMatch sourceFileMatch, String extractionName) {
        return Paths.get(extractionPath(extractionName).toString(), sourceFileMatch.getSourcePath() + JSON_FILE_EXTENSION);
    }

    String sourceFileMatchPath(Path extractionPath, String extractionName) {
        String relativePath = extractionPath(extractionName).relativize(extractionPath).toString();
        String withoutExtension = relativePath.substring(0, relativePath.length() - JSON_FILE_EXTENSION.length());
        return withoutExtension;
    }

    Set<String> sourceFileMatchPaths(List<Path> extractionPaths, String extractionName) {
        return extractionPaths.stream().map(p -> sourceFileMatchPath(p, extractionName)).collect(Collectors.toSet());
    }

    List<Path> findAllAssetExtractionPaths(String extractionName) {
        return Files.find(
                extractionPath(extractionName),
                100,
                (p, f) -> p.toString().endsWith(JSON_FILE_EXTENSION)).
                collect(Collectors.toList());
    }
}
