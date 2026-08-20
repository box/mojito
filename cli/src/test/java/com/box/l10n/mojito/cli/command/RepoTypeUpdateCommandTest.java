package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
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

public class RepoTypeUpdateCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(RepoTypeUpdateCommandTest.class);

  @Autowired RepoTypeService repoTypeService;

  @Autowired RepoTypeRepository repoTypeRepository;

  @Test
  public void testUpdateName() throws Exception {
    String name = testIdWatcher.getEntityName("Android");
    String newName = name + "_updated";
    String description = "Android strings.xml apps";

    RepoType created = repoTypeService.createRepoType(name, description, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_NEW_NAME_SHORT,
            newName);

    assertTrue(
        "Repo type is not updated successfully",
        outputCapture.toString().contains("updated --> repo type id: "));

    assertNull("Should not find repo type by old name", repoTypeRepository.findByName(name));

    RepoType updated = repoTypeRepository.findByName(newName);
    assertNotNull("Should find repo type by the new name", updated);
    assertEquals(created.getId(), updated.getId());
    assertEquals(description, updated.getDescription());
  }

  @Test
  public void testUpdateDescription() throws Exception {
    String name = testIdWatcher.getEntityName("iOS");
    String oldDescription = "old description";
    String newDescription = "Swift / Localizable.strings apps";

    RepoType created = repoTypeService.createRepoType(name, oldDescription, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            newDescription);

    assertTrue(
        "Repo type is not updated successfully",
        outputCapture.toString().contains("updated --> repo type id: "));

    RepoType updated = repoTypeRepository.findByName(name);
    assertNotNull(updated);
    assertEquals(created.getId(), updated.getId());
    assertEquals(newDescription, updated.getDescription());
  }

  @Test
  public void testUpdateWithoutNewNameOrDescription() throws Exception {
    String name = testIdWatcher.getEntityName("NoOptions");

    repoTypeService.createRepoType(name, "original description", null, null);

    getL10nJCommander().run("repo-type-update", Param.REPO_TYPE_NAME_SHORT, name);

    assertTrue(
        "Expecting error when neither --new-name nor --description is provided",
        outputCapture
            .toString()
            .contains(
                "Must provide at least one of the following options: --new-name, --description"));

    RepoType unchanged = repoTypeRepository.findByName(name);
    assertNotNull(unchanged);
    assertEquals("original description", unchanged.getDescription());
  }

  @Test
  public void testUpdateNonExistingRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("missing");

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            "new description");

    assertTrue(
        "Expecting error from updating non-existing repo type",
        outputCapture.toString().contains("Repo type with name [" + name + "] is not found"));
  }

  @Test
  public void testUpdateDuplicateName() throws Exception {
    String name1 = testIdWatcher.getEntityName("TypeA");
    String name2 = testIdWatcher.getEntityName("TypeB");

    repoTypeService.createRepoType(name1, null, null, null);
    repoTypeService.createRepoType(name2, null, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name1,
            Param.REPO_TYPE_NEW_NAME_SHORT,
            name2);

    assertTrue(
        "Expecting error from renaming to an existing repo type name",
        outputCapture.toString().contains("Repo type with name [" + name2 + "] already exists"));
  }
}
