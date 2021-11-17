package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import org.junit.Assert;
import org.junit.Test;

public class RunChecksCommandTest extends CLITestBase {

    @Test
    public void runSuccessfulChecks() throws Exception {

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source2");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1");

        getL10nJCommander().run("run-checks",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "SPELL_CHECKER");

        Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
        Assert.assertTrue(outputCapture.toString().contains("Checks completed"));
        Assert.assertFalse(outputCapture.toString().contains("failed") || outputCapture.toString().contains("Failed"));
    }

    @Test
    public void runHardFailChecks() throws Exception {

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source2");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1");

        getL10nJCommander().run("run-checks",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "SPELL_CHECKER",
                "-hf", "SPELL_CHECKER");

        Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
        Assert.assertTrue(outputCapture.toString().contains("The following checks had hard failures:" + System.lineSeparator()));
        Assert.assertTrue(outputCapture.toString().contains("SPELL_CHECKER"));
        Assert.assertTrue(outputCapture.toString().contains("The string 'This is a new sorce string with spelling failure' contains misspelled words:"));
        Assert.assertTrue(outputCapture.toString().contains("* 'sorce' - Did you mean "));
    }

    @Test
    public void runSoftFailChecks() throws Exception {

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source2");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1");

        getL10nJCommander().run("run-checks",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "SPELL_CHECKER");

        Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
        Assert.assertTrue(outputCapture.toString().contains("Failed checks: "));
        Assert.assertTrue(outputCapture.toString().contains("SPELL_CHECKER"));
        Assert.assertTrue(outputCapture.toString().contains("Checks completed"));
    }

    @Test
    public void runCheckWithInvalidCheckName() {
        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source2");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1");

        getL10nJCommander().run("run-checks",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "INVALID_CHECK_NAME");

        Assert.assertTrue(outputCapture.toString().contains("Unknown check 'INVALID_CHECK_NAME'"));
    }

    @Test
    public void runHardFailChecksWithInvalidCheckName() throws Exception {

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source2");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1");

        getL10nJCommander().run("run-checks",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "SPELL_CHECKER",
                "-hf", "INVALID_NAME");

        Assert.assertTrue(outputCapture.toString().contains("Unknown check name in hard fail list 'INVALID_NAME'"));
    }
}
