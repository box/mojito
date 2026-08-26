package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import java.util.regex.Pattern;
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
  public void testViewRepoTypeWithNullDescription() throws Exception {
    String name = testIdWatcher.getEntityName("NoDesc");

    repoTypeService.createRepoType(name, null, null, null);

    getL10nJCommander().run("repo-type-view", Param.REPO_TYPE_NAME_SHORT, name);

    String output = outputCapture.toString();
    assertFalse(
        "Null description must not print the string \"null\"",
        output.contains("Description --> null"));
    assertTrue(
        "Null description must print an empty value after the label",
        Pattern.compile("Description --> $", Pattern.MULTILINE).matcher(output).find());
  }

  @Test
  public void testViewNonExistingRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("missing");

    getL10nJCommander().run("repo-type-view", Param.REPO_TYPE_NAME_SHORT, name);

    assertTrue(
        "Expecting error from viewing non-existing repo type",
        outputCapture.toString().contains("Repo type with name [" + name + "] is not found"));
  }

  @Test
  public void testViewBlankNameDoesNotSelectTheOnlyType() throws Exception {
    String name = testIdWatcher.getEntityName("OnlyType");

    RepoType created = repoTypeService.createRepoType(name, "must not be viewed", null, null);

    getL10nJCommander().run("repo-type-view", Param.REPO_TYPE_NAME_SHORT, " ");

    String output = outputCapture.toString();
    assertTrue(
        "Blank -n must fail with a required-name error, not list-all lookup",
        output.contains("Repo type name is required"));
    assertFalse(
        "Must not fall through to not-found from a list-all of size != 1",
        output.contains("is not found"));
    assertFalse(
        "Must not print the type as if lookup succeeded",
        output.contains("Repo type id --> " + created.getId()));
  }
}
