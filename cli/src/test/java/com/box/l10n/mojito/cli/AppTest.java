package com.box.l10n.mojito.cli;

import org.junit.Test;

/**
 * @author jaurambault
 */
public class AppTest extends CLITestBase {

  @Test
  public void appVersion() throws Exception {
    getL10nJCommander().run("--version");
  }

  @Test
  public void appHelp() throws Exception {
    getL10nJCommander().run("--help");
  }

  @Test
  public void appHelpShort() throws Exception {
    getL10nJCommander().run("-h");
  }
}
