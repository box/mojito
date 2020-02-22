package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.io.Files;
import com.google.common.base.Preconditions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Contains logic to generate paths for extraction diffs and convert them back to original filenames
 */
public class ExtractionDiffsPaths {

    public static final String DEFAULT_OUTPUT_DIRECTORY = ".mojito/extraction-diffs";
    static final String JSON_FILE_EXTENSION = ".json";

    String outputDirectory;

    public ExtractionDiffsPaths(String outputDirectory) {
        Preconditions.checkNotNull(outputDirectory);
        this.outputDirectory = outputDirectory;
    }

    public Path outputDirectory() {
        return Paths.get(outputDirectory);
    }

    public Path extractionDiffPath(String diffExtractionName) {
        return Paths.get(outputDirectory, diffExtractionName);
    }

    public Path assetExtractionDiffPath(String sourceFileMatchPath, String diffExtractionName) {
        return Paths.get(outputDirectory, diffExtractionName, sourceFileMatchPath + JSON_FILE_EXTENSION);
    }

    public String sourceFileMatchPath(Path extractionDiffPath, String extractionDiffName) {
        String relativePath = extractionDiffPath(extractionDiffName).relativize(extractionDiffPath).toString();
        String withoutExtension = relativePath.substring(0, relativePath.length() - JSON_FILE_EXTENSION.length());
        return withoutExtension;
    }

    public Stream<Path> findAllAssetExtractionDiffPaths(String diffExtractionName) {
        return Files.find(
                extractionDiffPath(diffExtractionName),
                100,
                (p, f) -> p.toString().endsWith(JSON_FILE_EXTENSION));
    }
}
