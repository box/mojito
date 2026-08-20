package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.service.repotype.RepoTypeRepository;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RepoTypeDeleteCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(RepoTypeDeleteCommandTest.class);

  @Autowired RepoTypeService repoTypeService;

  @Autowired RepoTypeRepository repoTypeRepository;

  @Test
  public void testDeleteRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("Android");

    repoTypeService.createRepoType(name, "Android strings.xml apps", null, null);

    getL10nJCommander().run("repo-type-delete", Param.REPO_TYPE_NAME_SHORT, name);

    assertTrue(
        "Repo type is not deleted successfully",
        outputCapture.toString().contains("deleted --> repo type name: "));
    assertNull("Repo type should be deleted", repoTypeRepository.findByName(name));
  }

  @Test
  public void testDeleteNonExistingRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("missing");

    getL10nJCommander().run("repo-type-delete", Param.REPO_TYPE_NAME_SHORT, name);

    assertTrue(
        "Expecting error from deleting non-existing repo type",
        outputCapture.toString().contains("Repo type with name [" + name + "] is not found"));
  }
}
