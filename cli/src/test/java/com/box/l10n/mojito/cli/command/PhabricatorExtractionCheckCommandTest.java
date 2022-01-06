package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

public class PhabricatorExtractionCheckCommandTest extends CLITestBase {

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

        getL10nJCommander().run("phab-extraction-check",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "CONTEXT_COMMENT_CHECKER");

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

        getL10nJCommander().run("phab-extraction-check",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "CONTEXT_COMMENT_CHECKER",
                "-hf", "CONTEXT_COMMENT_CHECKER");

        Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
        Assert.assertTrue(outputCapture.toString().contains("The following checks had hard failures:" + System.lineSeparator()));
        Assert.assertTrue(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
        Assert.assertTrue(outputCapture.toString().contains("Context and comment check found failures:"));
        Assert.assertTrue(outputCapture.toString().contains("* Source string 'This is a new source string missing a context' failed check with error: Context string is empty."));
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

        getL10nJCommander().run("phab-extraction-check",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "CONTEXT_COMMENT_CHECKER");

        Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
        Assert.assertTrue(outputCapture.toString().contains("Failed checks: "));
        Assert.assertTrue(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
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

        getL10nJCommander().run("phab-extraction-check",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "INVALID_CHECK_NAME");

        Assert.assertTrue(outputCapture.toString().contains("Invalid type [INVALID_CHECK_NAME]"));
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

        getL10nJCommander().run("phab-extraction-check",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "CONTEXT_COMMENT_CHECKER",
                "-hf", "INVALID_NAME");

        Assert.assertTrue(outputCapture.toString().contains("Unknown check name in hard fail list 'INVALID_NAME'"));
    }

    @Test
    public void runCheckWithPhabricatorOverride() {
        PhabricatorExtractionCheckCommand phabricatorExtractionCheckCommand = new PhabricatorExtractionCheckCommand();
        phabricatorExtractionCheckCommand.phabObjectId = "D123456";
        DifferentialRevision differentialRevisionMock = Mockito.mock(DifferentialRevision.class);
        when(differentialRevisionMock.getTestPlan(isA(String.class))).thenReturn("A string containing " + PhabricatorExtractionCheckCommand.SKIP_I18N_CHECKS_FLAG);
        phabricatorExtractionCheckCommand.differentialRevision = differentialRevisionMock;
        Assert.assertTrue(phabricatorExtractionCheckCommand.areChecksSkipped());
    }

    @Test
    public void runCheckWithNoPhabricatorOverride() {
        PhabricatorExtractionCheckCommand phabricatorExtractionCheckCommand = new PhabricatorExtractionCheckCommand();
        phabricatorExtractionCheckCommand.phabObjectId = "D123456";
        DifferentialRevision differentialRevisionMock = Mockito.mock(DifferentialRevision.class);
        when(differentialRevisionMock.getTestPlan(isA(String.class))).thenReturn("A test plan string doing test plan string things.");
        phabricatorExtractionCheckCommand.differentialRevision = differentialRevisionMock;
        Assert.assertFalse(phabricatorExtractionCheckCommand.areChecksSkipped());
    }

}
