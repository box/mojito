package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jyi
 */
public class RepoViewCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepoViewCommandTest.class);

  @Autowired RepoTypeService repoTypeService;

  @Autowired RepositoryRepository repositoryRepository;

  @Test
  public void testViewTestRepo() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    String testRepoName = repository.getName();

    getL10nJCommander().run("repo-view", Param.REPOSITORY_NAME_SHORT, testRepoName);

    assertTrue(
        "Repository id is missing or incorrect from output",
        outputCapture.toString().contains("Repository id --> " + repository.getId()));
    assertFalse(outputCapture.toString().contains("Repository type -->"));
    assertFalse(
        "Repository integrity checker is incorrect",
        outputCapture.toString().contains("Integrity checkers -->"));
    assertTrue(
        "Repository integrity checker is incorrect",
        outputCapture.toString().contains("Repository locales --> \"(fr-CA)->fr-FR\" fr-FR ja-JP"));

    getL10nJCommander()
        .run(
            "repo-update",
            Param.REPOSITORY_NAME_SHORT,
            testRepoName,
            RepoCommand.INTEGRITY_CHECK_SHORT_PARAM,
            "xliff:SIMPLE_PRINTF_LIKE,properties:MESSAGE_FORMAT");
    getL10nJCommander().run("repo-view", Param.REPOSITORY_NAME_SHORT, testRepoName);

    assertTrue(
        "Repository id is missing or incorrect from output",
        outputCapture.toString().contains("Repository id --> " + repository.getId()));
    assertTrue(
        "Repository integrity checker is incorrect",
        outputCapture
            .toString()
            .contains("Integrity checkers --> properties:MESSAGE_FORMAT,xliff:SIMPLE_PRINTF_LIKE"));
    assertTrue(
        "Repository integrity checker is incorrect",
        outputCapture.toString().contains("Repository locales --> \"(fr-CA)->fr-FR\" fr-FR ja-JP"));
  }

  @Test
  public void testViewRepoType() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    RepoType repoType =
        repoTypeService.createRepoType(testIdWatcher.getEntityName("React"), null, null, Set.of());
    repository.setRepoType(repoType);
    repositoryRepository.save(repository);

    getL10nJCommander().run("repo-view", Param.REPOSITORY_NAME_SHORT, repository.getName());

    assertTrue(outputCapture.toString().contains("Repository type --> " + repoType.getName()));
  }

  @Test
  public void testViewNonExistingRepo() throws Exception {
    String testRepoName = testIdWatcher.getEntityName("repository");

    getL10nJCommander().run("repo-view", Param.REPOSITORY_NAME_SHORT, testRepoName);

    assertTrue(
        "Expecting error from viewing non-existing repository",
        outputCapture
            .toString()
            .contains("Repository with name [" + testRepoName + "] is not found"));
  }
}
