package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.RepoType;
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

  @Test
  public void testDeleteBlankNameDoesNotDeleteTheOnlyType() throws Exception {
    String name = testIdWatcher.getEntityName("OnlyType");

    repoTypeService.createRepoType(name, "must stay", null, null);

    getL10nJCommander().run("repo-type-delete", Param.REPO_TYPE_NAME_SHORT, " ");

    String output = outputCapture.toString();
    assertTrue(
        "Blank -n must fail with a required-name error, not list-all lookup",
        output.contains("Repo type name is required"));
    assertFalse(
        "Must not fall through to not-found from a list-all of size != 1",
        output.contains("is not found"));
    assertFalse(
        "Must not print a successful delete", output.contains("deleted --> repo type name: "));

    RepoType stillThere = repoTypeRepository.findByName(name);
    assertNotNull("Blank -n must not delete the type", stillThere);
  }
}
