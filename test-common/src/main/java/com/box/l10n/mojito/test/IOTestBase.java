package com.box.l10n.mojito.test;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Function;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOTestBase {

  final String TEST_RESOURCES_DIR = "src/test/resources/";
  final String TEST_RESOURCES_TARGET_DIR = "target/test-output/";
  final String TEST_RESOURCES_EXPECTED_DIR = "expected/";
  final String TEST_RESOURCES_INPUT_DIR = "input/";

  /** logger */
  static Logger logger = LoggerFactory.getLogger(IOTestBase.class);

  /** Test source directory, contains resource for the test */
  protected File sourceTestDir;

  /** Test target directory, to store output file during testing */
  protected File targetTestDir;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  /** Creates the test directories to easily compare input/output files with expected files. */
  @Before
  public void initializeTestDirectories() {
    logger.debug("Initialize test directories");
    String testId = testIdWatcher.getTestId();

    // append _IO avoid collision class name with package name - caused issue with AspectJ
    String ioDir = testIdWatcher.getSlashClassName() + "_IO/" + testIdWatcher.getMethodName();

    File baseDir = getBaseDir();
    File resourcesDir = new File(baseDir, TEST_RESOURCES_DIR);

    sourceTestDir = new File(resourcesDir, ioDir);
    targetTestDir = new File(baseDir, TEST_RESOURCES_TARGET_DIR + testId);

    logger.debug("Delete test target directory to remove data from previous run");

    try {
      FileUtils.deleteDirectory(targetTestDir);
    } catch (IOException e) {
      throw new RuntimeException(
          "Problem deleting directory: " + targetTestDir.getAbsolutePath(), e);
    }

    targetTestDir.mkdirs();
  }

  /**
   * When running test in Intellij, "basedir" is not available and returns null.
   *
   * @return
   */
  protected File getBaseDir() {
    logger.debug("Get \"basedir\" property (must be set by Maven)");
    String basedir = System.getProperty("basedir");

    if (basedir == null) {
      logger.debug(
          "Missing \"basedir\" to run tests (normally set by Maven). Assuming Intellij test run, look for env. variable");
      basedir = System.getenv().get("basedir");

      if (basedir == null) {
        logger.warn(
            "Missing \"basedir\" to run tests (normally set by Maven). Fallback to \"user.dir\" but if tests fails "
                + "set env. variable \"basedir=$MODULE_DIR$\" in run configuration");
        basedir = System.getProperty("user.dir");
      }
    }

    return new File(basedir);
  }

  /**
   * Options to override expected test files.
   *
   * <p>Test can be run that way: mvn test -DoverrideExpectedTestFiles=true or
   * System.setProperty("overrideExpectedTestFiles", "true");
   *
   * @return
   */
  protected boolean shouldOverrideExpectedTestFiles() {
    String overrideExpectedTestFiles = System.getProperty("overrideExpectedTestFiles");
    return overrideExpectedTestFiles == null ? false : Boolean.valueOf(overrideExpectedTestFiles);
  }

  /**
   * Options to create input files.
   *
   * <p>Test can be run that way: mvn test -DcreateInputTestFiles=true or temporarilly:
   * System.setProperty("createInputTestFiles", "true");
   *
   * @return
   */
  protected boolean shouldCreateInputTestFiles() {
    String createInputTestFiles = System.getProperty("createInputTestFiles");
    return createInputTestFiles == null ? false : Boolean.valueOf(createInputTestFiles);
  }

  /**
   * Gets the directory that contains expected resources.
   *
   * @return
   */
  protected File getExpectedResourcesTestDir() {
    return new File(sourceTestDir, TEST_RESOURCES_EXPECTED_DIR);
  }

  /**
   * Gets the directory that contains input resources for the test
   *
   * @return the directory that contains input resources for the test
   */
  protected File getInputResourcesTestDir() {
    return new File(sourceTestDir, TEST_RESOURCES_INPUT_DIR);
  }

  /**
   * Gets the subdirectory in the input directory for the test.
   *
   * @param subDirectory name of a sub directory in the input directory
   * @return the subdirectory in the input directory for the test
   */
  protected File getInputResourcesTestDir(String subDirectory) {
    return new File(sourceTestDir, TEST_RESOURCES_INPUT_DIR + "/" + subDirectory);
  }

  /**
   * Gets the directory that contains the output of the test.
   *
   * @return the directory that contains the output of the test.
   */
  protected File getTargetTestDir() {
    return targetTestDir;
  }

  /**
   * Gets the subdirectory in the target directory for the test and creates it if it doesn't exist.
   *
   * @param subDirectory name of sub directory in the target directory
   * @return the subdirectory in the target directory for the test
   */
  protected File getTargetTestDir(String subDirectory) {

    File file = new File(targetTestDir, "/" + subDirectory);

    if (!file.exists()) {
      file.mkdirs();
    }

    return file;
  }

  /**
   * Modify all files in the target directory (output of the test) by applying the function on the
   * content.
   *
   * @param fileContentModifier to apply to file content to modify the file
   * @throws IOException
   */
  public void modifyFilesInTargetTestDirectory(Function<String, String> fileContentModifier)
      throws IOException {
    modifyFilesInTargetTestDirectory(fileContentModifier, null);
  }

  public void modifyFilesInTargetTestDirectory(
      Function<String, String> fileContentModifier, String regex) throws IOException {

    Collection<File> files = FileUtils.listFiles(getTargetTestDir(), null, true);

    for (File file : files) {
      if (regex != null && !file.getName().matches(regex)) {
        continue;
      }
      String fileContent = Files.toString(file, StandardCharsets.UTF_8);
      String modifiedFileContent = fileContentModifier.apply(fileContent);
      Files.write(modifiedFileContent, file, StandardCharsets.UTF_8);
    }
  }

  /**
   * Asserts that the resources generated by the test exactly match the resources located in the
   * expected directory
   */
  public void checkExpectedGeneratedResources() {
    try {
      if (shouldOverrideExpectedTestFiles()) {
        logger.info("Override expected test files (instead of checking)");
        try {
          FileUtils.deleteDirectory(getExpectedResourcesTestDir());
          FileUtils.copyDirectory(targetTestDir, getExpectedResourcesTestDir());
        } catch (IOException io) {
          throw new RuntimeException(io);
        }
      }

      checkDirectoriesContainSameContent(getExpectedResourcesTestDir(), targetTestDir);
    } catch (DifferentDirectoryContentException e) {
      String msg =
          "Generated resources do not match the expected resource (Use -DoverrideExpectedTestFiles=true "
              + "to run test and override expected test files instead of doing this check)";
      logger.debug(msg, e);
      Assert.fail(msg + "\n" + e.getMessage());
    }
  }

  /**
   * Checks that the two directories have the same structure and that their files content are the
   * same
   *
   * @param dir1
   * @param dir2
   * @throws DifferentDirectoryContentException if the two directories are different
   */
  protected void checkDirectoriesContainSameContent(File dir1, File dir2)
      throws DifferentDirectoryContentException {

    try {
      Collection<File> listFiles1 = FileUtils.listFiles(dir1, null, true);
      Collection<File> listFiles2 = FileUtils.listFiles(dir2, null, true);

      // Get all the files inside the source directory, recursively
      for (File file1 : listFiles1) {

        Path relativePath1 = dir1.toPath().relativize(file1.toPath());

        logger.debug("Test file: {}", relativePath1);
        File file2 = new File(dir2, relativePath1.toString());

        // Check if the file exists in the other directory
        if (!file2.exists()) {
          throw new DifferentDirectoryContentException(
              "File: " + file2.toString() + " doesn't exist");
        }

        // If that's a file, check that the both files have the same content
        if (file2.isFile() && !Files.equal(file1, file2)) {
          throw new DifferentDirectoryContentException(
              "File: "
                  + file1.toString()
                  + " and file: "
                  + file2.toString()
                  + " have different content."
                  + "\n\nfile1 content:\n\n"
                  + FileUtils.readFileToString(file1, StandardCharsets.UTF_8)
                  + "\n\nfile2 content:\n\n"
                  + FileUtils.readFileToString(file2, StandardCharsets.UTF_8));
        }
      }

      if (listFiles1.size() < listFiles2.size()) {
        List<Path> listPath1 =
            listFiles1.stream()
                .map(file -> dir1.toPath().relativize(file.toPath()))
                .collect(toList());
        String extraFiles =
            listFiles2.stream()
                .map(file -> dir2.toPath().relativize(file.toPath()))
                .filter(path -> !listPath1.contains(path))
                .map(Path::toString)
                .collect(joining("\n"));
        throw new DifferentDirectoryContentException("Additional files in dir2:\n" + extraFiles);
      } else {
        logger.debug("Same file list size");
      }

    } catch (IOException e) {
      throw new DifferentDirectoryContentException("Failed to compare ", e);
    }
  }

  /**
   * Writes the given content into a file in given directory.
   *
   * @param directory directory that will contain the file
   * @param fileName the filename
   * @param content file content
   */
  public void writeToFileInDirectory(File directory, String fileName, String content) {

    try {
      Files.write(content, new File(directory, fileName), StandardCharsets.UTF_8);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Read a test input resource.
   *
   * @param path resource path in {@link #getInputResourcesTestDir()}
   * @return the resource content, content must be utf-8
   */
  public String readInputResource(String path) {

    try {
      File inputFile = getInputResourcesTestDir().toPath().resolve(path).toFile();

      if (!inputFile.exists()) {
        logger.info(
            "Missing test input file. mvn test -DcreateInputTestFiles=true "
                + "or temporarilly: System.setProperty(\"createInputTestFiles\", \"true\");");
        if (shouldCreateInputTestFiles()) {
          inputFile.getParentFile().mkdirs();
          Files.write("<replace with test data>", inputFile, StandardCharsets.UTF_8);
        }
      }

      return Files.toString(inputFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Returns the path of the given file, within the target test directory
   *
   * @param fileName
   * @return
   */
  public String getPathForFileInTargetDir(String fileName) {
    File targetFile = new File(targetTestDir, fileName);
    return targetFile.getPath();
  }

  protected static class DifferentDirectoryContentException extends Exception {

    public DifferentDirectoryContentException(String message) {
      super(message);
    }

    public DifferentDirectoryContentException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
