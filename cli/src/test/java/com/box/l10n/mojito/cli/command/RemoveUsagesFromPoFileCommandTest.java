package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import org.junit.Test;

public class RemoveUsagesFromPoFileCommandTest extends CLITestBase {

    @Test
    public void strip() {
        System.setProperty("overrideExpectedTestFiles", "true");

        getL10nJCommander().run("po-remove-usages",
                "-s", getInputResourcesTestDir().getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }
}