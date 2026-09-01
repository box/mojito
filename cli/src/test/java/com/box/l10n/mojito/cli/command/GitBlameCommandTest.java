package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.filefinder.file.AndroidStringsFileType;
import com.box.l10n.mojito.cli.filefinder.file.ChromeExtensionJSONFileType;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.POFileType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.entity.GitBlame;
import com.box.l10n.mojito.rest.entity.GitBlameWithUsage;
import com.box.l10n.mojito.service.gitblame.GitBlameService;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author emagalindan
 */
public class GitBlameCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(GitBlameCommandTest.class);

  static final String BLAME_AUTHOR_NAME = "Git Blame Test";
  static final String BLAME_AUTHOR_EMAIL = "git-blame-test@example.com";
  static final long BLAME_COMMIT_TIME_MILLIS = 1_537_568_049_000L;

  @Autowired GitBlameService gitBlameService;

  @Test
  public void android() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    CommittedGitRepo sourceRepo = createTempGitRepoFrom(getInputResourcesTestDir("source"));

    logger.debug("Source directory is [{}]", sourceRepo.directory.getAbsoluteFile());
    getL10nJCommander()
        .run("push", "-r", repository.getName(), "-s", sourceRepo.directory.getAbsolutePath());

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repository.getId());
    List<com.box.l10n.mojito.service.gitblame.GitBlameWithUsage> gitBlameWithUsages =
        gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);

    for (com.box.l10n.mojito.service.gitblame.GitBlameWithUsage gitBlameWithUsage :
        gitBlameWithUsages) {
      assertNull(gitBlameWithUsage.getGitBlame());
    }

    getL10nJCommander()
        .run(
            "git-blame",
            "-r",
            repository.getName(),
            "-s",
            sourceRepo.directory.getAbsolutePath());

    gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);
    verifyGitBlame(gitBlameWithUsages, sourceRepo.gitBlame);
  }

  @Test
  public void poFile() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    CommittedGitRepo sourceRepo = createTempGitRepoForPoFile();

    logger.debug("Source directory is [{}]", sourceRepo.directory.getAbsoluteFile());
    getL10nJCommander()
        .run("push", "-r", repository.getName(), "-s", sourceRepo.directory.getAbsolutePath());

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repository.getId());
    List<com.box.l10n.mojito.service.gitblame.GitBlameWithUsage> gitBlameWithUsages =
        gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);

    for (com.box.l10n.mojito.service.gitblame.GitBlameWithUsage gitBlameWithUsage :
        gitBlameWithUsages) {
      assertNull(gitBlameWithUsage.getGitBlame());
    }

    L10nJCommander l10nJCommander = getL10nJCommander();
    l10nJCommander.run(
        "git-blame",
        "-r",
        repository.getName(),
        "-s",
        sourceRepo.directory.getAbsolutePath(),
        "-ft",
        "po");

    assertEquals(0, l10nJCommander.getExitCode());

    gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);
    verifyGitBlame(gitBlameWithUsages, sourceRepo.gitBlame);
  }

  @Test
  public void textUnitNameToTextUnitNameInSourceDefault() {
    GitBlameCommand gitBlameCommand = new GitBlameCommand();

    FileType fileType = new POFileType();

    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test", fileType, false));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test _zero", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test _one", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test _two", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test _few", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test _many", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test _other", fileType, true));
    assertEquals(
        "test_test",
        gitBlameCommand.textUnitNameToTextUnitNameInSource("test_test", fileType, false));
    assertEquals(
        "test_test",
        gitBlameCommand.textUnitNameToTextUnitNameInSource("test_test _one", fileType, true));
  }

  @Test
  public void textUnitNameToTextUnitNameInSourceAndroid() {
    GitBlameCommand gitBlameCommand = new GitBlameCommand();

    FileType fileType = new AndroidStringsFileType();

    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test", fileType, false));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test_zero", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test_one", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test_two", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test_few", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test_many", fileType, true));
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test_other", fileType, true));
    assertEquals(
        "test_test",
        gitBlameCommand.textUnitNameToTextUnitNameInSource("test_test", fileType, false));
    assertEquals(
        "test_test",
        gitBlameCommand.textUnitNameToTextUnitNameInSource("test_test_one", fileType, true));
    // array in android
    assertEquals(
        "test", gitBlameCommand.textUnitNameToTextUnitNameInSource("test_1", fileType, false));
  }

  @Test
  public void textUnitNameToTextUnitNameInSourceChromeExtJson() {
    GitBlameCommand gitBlameCommand = new GitBlameCommand();

    FileType fileType = new ChromeExtensionJSONFileType();

    assertEquals(
        "hello",
        gitBlameCommand.textUnitNameToTextUnitNameInSource("hello/message", fileType, false));
    assertEquals(
        "hello/message_zero",
        gitBlameCommand.textUnitNameToTextUnitNameInSource("hello/message_zero", fileType, true));
    try {
      gitBlameCommand.textUnitNameToTextUnitNameInSource("hello_zero", fileType, false);
      fail("should throw an exception if the regex doesn't match");
    } catch (IllegalArgumentException iae) {
      assertEquals(
          "The pattern to extract the 'text unit name in source' must match. For text unit name: hello_zero",
          iae.getMessage());
    }
  }

  @Test
  public void getTextUnitName() {
    String lines[] =
        new String[] {
          "<string name=\"test_0\">Test 0</string>\n", "    <string name=\"test_1\">Test 1</string>"
        };
    GitBlameWithUsage gitBlameWithUsage = new GitBlameWithUsage();
    gitBlameWithUsage.setTextUnitName("test_0");
    GitBlameWithUsage gitBlameWithUsage1 = new GitBlameWithUsage();
    gitBlameWithUsage1.setTextUnitName("test_1");

    List<GitBlameWithUsage> textUnitWithUsages = new ArrayList<>();
    textUnitWithUsages.add(gitBlameWithUsage);
    textUnitWithUsages.add(gitBlameWithUsage1);

    GitBlameCommand gitBlameCommand = new GitBlameCommand();

    for (int i = 0; i < lines.length; i++) {
      List<GitBlameWithUsage> gitBlameWithUsages =
          gitBlameCommand.getGitBlameWithUsagesFromLine(
              lines[i], textUnitWithUsages, new AndroidStringsFileType());
      assertEquals(textUnitWithUsages.get(i), gitBlameWithUsages.get(i));
      assertEquals(2, gitBlameWithUsages.size());
    }
  }

  @Test
  public void getTextUnitNamePlural() {
    String line = "<plurals name=\"plural_tests\">\n";
    GitBlameWithUsage gitBlameWithUsage_zero = new GitBlameWithUsage();
    gitBlameWithUsage_zero.setTextUnitName("plural_tests_zero");
    GitBlameWithUsage gitBlameWithUsage_one = new GitBlameWithUsage();
    gitBlameWithUsage_one.setTextUnitName("plural_tests_one");
    GitBlameWithUsage gitBlameWithUsage_two = new GitBlameWithUsage();
    gitBlameWithUsage_two.setTextUnitName("plural_tests_two");
    GitBlameWithUsage gitBlameWithUsage_few = new GitBlameWithUsage();
    gitBlameWithUsage_few.setTextUnitName("plural_tests_few");
    GitBlameWithUsage gitBlameWithUsage_many = new GitBlameWithUsage();
    gitBlameWithUsage_many.setTextUnitName("plural_tests_many");
    GitBlameWithUsage gitBlameWithUsage_other = new GitBlameWithUsage();
    gitBlameWithUsage_other.setTextUnitName("plural_tests_other");

    List<GitBlameWithUsage> gitBlameWithUsagesExpected = new ArrayList<>();
    gitBlameWithUsagesExpected.add(gitBlameWithUsage_zero);
    gitBlameWithUsagesExpected.add(gitBlameWithUsage_one);
    gitBlameWithUsagesExpected.add(gitBlameWithUsage_two);
    gitBlameWithUsagesExpected.add(gitBlameWithUsage_few);
    gitBlameWithUsagesExpected.add(gitBlameWithUsage_many);
    gitBlameWithUsagesExpected.add(gitBlameWithUsage_other);

    GitBlameCommand gitBlameCommand = new GitBlameCommand();
    List<GitBlameWithUsage> gitBlameWithUsagesActual =
        gitBlameCommand.getGitBlameWithUsagesFromLine(
            line, gitBlameWithUsagesExpected, new AndroidStringsFileType());

    for (int i = 0; i < gitBlameWithUsagesActual.size(); i++)
      assertEquals(gitBlameWithUsagesExpected.get(i), gitBlameWithUsagesActual.get(i));
  }

  @Test
  public void getBlameResultForLines() throws Exception {
    CommittedGitRepo sourceRepo = createTempGitRepoFrom(getInputResourcesTestDir("source"));

    GitBlameCommand gitBlameCommand = gitBlameCommandForRepo(sourceRepo.directory);

    String relativePath = "res/values/strings.xml";
    BlameResult blameResult = gitBlameCommand.gitRepository.getBlameResultForFile(relativePath);

    for (int lineNumber = 0; lineNumber < blameResult.getResultContents().size(); lineNumber++) {
      PersonIdent actualAuthor = blameResult.getSourceAuthor(lineNumber);
      RevCommit actualCommit = blameResult.getSourceCommit(lineNumber);
      assertEquals(BLAME_AUTHOR_NAME, actualAuthor.getName());
      assertEquals(BLAME_AUTHOR_EMAIL, actualAuthor.getEmailAddress());
      assertEquals(sourceRepo.gitBlame.getCommitName(), actualCommit.getName());
      assertEquals(
          Integer.parseInt(sourceRepo.gitBlame.getCommitTime()), actualCommit.getCommitTime());
    }
  }

  @Test
  public void getFileName() {
    GitBlameCommand gitBlameCommand = new GitBlameCommand();

    assertEquals("file.js", gitBlameCommand.getFileName("file.js"));
    assertEquals("file.js", gitBlameCommand.getFileName("file.js:25"));
    assertEquals("path/to/file.js", gitBlameCommand.getFileName("path/to/file.js"));
    assertEquals("path/to/file.js", gitBlameCommand.getFileName("path/to/file.js:25"));
  }

  @Test
  public void getFileLine() throws Exception {
    GitBlameCommand gitBlameCommand = new GitBlameCommand();

    assertEquals(24, gitBlameCommand.getLineNumber("file.js:25"));
    assertEquals(24, gitBlameCommand.getLineNumber("path/to/file.js:25"));
  }

  @Test
  public void getBlameResultForFileWhenFileIsMissing()
      throws CommandException, NoSuchFileException, Exception {
    CommittedGitRepo sourceRepo = createTempGitRepoWithSingleFile("tracked.txt", "content\n");
    GitBlameCommand gitBlameCommand = gitBlameCommandForRepo(sourceRepo.directory);
    BlameResult blameResult =
        gitBlameCommand.gitRepository.getBlameResultForFile("forSomeMissingFile");
    assertNull(blameResult);
  }

  @Test(expected = NoSuchFileException.class)
  public void getBlameResultForFileCachedWhenFileIsMissing()
      throws CommandException, NoSuchFileException, Exception {
    CommittedGitRepo sourceRepo = createTempGitRepoWithSingleFile("tracked.txt", "content\n");
    GitBlameCommand gitBlameCommand = gitBlameCommandForRepo(sourceRepo.directory);
    gitBlameCommand.getBlameResultForFileCached("forSomeMissingFile");
  }

  @Test(expected = LineMissingException.class)
  public void updateGitBlameOutOfBousnd()
      throws CommandException, NoSuchFileException, LineMissingException, Exception {
    CommittedGitRepo sourceRepo = createTempGitRepoWithSingleFile("pom.xml", "<project/>\n");
    GitBlameCommand gitBlameCommand = gitBlameCommandForRepo(sourceRepo.directory);
    BlameResult blameResult = gitBlameCommand.getBlameResultForFileCached("pom.xml");
    GitBlameWithUsage gitBlameWithUsage = new GitBlameWithUsage();
    gitBlameCommand.updateBlameResultsInGitBlameWithUsage(100000, blameResult, gitBlameWithUsage);
    assertNull(gitBlameWithUsage.getGitBlame().getAuthorName());
  }

  @Test(expected = NoSuchFileException.class)
  public void updateGitBlameOMissingFile()
      throws CommandException, NoSuchFileException, LineMissingException, Exception {
    CommittedGitRepo sourceRepo = createTempGitRepoWithSingleFile("tracked.txt", "content\n");
    GitBlameCommand gitBlameCommand = gitBlameCommandForRepo(sourceRepo.directory);
    BlameResult blameResult = gitBlameCommand.getBlameResultForFileCached("somemissginfile");
    GitBlameWithUsage gitBlameWithUsage = new GitBlameWithUsage();
    gitBlameCommand.updateBlameResultsInGitBlameWithUsage(10, blameResult, gitBlameWithUsage);
    assertNull(gitBlameWithUsage.getGitBlame().getAuthorName());
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void getSourceCommitsAccessOutOfBound()
      throws CommandException, NoSuchFileException, Exception {
    CommittedGitRepo sourceRepo = createTempGitRepoWithSingleFile("pom.xml", "<project/>\n");
    GitBlameCommand gitBlameCommand = gitBlameCommandForRepo(sourceRepo.directory);
    BlameResult blameResult = gitBlameCommand.getBlameResultForFileCached("pom.xml");
    blameResult.getSourceCommit(100000);
  }

  @Test
  public void getGitBlameWithUsagesToProcess() {
    GitBlameCommand gitBlameCommand = new GitBlameCommand();

    ArrayList<GitBlameWithUsage> gitBlameWithUsages = new ArrayList<>();
    GitBlameWithUsage toSkip = new GitBlameWithUsage();
    toSkip.setGitBlame(new GitBlame());
    gitBlameWithUsages.add(toSkip);
    gitBlameWithUsages.add(new GitBlameWithUsage());

    List<GitBlameWithUsage> gitBlameWithUsagesToProcess =
        gitBlameCommand.getGitBlameWithUsagesToProcess(gitBlameWithUsages);

    assertEquals(1, gitBlameWithUsagesToProcess.size());
    assertFalse(gitBlameWithUsagesToProcess.contains(toSkip));
  }

  void verifyGitBlame(
      List<com.box.l10n.mojito.service.gitblame.GitBlameWithUsage> gitBlameWithUsages,
      GitBlame gitBlame) {
    assertFalse(gitBlameWithUsages.isEmpty());

    gitBlameWithUsages.stream()
        .forEach(
            g -> {
              logger.debug(
                  "name: {}, plural: {}, commit: {}",
                  g.getTextUnitName(),
                  g.getPluralForm(),
                  g.getGitBlame().getCommitName());
            });

    for (com.box.l10n.mojito.service.gitblame.GitBlameWithUsage gitBlameWithUsage :
        gitBlameWithUsages) {
      assertEquals(gitBlame.getCommitName(), gitBlameWithUsage.getGitBlame().getCommitName());
      assertEquals(gitBlame.getCommitTime(), gitBlameWithUsage.getGitBlame().getCommitTime());
      assertEquals(gitBlame.getAuthorName(), gitBlameWithUsage.getGitBlame().getAuthorName());
      assertEquals(gitBlame.getAuthorEmail(), gitBlameWithUsage.getGitBlame().getAuthorEmail());
    }
  }

  GitBlameCommand gitBlameCommandForRepo(File repoDir) throws CommandException {
    GitBlameCommand gitBlameCommand = new GitBlameCommand();
    gitBlameCommand.commandDirectories = new CommandDirectories(repoDir.getAbsolutePath());
    gitBlameCommand.initGitRepository();
    return gitBlameCommand;
  }

  CommittedGitRepo createTempGitRepoFrom(File sourceDirectory) throws Exception {
    File repoDir = new File(getTargetTestDir(), "git-repo");
    FileUtils.copyDirectory(sourceDirectory, repoDir);
    return commitWorkingTree(repoDir);
  }

  CommittedGitRepo createTempGitRepoForPoFile() throws Exception {
    File repoDir = new File(getTargetTestDir(), "git-repo");
    File sourceInRepo = new File(repoDir, "source");
    FileUtils.copyDirectory(getInputResourcesTestDir("source"), sourceInRepo);
    FileUtils.copyFile(
        new File(getInputResourcesTestDir(), "file.js"), new File(repoDir, "file.js"));
    CommittedGitRepo committed = commitWorkingTree(repoDir);
    return new CommittedGitRepo(sourceInRepo, committed.gitBlame);
  }

  CommittedGitRepo createTempGitRepoWithSingleFile(String relativePath, String content)
      throws Exception {
    File repoDir = new File(getTargetTestDir(), "git-repo");
    File file = new File(repoDir, relativePath);
    file.getParentFile().mkdirs();
    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    return commitWorkingTree(repoDir);
  }

  CommittedGitRepo commitWorkingTree(File repoDir) throws Exception {
    PersonIdent ident =
        new PersonIdent(
            BLAME_AUTHOR_NAME,
            BLAME_AUTHOR_EMAIL,
            new Date(BLAME_COMMIT_TIME_MILLIS),
            TimeZone.getTimeZone("UTC"));

    try (Git git = Git.init().setDirectory(repoDir).call()) {
      git.add().addFilepattern(".").call();
      RevCommit commit =
          git.commit()
              .setMessage("Deterministic git-blame test commit")
              .setAuthor(ident)
              .setCommitter(ident)
              .call();

      GitBlame gitBlame = new GitBlame();
      gitBlame.setCommitName(commit.getName());
      gitBlame.setCommitTime(Integer.toString(commit.getCommitTime()));
      gitBlame.setAuthorName(ident.getName());
      gitBlame.setAuthorEmail(ident.getEmailAddress());
      return new CommittedGitRepo(repoDir, gitBlame);
    }
  }

  static class CommittedGitRepo {
    final File directory;
    final GitBlame gitBlame;

    CommittedGitRepo(File directory, GitBlame gitBlame) {
      this.directory = directory;
      this.gitBlame = gitBlame;
    }
  }
}
