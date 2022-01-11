package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import org.fusesource.jansi.Ansi;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtractionCheckCommandTest extends CLITestBase {

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

        getL10nJCommander().run("extraction-check",
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

        getL10nJCommander().run("extraction-check",
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

        getL10nJCommander().run("extraction-check",
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

        getL10nJCommander().run("extraction-check",
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

        getL10nJCommander().run("extraction-check",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-cl", "CONTEXT_COMMENT_CHECKER",
                "-hf", "INVALID_NAME");

        Assert.assertTrue(outputCapture.toString().contains("Unknown check name in hard fail list 'INVALID_NAME'"));
    }

    @Test
    public void testChecksSkippedIfSkipChecksEnabled() {
        ConsoleWriter consoleWriter = Mockito.mock(ConsoleWriter.class);
        ExtractionCheckCommand extractionCheckCommand = Mockito.spy(new ExtractionCheckCommand());
        extractionCheckCommand.consoleWriter = consoleWriter;
        extractionCheckCommand.areChecksSkipped = true;
        when(consoleWriter.fg(isA(Ansi.Color.class))).thenReturn(consoleWriter);
        when(consoleWriter.newLine()).thenReturn(consoleWriter);
        when(consoleWriter.a(isA(String.class))).thenReturn(consoleWriter);
        extractionCheckCommand.execute();
        verify(consoleWriter, times(1)).a("Checks disabled as --skip-checks is set to true.");
    }

}
