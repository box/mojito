package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jyi
 */
public class RepoUpdateCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepoUpdateCommandTest.class);

  @Autowired RepositoryRepository repositoryRepository;

  @Test
  public void testUpdateName() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    String testRepoName = repository.getName();
    String newName = testRepoName + "_updated";

    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            Param.REPOSITORY_NEW_NAME_SHORT,
            newName);

    assertTrue(
        "Repository name is not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    repository = repositoryRepository.findByName(testRepoName);
    assertNull("Should not find repository by old name", repository);

    repository = repositoryRepository.findByName(newName);
    assertNotNull("Should find repository by the new name", repository);
  }

  @Test
  public void testUpdateLocale() throws Exception {
    Repository repository = createTestRepoUsingRepoService("repo", true);
    String testRepoName = repository.getName();
    Boolean testCheckSLA = repository.getCheckSLA();

    String newName = testRepoName + "_updated";

    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            Param.REPOSITORY_NEW_NAME_SHORT,
            newName,
            Param.REPOSITORY_LOCALES_SHORT,
            "de-DE");

    assertTrue(
        "Repository locales are not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    repository = repositoryRepository.findByName(newName);
    assertEquals(2, repository.getRepositoryLocales().size());
    assertEquals(testCheckSLA, repository.getCheckSLA());
    for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
      assertTrue(
          "en".equals(repositoryLocale.getLocale().getBcp47Tag())
              || "de-DE".equals(repositoryLocale.getLocale().getBcp47Tag()));
    }
  }

  @Test
  public void testUpdateLocalesWithInheritance() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    String testRepoName = repository.getName();
    String newName = testRepoName + "_updated";

    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            Param.REPOSITORY_NEW_NAME_SHORT,
            newName,
            Param.REPOSITORY_LOCALES_SHORT,
            "en-GB",
            "en-CA->en-GB",
            "(en-AU)->en-GB",
            "fr-FR",
            "ko-KR");

    assertTrue(
        "Repository locales are not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    List<String> expectedLocales = Arrays.asList("en", "en-GB", "en-CA", "en-AU", "fr-FR", "ko-KR");
    repository = repositoryRepository.findByName(newName);
    assertEquals(expectedLocales.size(), repository.getRepositoryLocales().size());
    for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
      assertTrue(expectedLocales.contains(repositoryLocale.getLocale().getBcp47Tag()));
      if (repositoryLocale.getParentLocale() != null) {
        if ("en-CA".equals(repositoryLocale.getLocale().getBcp47Tag())
            || "en-AU".equals(repositoryLocale.getLocale().getBcp47Tag())) {
          assertEquals("en-GB", repositoryLocale.getParentLocale().getLocale().getBcp47Tag());
        } else {
          assertEquals("en", repositoryLocale.getParentLocale().getLocale().getBcp47Tag());
        }
      }
    }
  }

  @Test
  public void testUpdateAll() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    String testRepoName = repository.getName();
    String newName = testRepoName + "_updated";
    String newDescription = newName + "_description";

    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            Param.REPOSITORY_NEW_NAME_SHORT,
            newName,
            Param.REPOSITORY_DESCRIPTION_SHORT,
            newDescription,
            Param.REPOSITORY_LOCALES_SHORT,
            "de-DE",
            RepoCommand.INTEGRITY_CHECK_SHORT_PARAM,
            "properties:MESSAGE_FORMAT");

    assertTrue(
        "Repository is not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    repository = repositoryRepository.findByName(testRepoName);
    assertNull("Should not find repository by old name", repository);

    repository = repositoryRepository.findByName(newName);
    assertNotNull("Should find repository by the new name", repository);
    assertEquals(
        "Repository description is not updated", newDescription, repository.getDescription());
    assertEquals(2, repository.getRepositoryLocales().size());
    for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
      assertTrue(
          "en".equals(repositoryLocale.getLocale().getBcp47Tag())
              || "de-DE".equals(repositoryLocale.getLocale().getBcp47Tag()));
    }
    assertEquals(1, repository.getAssetIntegrityCheckers().size());
  }

  @Test
  public void testUpdateNonExistingRepo() throws Exception {
    String testRepoName = testIdWatcher.getEntityName("repository");
    String newName = testRepoName + "_updated";
    String newDescription = newName + "_description";

    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            Param.REPOSITORY_NEW_NAME_SHORT,
            newName,
            Param.REPOSITORY_DESCRIPTION_SHORT,
            newDescription,
            Param.REPOSITORY_LOCALES_SHORT,
            "de-DE",
            RepoCommand.INTEGRITY_CHECK_SHORT_PARAM,
            "properties:MESSAGE_FORMAT");

    assertTrue(
        "Expecting error from updating non-existing repository",
        outputCapture
            .toString()
            .contains("Repository with name [" + testRepoName + "] is not found"));
  }

  @Test
  public void testUpdateIntegrityChecker() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    String testRepoName = repository.getName();

    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            RepoCommand.INTEGRITY_CHECK_SHORT_PARAM,
            "resx:MESSAGE_FORMAT,resw:MESSAGE_FORMAT");
    assertTrue(
        "Repository is not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    repository = repositoryRepository.findByName(testRepoName);
    assertEquals(2, repository.getAssetIntegrityCheckers().size());
    assertEquals(
        "MESSAGE_FORMAT",
        repository
            .getAssetIntegrityCheckers()
            .iterator()
            .next()
            .getIntegrityCheckerType()
            .toString());
    assertEquals(
        "MESSAGE_FORMAT",
        repository
            .getAssetIntegrityCheckers()
            .iterator()
            .next()
            .getIntegrityCheckerType()
            .toString());

    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            RepoCommand.INTEGRITY_CHECK_SHORT_PARAM,
            "resx:COMPOSITE_FORMAT");
    assertTrue(
        "Repository is not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    repository = repositoryRepository.findByName(testRepoName);
    assertEquals(1, repository.getAssetIntegrityCheckers().size());
    assertEquals(
        "COMPOSITE_FORMAT",
        repository
            .getAssetIntegrityCheckers()
            .iterator()
            .next()
            .getIntegrityCheckerType()
            .toString());
  }

  @Test
  public void testDeleteExistingIntegrityChecker() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    String testRepoName = repository.getName();
    String testRepoNameUpdated = testRepoName + "_updated";

    // add two integrity checkers
    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            RepoCommand.INTEGRITY_CHECK_SHORT_PARAM,
            "resx:MESSAGE_FORMAT,resw:MESSAGE_FORMAT");
    assertTrue(
        "Repository is not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    repository = repositoryRepository.findByName(testRepoName);
    assertEquals(2, repository.getAssetIntegrityCheckers().size());
    assertEquals(
        "MESSAGE_FORMAT",
        repository
            .getAssetIntegrityCheckers()
            .iterator()
            .next()
            .getIntegrityCheckerType()
            .toString());
    assertEquals(
        "MESSAGE_FORMAT",
        repository
            .getAssetIntegrityCheckers()
            .iterator()
            .next()
            .getIntegrityCheckerType()
            .toString());

    // update repo without integrity checker param and check there was no change in existing
    // integrity checkers
    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            Param.REPOSITORY_NEW_NAME_SHORT,
            testRepoNameUpdated);
    assertTrue(
        "Repository is not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    repository = repositoryRepository.findByName(testRepoNameUpdated);
    assertEquals(2, repository.getAssetIntegrityCheckers().size());
    assertEquals(
        "MESSAGE_FORMAT",
        repository
            .getAssetIntegrityCheckers()
            .iterator()
            .next()
            .getIntegrityCheckerType()
            .toString());
    assertEquals(
        "MESSAGE_FORMAT",
        repository
            .getAssetIntegrityCheckers()
            .iterator()
            .next()
            .getIntegrityCheckerType()
            .toString());

    // update repo with empty integrity checker param and check existing integrity checkers are
    // deleted
    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoNameUpdated,
            RepoCommand.INTEGRITY_CHECK_SHORT_PARAM,
            "");
    assertTrue(
        "Repository is not updated successfully",
        outputCapture.toString().contains("updated --> repository name: "));

    repository = repositoryRepository.findByName(testRepoNameUpdated);
    assertEquals(
        "Integrity checker should have been deleted",
        0,
        repository.getAssetIntegrityCheckers().size());
  }

  @Test(expected = CommandException.class)
  public void testConflictingParameters() throws CommandException {
    RepoUpdateCommand repoUpdateCommand = new RepoUpdateCommand();
    repoUpdateCommand.nameParam = "repo1";
    repoUpdateCommand.repositoryNames = Arrays.asList("repo2");
    repoUpdateCommand.checkRepositoryParams();
  }

  @Test
  public void testUpdateAllWithNameFilter() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    String testRepoName = repository.getName();

    Repository repository2 = createTestRepoUsingRepoService("repo2", false);
    String testRepoName2 = repository2.getName();

    Repository repository3 = createTestRepoUsingRepoService("repo3", false);
    String testRepoName3 = repository3.getName();

    getL10nJCommander()
        .run(
            "repo-update",
            "-l",
            "fr-FR,ko-KR,ja-JP",
            "-rns",
            Joiner.on(",").join(testRepoName, testRepoName3));

    assertFalse(
        "Repositories not updated correctly",
        outputCapture.toString().contains("updated --> repository name: " + testRepoName2));
    assertTrue(
        "Repositories not updated correctly",
        outputCapture.toString().contains("updated --> repository name: " + testRepoName));
    assertTrue(
        "Repositories not update correctly",
        outputCapture.toString().contains("updated --> repository name: " + testRepoName3));
  }
}
