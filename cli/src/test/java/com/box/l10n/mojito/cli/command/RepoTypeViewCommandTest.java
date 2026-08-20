package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RepoTypeViewCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(RepoTypeViewCommandTest.class);

  @Autowired RepoTypeService repoTypeService;

  @Test
  public void testViewRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("Android");
    String description = "Android strings.xml apps";

    RepoType created = repoTypeService.createRepoType(name, description, null, null);

    getL10nJCommander().run("repo-type-view", Param.REPO_TYPE_NAME_SHORT, name);

    String output = outputCapture.toString();
    assertTrue(
        "Repo type id is missing or incorrect from output",
        output.contains("Repo type id --> " + created.getId()));
    assertTrue(
        "Repo type name is missing or incorrect from output", output.contains("Name --> " + name));
    assertTrue(
        "Repo type description is missing or incorrect from output",
        output.contains("Description --> " + description));
  }

  @Test
  public void testViewNonExistingRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("missing");

    getL10nJCommander().run("repo-type-view", Param.REPO_TYPE_NAME_SHORT, name);

    assertTrue(
        "Expecting error from viewing non-existing repo type",
        outputCapture.toString().contains("Repo type with name [" + name + "] is not found"));
  }
}
