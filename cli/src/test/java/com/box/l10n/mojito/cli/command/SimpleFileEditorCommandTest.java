package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import org.junit.Test;

public class SimpleFileEditorCommandTest extends CLITestBase {

    @Test
    public void jsonIndent() {
        getL10nJCommander().run("simple-file-editor",
                "-i", getInputResourcesTestDir().getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath(),
                 "--json-indent");

        checkExpectedGeneratedResources();
    }

    @Test
    public void poRemoveUsages() {
        getL10nJCommander().run("simple-file-editor",
                "-i", getInputResourcesTestDir().getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath(),
                "--po-remove-usages"
                );

        checkExpectedGeneratedResources();
    }

    @Test
    public void macStringsRemoveUsages() {
        getL10nJCommander().run("simple-file-editor",
                "-i", getInputResourcesTestDir().getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath(),
                "--macstrings-remove-usages"
        );

        checkExpectedGeneratedResources();
    }
}