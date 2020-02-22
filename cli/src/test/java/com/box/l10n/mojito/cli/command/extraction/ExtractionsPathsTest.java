package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.test.IOTestBase;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;


public class ExtractionsPathsTest extends IOTestBase {

    static final String outputDirectory = "someoutput";
    static final String extractName = "testExtract";

    @Test
    public void outputDirectory() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        Assert.assertEquals(Paths.get(outputDirectory), extractionsPaths.outputDirectory());
    }

    @Test
    public void extractionPath() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        Assert.assertEquals(Paths.get(outputDirectory, extractName), extractionsPaths.extractionPath(extractName));
    }

    @Test
    public void assetExtractionPath() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        Assert.assertEquals(
                Paths.get(outputDirectory, extractName, "LC_MESSAGES/messages.pot.json"),
                extractionsPaths.assetExtractionPath("LC_MESSAGES/messages.pot", extractName));
    }

    @Test
    public void sourceFileMatchPath() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        extractionsPaths.sourceFileMatchPath(Paths.get(outputDirectory, extractName, "LC_MESSAGES/messages.pot.json"), extractName);
    }

    @Test
    public void findAllAssetExtractionPaths() {
        File inputResourcesTestDir = getInputResourcesTestDir();
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(inputResourcesTestDir.getPath());

        List<Path> allAssetExtractionPaths = extractionsPaths.findAllAssetExtractionPaths(extractName);
        Assert.assertEquals(
                Sets.newHashSet(
                        inputResourcesTestDir.toPath().resolve(Paths.get("testExtract/LC_MESSAGES/messages.pot.json")),
                        inputResourcesTestDir.toPath().resolve(Paths.get("testExtract/LC_MESSAGES/messages2.pot.json")))
                ,
                new HashSet<>(allAssetExtractionPaths));
    }
}
