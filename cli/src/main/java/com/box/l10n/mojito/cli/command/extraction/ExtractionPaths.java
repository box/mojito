package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.io.Files;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains logic to generate paths for local extractions and convert them back to original filenames
 */
public class ExtractionPaths {

    public static final String DEFAULT_OUTPUT_DIRECTORY = ".mojito/extractions";
    static final String JSON_FILE_EXTENSION = ".json";

    String outputDirectory;
    String extractionName;

    public ExtractionPaths(String outputDirectory, String extractionName) {
        this.outputDirectory = Preconditions.checkNotNull(outputDirectory);
        this.extractionName = Preconditions.checkNotNull(extractionName);
    }

    public Path getOutputDirectory() {
        return Paths.get(outputDirectory);
    }

    public String getExtractionName() {
        return extractionName;
    }

    Path extractionPath() {
        return getOutputDirectory().resolve(extractionName);
    }

    Path assetExtractionPath(String sourceFileMatchPath) {
        return extractionPath().resolve(sourceFileMatchPath + JSON_FILE_EXTENSION);
    }

    String sourceFileMatchPath(Path extractionPath) {
        String relativePath = extractionPath().relativize(extractionPath).toString();
        String withoutExtension = relativePath.substring(0, relativePath.length() - JSON_FILE_EXTENSION.length());
        return withoutExtension;
    }

    Set<Path> findAllAssetExtractionPaths() {
        return Files.find(
                extractionPath(),
                100,
                (p, f) -> p.toString().endsWith(JSON_FILE_EXTENSION)).
                collect(Collectors.toCollection(Sets::newTreeSet));
    }
}
