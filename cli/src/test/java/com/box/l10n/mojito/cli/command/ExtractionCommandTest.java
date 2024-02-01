package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeanaurambault
 */
public class ExtractionCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ExtractionCommandTest.class);

  @Test
  public void extract() throws Exception {
    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir().getAbsolutePath(),
            "-fo",
            "testoption=something",
            "-n",
            "source1");

    checkExpectedGeneratedResources();
  }

  @Test
  public void extractBrokenPO() {
    L10nJCommander l10nJCommander = getL10nJCommander();
    l10nJCommander.run(
        "extract",
        "-s",
        getInputResourcesTestDir("source1").getAbsolutePath(),
        "-o",
        getTargetTestDir().getAbsolutePath(),
        "-fo",
        "testoption=something",
        "-n",
        "source1");

    Assert.assertEquals(1L, l10nJCommander.getExitCode());
  }
}
