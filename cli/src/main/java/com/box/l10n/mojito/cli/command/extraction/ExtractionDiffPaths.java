package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.io.Files;
import com.google.common.base.Preconditions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Contains logic to generate paths for extraction diffs and convert them back to original filenames
 */
public class ExtractionDiffPaths {

    public static final String DEFAULT_OUTPUT_DIRECTORY = ".mojito/extraction-diffs";
    static final String JSON_FILE_EXTENSION = ".json";

    private Path outputDirectory;
    private String diffExtractionName;
    private ExtractionPaths baseExtractorPaths;
    private ExtractionPaths currentExtractorPaths;

    private ExtractionDiffPaths(Path outputDirectory, String diffExtractionName, ExtractionPaths baseExtractionPaths, ExtractionPaths currentExtractionPath) {
        this.outputDirectory = Preconditions.checkNotNull(outputDirectory);
        this.diffExtractionName = Preconditions.checkNotNull(diffExtractionName);
        this.baseExtractorPaths = Preconditions.checkNotNull(baseExtractionPaths);
        this.currentExtractorPaths = Preconditions.checkNotNull(currentExtractionPath);
    }

    Path outputDirectory() {
        return outputDirectory;
    }

    Path extractionDiffPath() {
        return outputDirectory().resolve(diffExtractionName);
    }

    public Path assetExtractionDiffPath(String sourceFileMatchPath) {
        return extractionDiffPath().resolve(sourceFileMatchPath + JSON_FILE_EXTENSION);
    }

    public String sourceFileMatchPath(Path extractionDiffPath) {
        String relativePath = extractionDiffPath().relativize(extractionDiffPath).toString();
        String withoutExtension = relativePath.substring(0, relativePath.length() - JSON_FILE_EXTENSION.length());
        return withoutExtension;
    }

    public Stream<Path> findAllAssetExtractionDiffPaths() {
        return Files.find(
                extractionDiffPath(),
                100,
                (p, f) -> p.toString().endsWith(JSON_FILE_EXTENSION));
    }

    public String getDiffExtractionName() {
        return diffExtractionName;
    }

    public ExtractionPaths getBaseExtractorPaths() {
        return baseExtractorPaths;
    }

    public ExtractionPaths getCurrentExtractorPaths() {
        return currentExtractorPaths;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String outputDirectory;
        private String diffExtractionName;
        private ExtractionPaths baseExtractorPaths;
        private ExtractionPaths currentExtractorPaths;

        private Builder() {
        }

        public Builder outputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public Builder diffExtractionName(String diffExtractionName) {
            this.diffExtractionName = diffExtractionName;
            return this;
        }

        public Builder baseExtractorPaths(ExtractionPaths baseExtractorPaths) {
            this.baseExtractorPaths = baseExtractorPaths;
            return this;
        }

        public Builder currentExtractorPaths(ExtractionPaths currentExtractorPaths) {
            this.currentExtractorPaths = currentExtractorPaths;
            return this;
        }

        public ExtractionDiffPaths build() {
            Preconditions.checkNotNull(outputDirectory);
            Preconditions.checkNotNull(baseExtractorPaths);
            Preconditions.checkNotNull(currentExtractorPaths);

            if (diffExtractionName == null) {
                diffExtractionName = currentExtractorPaths.getExtractionName() + "_" + baseExtractorPaths.getExtractionName();
            }

            return new ExtractionDiffPaths(Paths.get(outputDirectory), diffExtractionName, baseExtractorPaths, currentExtractorPaths);
        }
    }
}
