package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.entity.RepoTypeIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.box.l10n.mojito.service.repotype.RepoTypeRepository;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import java.util.Set;
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
  public void testUpdateDescriptionDoesNotClearPromptOrCheckers() throws Exception {
    String name = testIdWatcher.getEntityName("React");
    String prompt = "You are a React i18n expert";
    RepoTypeIntegrityChecker checker = new RepoTypeIntegrityChecker();
    checker.setAssetExtension("json");
    checker.setIntegrityCheckerType(IntegrityCheckerType.MESSAGE_FORMAT);

    RepoType created = repoTypeService.createRepoType(name, "old", prompt, Set.of(checker));

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            "new desc");

    assertTrue(
        "Repo type is not updated successfully",
        outputCapture.toString().contains("updated --> repo type id: "));

    RepoType updated = repoTypeService.getRepoTypeById(created.getId());
    assertEquals("new desc", updated.getDescription());
    assertPromptAndCheckerUnchanged(updated, prompt, "json", IntegrityCheckerType.MESSAGE_FORMAT);
  }

  @Test
  public void testUpdateNameDoesNotClearPromptOrCheckers() throws Exception {
    String name = testIdWatcher.getEntityName("Android");
    String newName = name + "_renamed";
    String prompt = "You are an Android i18n expert";
    RepoTypeIntegrityChecker checker = new RepoTypeIntegrityChecker();
    checker.setAssetExtension("xml");
    checker.setIntegrityCheckerType(IntegrityCheckerType.MESSAGE_FORMAT);

    RepoType created = repoTypeService.createRepoType(name, "old", prompt, Set.of(checker));

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

    RepoType updated = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(newName, updated.getName());
    assertEquals("old", updated.getDescription());
    assertPromptAndCheckerUnchanged(updated, prompt, "xml", IntegrityCheckerType.MESSAGE_FORMAT);
  }

  private static void assertPromptAndCheckerUnchanged(
      RepoType updated, String prompt, String assetExtension, IntegrityCheckerType checkerType) {
    assertEquals(prompt, updated.getAiPrompt());
    assertEquals(1, updated.getIntegrityCheckers().size());
    RepoTypeIntegrityChecker remaining = updated.getIntegrityCheckers().iterator().next();
    assertEquals(assetExtension, remaining.getAssetExtension());
    assertEquals(checkerType, remaining.getIntegrityCheckerType());
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

    String output = outputCapture.toString();
    assertTrue(
        "Expecting error from renaming to an existing repo type name",
        output.contains("RepoType with name [" + name2 + "] already exists"));
    assertFalse(
        "Mapped 409 must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));
  }

  @Test
  public void testUpdateBlankNameDoesNotSelectTheOnlyType() throws Exception {
    String name = testIdWatcher.getEntityName("OnlyType");
    String description = "must stay";

    repoTypeService.createRepoType(name, description, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            " ",
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            "oops");

    String output = outputCapture.toString();
    assertTrue(
        "Blank -n must fail with a required-name error, not list-all lookup",
        output.contains("Repo type name is required"));
    assertFalse(
        "Must not fall through to not-found from a list-all of size != 1",
        output.contains("is not found"));

    RepoType unchanged = repoTypeRepository.findByName(name);
    assertNotNull(unchanged);
    assertEquals(description, unchanged.getDescription());
  }

  @Test
  public void testUpdateDescriptionLongerThanMaxIsAShortError() throws Exception {
    String name = testIdWatcher.getEntityName("TooLong");
    String originalDescription = "keep me";

    repoTypeService.createRepoType(name, originalDescription, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            "B".repeat(RepoType.DESCRIPTION_MAX_LENGTH + 1));

    String output = outputCapture.toString();
    assertTrue(
        "400 body must be shown as a CommandException, not a generic HTTP dump",
        output.contains(
            "description must be at most " + RepoType.DESCRIPTION_MAX_LENGTH + " characters"));
    assertFalse(
        "Mapped 400 must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));

    RepoType unchanged = repoTypeRepository.findByName(name);
    assertNotNull(unchanged);
    assertEquals(originalDescription, unchanged.getDescription());
  }
}
