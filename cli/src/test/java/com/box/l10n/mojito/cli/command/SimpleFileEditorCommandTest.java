package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import org.junit.Test;

public class SimpleFileEditorCommandTest extends CLITestBase {

    @Test
    public void edit() {
        getL10nJCommander().run("simple-file-editor",
                "-i", getInputResourcesTestDir().getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath(),
                "--po-remove-usages",
                "--json-indent");

        checkExpectedGeneratedResources();
    }
}