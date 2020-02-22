package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.io.Files;
import com.google.common.base.Preconditions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains logic to generate paths for local extractions and convert them back to original filenames
 */
public class ExtractionsPaths {

    public static final String DEFAULT_OUTPUT_DIRECTORY = ".mojito/extractions";
    static final String JSON_FILE_EXTENSION = ".json";

    String outputDirectory;

    public ExtractionsPaths(String outputDirectory) {
        Preconditions.checkNotNull(outputDirectory);
        this.outputDirectory = outputDirectory;
    }

    public Path outputDirectory() {
        return Paths.get(outputDirectory);
    }

    Path extractionPath(String extractionName) {
        return Paths.get(outputDirectory, extractionName);
    }

    Path assetExtractionPath(String sourceFileMatchPath, String extractionName) {
        return Paths.get(extractionPath(extractionName).toString(), sourceFileMatchPath + JSON_FILE_EXTENSION);
    }

    String sourceFileMatchPath(Path extractionPath, String extractionName) {
        String relativePath = extractionPath(extractionName).relativize(extractionPath).toString();
        String withoutExtension = relativePath.substring(0, relativePath.length() - JSON_FILE_EXTENSION.length());
        return withoutExtension;
    }

    List<Path> findAllAssetExtractionPaths(String extractionName) {
        return Files.find(
                extractionPath(extractionName),
                100,
                (p, f) -> p.toString().endsWith(JSON_FILE_EXTENSION)).
                collect(Collectors.toList());
    }
}
