package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.*;

import com.box.l10n.mojito.test.IOTestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** @author jaurambault */
public class CommandDirectoriesTest extends IOTestBase {

  @Test
  public void testRelativizeWithSourceDirectory() {

    Path path = Paths.get("source/sub/file1");
    CommandDirectories instance = new CommandDirectories("source", "target");
    Path expResult = Paths.get("sub/file1");
    Path result = instance.relativizeWithSourceDirectory(path);

    assertEquals(expResult, result);
  }

  @Test
  public void testRelativizeWithTargetDirectory() {

    Path path = Paths.get("target/sub/file1");
    CommandDirectories instance = new CommandDirectories("source", "target");
    Path expResult = Paths.get("sub/file1");
    Path result = instance.relativizeWithTargetDirectory(path);

    assertEquals(expResult, result);
  }

  @Test
  public void testRelativizeWithUserDirectory() {

    Path path = Paths.get("target/sub/file1");
    CommandDirectories instance = new CommandDirectories("source", "target");
    Path expResult = Paths.get("target/sub/file1");
    Path result = instance.relativizeWithUserDirectory(path);

    assertEquals(expResult, result);
  }

  @Test
  public void testResolveWithTargetDirectory() {

    Path pathInSourceDirectory = Paths.get("source/sub/file1");
    CommandDirectories instance = new CommandDirectories("source", "target");
    Path expResult = Paths.get("target/sub/file1");
    Path result = instance.resolveWithTargetDirectory(pathInSourceDirectory);

    assertEquals(expResult, result);
  }

  @Test
  public void testResolveWithTargetDirectoryAndCreateParentDirectories() throws Exception {

    Path fileInSourceDirectory = Paths.get(getInputResourcesTestDir().toString(), "sub/file1");
    CommandDirectories instance =
        new CommandDirectories(
            getInputResourcesTestDir().toString(), getTargetTestDir().toString());
    Path expResult =
        getBaseDir().toPath().relativize(Paths.get(getTargetTestDir().toString(), "sub/file1"));
    Path result =
        instance.resolveWithTargetDirectoryAndCreateParentDirectories(fileInSourceDirectory);

    assertEquals(expResult, result);

    Files.write(result, new byte[0]);

    checkExpectedGeneratedResources();
  }

  @Test
  public void testListFilesWithExtensionInSourceDirectory() throws Exception {
    String extension = "xliff";
    CommandDirectories instance =
        new CommandDirectories(
            getInputResourcesTestDir().toString(), getTargetTestDir().toString());
    List<Path> expResult = new ArrayList<>();

    Collections.sort(expResult);

    expResult.add(Paths.get(getInputResourcesTestDir().toString(), "file.xliff"));
    expResult.add(Paths.get(getInputResourcesTestDir().toString(), "sub/file.xliff"));

    List<Path> result = instance.listFilesWithExtensionInSourceDirectory(extension);

    Collections.sort(result);

    assertEquals(expResult, result);
  }

  @Test
  public void testBuildGlobPatternForFileWithExtension() {
    CommandDirectories commandDirectories = new CommandDirectories(null);
    String expected = "glob:**.{jpg,JPG}";
    String buildGlobPatternForFileWithExtension =
        commandDirectories.buildGlobPatternForFileWithExtensions("jpg");
    assertEquals(expected, buildGlobPatternForFileWithExtension);
  }

  @Test
  public void testBuildGlobPatternForFileWithExtensionMultiple() {
    CommandDirectories commandDirectories = new CommandDirectories(null);
    String expected = "glob:**.{jpg,JPG,png,PNG}";
    String buildGlobPatternForFileWithExtension =
        commandDirectories.buildGlobPatternForFileWithExtensions("jpg", "png");
    assertEquals(expected, buildGlobPatternForFileWithExtension);
  }
}
