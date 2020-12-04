package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import org.junit.Test;

public class RemoveUsagesFromAssetCommandTest extends CLITestBase {

    @Test
    public void strip() {
        System.setProperty("overrideExpectedTestFiles", "true");

        getL10nJCommander().run("remove-usages",
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }
}