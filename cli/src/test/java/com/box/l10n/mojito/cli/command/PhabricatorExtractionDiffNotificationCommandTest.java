package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class PhabricatorExtractionDiffNotificationCommandTest extends CLITestBase {

    @Test
    public void getMessageInfo() {
        PhabricatorExtractionDiffNotificationCommand phabricatorExtractionDiffNotificationCommand = new PhabricatorExtractionDiffNotificationCommand();
        assertEquals("ℹ️ 0 strings removed and 1 string added (from 10 to 11)", phabricatorExtractionDiffNotificationCommand.getMessage(ExtractionDiffStatistics.builder().added(1).removed(0).base(10).current(11).build()));
    }

    @Test
    public void getMessageWarning() {
        PhabricatorExtractionDiffNotificationCommand phabricatorExtractionDiffNotificationCommand = new PhabricatorExtractionDiffNotificationCommand();
        assertEquals("⚠️ 10 strings removed and 8 strings added (from 20 to 18)", phabricatorExtractionDiffNotificationCommand.getMessage(ExtractionDiffStatistics.builder().added(8).removed(10).base(20).current(18).build()));
    }

    @Test
    public void getMessageError() {
        PhabricatorExtractionDiffNotificationCommand phabricatorExtractionDiffNotificationCommand = new PhabricatorExtractionDiffNotificationCommand();
        assertEquals("🛑 200 strings removed and 0 strings added (from 500 to 300)", phabricatorExtractionDiffNotificationCommand.getMessage(ExtractionDiffStatistics.builder().added(0).removed(200).base(500).current(300).build()));
    }

    @Test
    public void withTemplate() {
        PhabricatorExtractionDiffNotificationCommand phabricatorExtractionDiffNotificationCommand = new PhabricatorExtractionDiffNotificationCommand();
        phabricatorExtractionDiffNotificationCommand.messageTemplate = "{baseMessage}. Check [[https://build.org/1234|build]].";
        assertEquals("🛑 200 strings removed and 0 strings added (from 500 to 300). Check [[https://build.org/1234|build]].",
                phabricatorExtractionDiffNotificationCommand.getMessage(
                        ExtractionDiffStatistics.builder().added(0).removed(200).base(500).current(300).build()));
    }

    @Test
    public void shouldSendNotification() {
        PhabricatorExtractionDiffNotificationCommand phabricatorExtractionDiffNotificationCommand = new PhabricatorExtractionDiffNotificationCommand();
        assertTrue(phabricatorExtractionDiffNotificationCommand.shouldSendNotification(
                ExtractionDiffStatistics.builder().added(0).removed(200).base(500).current(300).build()));
    }

    @Test
    public void shouldNotSendNotification() {
        PhabricatorExtractionDiffNotificationCommand phabricatorExtractionDiffNotificationCommand = new PhabricatorExtractionDiffNotificationCommand();
        assertFalse(phabricatorExtractionDiffNotificationCommand.shouldSendNotification(
                ExtractionDiffStatistics.builder().added(0).removed(0).base(500).current(300).build()));
    }

    @Test
    public void sendNotification() throws Exception {

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source1",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source2",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1");

        L10nJCommander l10nJCommander = getL10nJCommander();
        PhabricatorExtractionDiffNotificationCommand command = l10nJCommander.getCommand(PhabricatorExtractionDiffNotificationCommand.class);
        DifferentialRevision mock = Mockito.mock(DifferentialRevision.class);
        command.differentialRevision = mock;

        l10nJCommander.run("phab-extraction-diff-notif",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-mt", "{baseMessage} in diff: ${DIFF_ID}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details.",
                "-oid", "{objectId}");

        Mockito.verify(mock, Mockito.times(1)).addComment("{objectId}", "⚠️ 5 strings removed and 2 strings added (from 10 to 7) in diff: ${DIFF_ID}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details.");
        assertTrue(outputCapture.toString().contains("⚠️ 5 strings removed and 2 strings added (from 10 to 7) in diff: ${DIFF_ID}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details."));
        checkExpectedGeneratedResources();
    }

    @Test
    public void noNotifications() throws Exception {

        L10nJCommander l10nJCommander = getL10nJCommander();
        PhabricatorExtractionDiffNotificationCommand command = l10nJCommander.getCommand(PhabricatorExtractionDiffNotificationCommand.class);
        DifferentialRevision mock = Mockito.mock(DifferentialRevision.class);
        command.differentialRevision = mock;
        command.extractionDiffService = Mockito.mock(ExtractionDiffService.class);
        Mockito.when(command.extractionDiffService.computeExtractionDiffStatistics(any())).thenReturn(ExtractionDiffStatistics.builder().build());

        l10nJCommander.run("phab-extraction-diff-notif",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "-mt", "{baseMessage} in diff: ${DIFF_ID}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details.",
                "-oid", "{objectId}");

        Mockito.verify(mock, Mockito.never()).addComment(anyString(), anyString());
        assertTrue(outputCapture.toString().contains("No need to send notification"));
    }
}