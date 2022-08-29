package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.test.IOTestBase;
import com.google.common.collect.Sets;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class ExtractionPathsTest extends IOTestBase {

  static final String outputDirectory = "someoutput";
  static final String extractName = "testExtract";

  @Test
  public void outputDirectory() {
    ExtractionPaths extractionPaths = new ExtractionPaths(outputDirectory, extractName);
    Assert.assertEquals(Paths.get(outputDirectory), extractionPaths.getOutputDirectory());
  }

  @Test
  public void extractionPath() {
    ExtractionPaths extractionPaths = new ExtractionPaths(outputDirectory, extractName);
    Assert.assertEquals(Paths.get(outputDirectory, extractName), extractionPaths.extractionPath());
  }

  @Test
  public void assetExtractionPath() {
    ExtractionPaths extractionPaths = new ExtractionPaths(outputDirectory, extractName);
    Assert.assertEquals(
        Paths.get(outputDirectory, extractName, "LC_MESSAGES/messages.pot.json"),
        extractionPaths.assetExtractionPath("LC_MESSAGES/messages.pot"));
  }

  @Test
  public void sourceFileMatchPath() {
    ExtractionPaths extractionPaths = new ExtractionPaths(outputDirectory, extractName);
    extractionPaths.sourceFileMatchPath(
        Paths.get(outputDirectory, extractName, "LC_MESSAGES/messages.pot.json"));
  }

  @Test
  public void findAllAssetExtractionPaths() {
    File inputResourcesTestDir = getInputResourcesTestDir();
    ExtractionPaths extractionPaths =
        new ExtractionPaths(inputResourcesTestDir.getPath(), extractName);

    Set<Path> allAssetExtractionPaths = extractionPaths.findAllAssetExtractionPaths();
    Assert.assertEquals(
        Sets.newHashSet(
            inputResourcesTestDir
                .toPath()
                .resolve(Paths.get("testExtract/LC_MESSAGES/messages.pot.json")),
            inputResourcesTestDir
                .toPath()
                .resolve(Paths.get("testExtract/LC_MESSAGES/messages2.pot.json"))),
        allAssetExtractionPaths);
  }
}
