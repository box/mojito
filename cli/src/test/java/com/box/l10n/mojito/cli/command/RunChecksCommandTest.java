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
                "-hf", "all");

        Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
        Assert.assertTrue(outputCapture.toString().contains("Check SPELL_CHECKER failed with error"));
        Assert.assertTrue(outputCapture.toString().contains("Spelling failures found:"));
        Assert.assertTrue(outputCapture.toString().contains("The string 'This is a new sorce string with spelling failure' contains misspelled words:"));
        Assert.assertTrue(outputCapture.toString().contains("\t* 'sorce' - Did you mean "));
        Assert.assertTrue(outputCapture.toString().contains("Please correct any spelling errors in a new commit."));
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
        Assert.assertTrue(outputCapture.toString().contains("\t* SPELL_CHECKER"));
        Assert.assertTrue(outputCapture.toString().contains("Sending notifications."));
        Assert.assertTrue(outputCapture.toString().contains("Checks completed"));

        // Verify hard failure output is not contained in soft fails
        Assert.assertFalse(outputCapture.toString().contains("SPELL_CHECKER failed with error"));
        Assert.assertFalse(outputCapture.toString().contains("Spelling failures found:"));
        Assert.assertFalse(outputCapture.toString().contains("The string 'This is a new sorce string with spelling failure' contains misspelled words:"));
        Assert.assertFalse(outputCapture.toString().contains("\t* 'sorce' - Did you mean "));
        Assert.assertFalse(outputCapture.toString().contains("Please correct any spelling errors in a new commit."));
    }
}
