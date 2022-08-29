package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.Repository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author jyi */
public class RepoDeleteCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepoDeleteCommandTest.class);

  @Test
  public void testDeleteTestRepo() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    String testRepoName = repository.getName();

    getL10nJCommander().run("repo-delete", Param.REPOSITORY_NAME_SHORT, testRepoName);

    assertTrue(
        "Repository is not deleted successfully",
        outputCapture.toString().contains("deleted --> repository name: "));
  }

  @Test
  public void testDeleteNonExistingRepo() throws Exception {
    String testRepoName = testIdWatcher.getEntityName("repository");

    getL10nJCommander().run("repo-delete", Param.REPOSITORY_NAME_SHORT, testRepoName);

    assertTrue(
        "Expecting error from deleting non-existing repository",
        outputCapture
            .toString()
            .contains("Repository with name [" + testRepoName + "] is not found"));
  }
}
