package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.test.IOTestBase;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ExtractionsPathsTest extends IOTestBase {

    static final String outputDirectory = "someoutput";
    static final String extractName = "testExtract";

    @Test
    public void outputDirectory() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        Assert.assertEquals(Paths.get(outputDirectory), extractionsPaths.outputDirectory());
    }

    @Test
    public void inputDirectory() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        Assert.assertEquals(
                "If not provided the input directory will be the same as the output directory",
                Paths.get(outputDirectory),
                extractionsPaths.inputDirectory());
    }

    @Test
    public void diffPath() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        Assert.assertEquals(Paths.get(outputDirectory, "diff.json"), extractionsPaths.diffPath());
    }

    @Test
    public void extractionPath() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        Assert.assertEquals(Paths.get(outputDirectory, extractName), extractionsPaths.extractionPath(extractName));
    }

    @Test
    public void assetExtractionPath() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);

        FileMatch sourceFileMatch = Mockito.mock(FileMatch.class);
        Mockito.when(sourceFileMatch.getSourcePath()).thenReturn("LC_MESSAGES/messages.pot");

        Assert.assertEquals(
                Paths.get(outputDirectory, extractName, "LC_MESSAGES/messages.pot.json"),
                extractionsPaths.assetExtractionPath(sourceFileMatch, extractName));
    }

    @Test
    public void sourceFileMatchPath() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        extractionsPaths.sourceFileMatchPath(Paths.get(outputDirectory, extractName, "LC_MESSAGES/messages.pot.json"), extractName);
    }

    @Test
    public void sourceFileMatchPaths() {
        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectory);
        Set<String> sourceFileMatchPaths = extractionsPaths.sourceFileMatchPaths(
                Arrays.asList(
                        Paths.get(outputDirectory, extractName, "LC_MESSAGES/messages.pot.json"),
                        Paths.get(outputDirectory, extractName, "LC_MESSAGES/messages2.pot.json")),
                extractName
        );

        Assert.assertEquals(Sets.newHashSet("LC_MESSAGES/messages.pot", "LC_MESSAGES/messages2.pot"), sourceFileMatchPaths);
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
