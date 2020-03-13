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

public class ExtractionDiffsPathsTest extends IOTestBase {

    static final String outputDirectory = "someoutput";
    static final String extractionDiffName = "testExtractionDiffName";

    @Test
    public void outputDirectory() {
        ExtractionDiffsPaths extractionDiffsPaths = new ExtractionDiffsPaths(outputDirectory);
        Assert.assertEquals(Paths.get(outputDirectory), extractionDiffsPaths.outputDirectory());
    }

    @Test
    public void extractionDiffPath() {
        ExtractionDiffsPaths extractionDiffsPaths = new ExtractionDiffsPaths(outputDirectory);
        Assert.assertEquals(Paths.get(outputDirectory, extractionDiffName), extractionDiffsPaths.extractionDiffPath(extractionDiffName));
    }

    @Test
    public void assetExtractionDiffPath() {
        ExtractionDiffsPaths extractionDiffsPaths = new ExtractionDiffsPaths(outputDirectory);
        Assert.assertEquals(
                Paths.get(outputDirectory, extractionDiffName, "LC_MESSAGES/messages.pot.json"),
                extractionDiffsPaths.assetExtractionDiffPath("LC_MESSAGES/messages.pot", extractionDiffName));

    }

    @Test
    public void sourceFileMatchPath() {
        ExtractionDiffsPaths extractionDiffsPaths = new ExtractionDiffsPaths(outputDirectory);
        extractionDiffsPaths.sourceFileMatchPath(Paths.get(outputDirectory, extractionDiffName, "LC_MESSAGES/messages.pot.json"), extractionDiffName);
    }

    @Test
    public void findAllAssetExtractionDiffPaths() {
        File inputResourcesTestDir = getInputResourcesTestDir();
        ExtractionDiffsPaths extractionDiffsPaths = new ExtractionDiffsPaths(inputResourcesTestDir.getPath());

        Set<Path> allAssetExtractionPaths = extractionDiffsPaths.findAllAssetExtractionDiffPaths(extractionDiffName).collect(Collectors.toSet());
        Assert.assertEquals(
                Sets.newHashSet(
                        inputResourcesTestDir.toPath().resolve(Paths.get("testExtractionDiffName/LC_MESSAGES/messages.pot.json")),
                        inputResourcesTestDir.toPath().resolve(Paths.get("testExtractionDiffName/LC_MESSAGES/messages2.pot.json")))
                ,
                allAssetExtractionPaths);
    }
}
