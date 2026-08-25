package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.service.repotype.RepoTypeRepository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RepoTypeCreateCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(RepoTypeCreateCommandTest.class);

  @Autowired RepoTypeRepository repoTypeRepository;

  @Test
  public void testCreateRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("React");
    String description = "FormatJS / react-intl apps";

    getL10nJCommander()
        .run(
            "repo-type-create",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            description);

    assertTrue(outputCapture.toString().contains("created --> repo type id: "));

    RepoType created = repoTypeRepository.findByName(name);
    assertNotNull(created);
    assertEquals(name, created.getName());
    assertEquals(description, created.getDescription());
  }

  @Test
  public void testCreateRepoTypeWithoutDescription() throws Exception {
    String name = testIdWatcher.getEntityName("NoDesc");

    getL10nJCommander().run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name);

    assertTrue(outputCapture.toString().contains("created --> repo type id: "));

    RepoType created = repoTypeRepository.findByName(name);
    assertNotNull(created);
    assertEquals(name, created.getName());
    assertNull(created.getDescription());
  }

  @Test
  public void testCreateRepoTypeDuplicateName() throws Exception {
    String name = testIdWatcher.getEntityName("Conflict");

    getL10nJCommander().run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name);
    getL10nJCommander().run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name);

    String output = outputCapture.toString();
    assertTrue(output.contains("RepoType with name [" + name + "] already exists"));
    assertFalse(
        "Mapped 409 must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));
  }

  @Test
  public void testCreateNameLongerThanMaxIsAShortError() throws Exception {
    String name = "A".repeat(RepoType.NAME_MAX_LENGTH + 1);

    getL10nJCommander().run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name);

    String output = outputCapture.toString();
    assertTrue(
        "400 body must be shown as a CommandException, not a generic HTTP dump",
        output.contains("name must be at most " + RepoType.NAME_MAX_LENGTH + " characters"));
    assertFalse(
        "Mapped 400 must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));
    assertNull(repoTypeRepository.findByName(name));
  }
}
