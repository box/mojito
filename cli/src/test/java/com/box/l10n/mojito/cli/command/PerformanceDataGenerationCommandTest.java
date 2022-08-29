package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.*;

import com.box.l10n.mojito.cli.CLITestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceDataGenerationCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PushCommandTest.class);

  @Test
  public void testGen() {
    getL10nJCommander().run("performance-data-gen", "--branch-creator", "jeanaurambault");
  }
}
