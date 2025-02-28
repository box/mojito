package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class PseudoLocCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PseudoLocCommandTest.class);

  @Test
  public void pseudo() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    getL10nJCommander()
        .run(
            "pseudo",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    checkExpectedGeneratedResources();
  }
}
