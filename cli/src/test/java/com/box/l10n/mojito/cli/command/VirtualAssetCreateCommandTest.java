package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualAssetCreateCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(VirtualAssetCreateCommandTest.class);

  @Test
  public void execute() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    getL10nJCommander().run("virtual-asset-create", "-r", repository.getName(), "-p", "asset1");
    getL10nJCommander().run("virtual-asset-create", "-r", repository.getName(), "-p", "asset2");
  }
}
