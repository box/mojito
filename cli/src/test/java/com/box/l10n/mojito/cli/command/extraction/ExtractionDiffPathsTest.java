package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.test.IOTestBase;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class ExtractionDiffPathsTest extends IOTestBase {

    static final String outputDirectory = "someoutput";
    static final String extractionDiffName = "testExtractionDiffName";


    @Test
    public void outputDirectory() {
        ExtractionDiffPaths extractionDiffPaths = getExtractionDiffsPaths();
        Assert.assertEquals(Paths.get(outputDirectory), extractionDiffPaths.outputDirectory());
    }

    @Test
    public void extractionDiffPath() {
        ExtractionDiffPaths extractionDiffPaths = getExtractionDiffsPaths();
        Assert.assertEquals(Paths.get(outputDirectory, extractionDiffName), extractionDiffPaths.extractionDiffPath());
    }

    @Test
    public void assetExtractionDiffPath() {
        ExtractionDiffPaths extractionDiffPaths = getExtractionDiffsPaths();
        Assert.assertEquals(
                Paths.get(outputDirectory, extractionDiffName, "LC_MESSAGES/messages.pot.json"),
                extractionDiffPaths.assetExtractionDiffPath("LC_MESSAGES/messages.pot"));
    }

    @Test
    public void sourceFileMatchPath() {
        ExtractionDiffPaths extractionDiffPaths = getExtractionDiffsPaths();
        extractionDiffPaths.sourceFileMatchPath(Paths.get(outputDirectory, extractionDiffName, "LC_MESSAGES/messages.pot.json"));
    }

    @Test
    public void findAllAssetExtractionDiffPaths() {
        File inputResourcesTestDir = getInputResourcesTestDir();
        ExtractionDiffPaths extractionDiffPaths = ExtractionDiffPaths.builder()
                .outputDirectory(inputResourcesTestDir.getPath())
                .diffExtractionName(extractionDiffName)
                .baseExtractorPaths(new ExtractionPaths(outputDirectory, "base"))
                .currentExtractorPaths(new ExtractionPaths(outputDirectory, "current"))
                .build();

        Set<Path> allAssetExtractionPaths = extractionDiffPaths.findAllAssetExtractionDiffPaths().collect(Collectors.toSet());
        Assert.assertEquals(
                Sets.newHashSet(
                        inputResourcesTestDir.toPath().resolve(Paths.get("testExtractionDiffName/LC_MESSAGES/messages.pot.json")),
                        inputResourcesTestDir.toPath().resolve(Paths.get("testExtractionDiffName/LC_MESSAGES/messages2.pot.json"))),
                allAssetExtractionPaths);
    }

    @Test
    public void getDiffExtractionNameOrDefault() {
        ExtractionDiffPaths extractionDiffPaths = ExtractionDiffPaths.builder()
                .outputDirectory(outputDirectory)
                .baseExtractorPaths(new ExtractionPaths(outputDirectory, "base"))
                .currentExtractorPaths(new ExtractionPaths(outputDirectory, "current"))
                .build();
        Assert.assertEquals("current_base", extractionDiffPaths.getDiffExtractionName());
    }

    @Test
    public void getDiffExtractionNameOrDefaultWithName() {
        ExtractionDiffPaths extractionDiffPaths = ExtractionDiffPaths.builder()
                .diffExtractionName("provided")
                .outputDirectory(outputDirectory)
                .baseExtractorPaths(new ExtractionPaths(outputDirectory, "base"))
                .currentExtractorPaths(new ExtractionPaths(outputDirectory, "current"))
                .build();
        Assert.assertEquals("provided", extractionDiffPaths.getDiffExtractionName());
    }

    ExtractionDiffPaths getExtractionDiffsPaths() {
        return ExtractionDiffPaths.builder()
                .outputDirectory(outputDirectory)
                .diffExtractionName(extractionDiffName)
                .baseExtractorPaths(new ExtractionPaths(outputDirectory, "base"))
                .currentExtractorPaths(new ExtractionPaths(outputDirectory, "current"))
                .build();
    }
}
