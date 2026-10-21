package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.RepositoryAiTranslateClient;
import com.box.l10n.mojito.rest.client.RepositoryAiTranslateClient.ProtoAiTranslateGetReportLocaleResponse;
import com.box.l10n.mojito.rest.client.RepositoryAiTranslateClient.ProtoAiTranslateGetReportResponse;
import com.box.l10n.mojito.rest.client.RepositoryAiTranslateClient.ProtoAiTranslateResponse;
import com.box.l10n.mojito.rest.entity.PollableTask;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.system.OutputCaptureRule;

/**
 * Unit tests for non-batch AI translate report handling: OpenAI/locale failures must fail the CLI
 * instead of printing Finished with exit 0.
 */
public class RepositoryAiTranslationCommandTest {

  @Rule public OutputCaptureRule outputCapture = new OutputCaptureRule();

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  RepositoryAiTranslateClient repositoryAiTranslateClient;
  PollableTaskClient pollableTaskClient;
  CommandHelper commandHelper;
  RepositoryAiTranslationCommand command;

  @Before
  public void setUp() {
    repositoryAiTranslateClient = mock(RepositoryAiTranslateClient.class);
    pollableTaskClient = mock(PollableTaskClient.class);
    commandHelper = mock(CommandHelper.class);

    command = new RepositoryAiTranslationCommand();
    command.repositoryAiTranslateClient = repositoryAiTranslateClient;
    command.pollableTaskClient = pollableTaskClient;
    command.commandHelper = commandHelper;
    command.objectMapper = new ObjectMapper();
    command.consoleWriter =
        new ConsoleWriter(false, ConsoleWriter.OutputType.ANSI_CONSOLE_AND_LOGGER);
    command.repositoryParam = "Pendo";
    command.locales = List.of("bn-IN");
    command.useBatch = false;
    command.downloadReport = false;
  }

  @Test
  public void failsWhenNonBatchReportContainsError() {
    stubSuccessfulTranslate(46945665L);
    when(repositoryAiTranslateClient.getReport(46945665L))
        .thenReturn(new ProtoAiTranslateGetReportResponse(List.of("46945665/locale/bn-IN")));
    when(repositoryAiTranslateClient.getReportLocale("46945665/locale/bn-IN"))
        .thenReturn(
            new ProtoAiTranslateGetReportLocaleResponse(
                """
                [{"tmTexUnitId":36486276,"locale":"bn-IN","error":"Error when getting the responsesResponse: java.lang.RuntimeException: java.net.SocketTimeoutException: Read timed out","tmTextUnitCurrentVariantUpdated":false}]
                """));

    try {
      command.execute();
      fail("Expected CommandException");
    } catch (CommandException e) {
      assertEquals(
          "AI translate completed with 1 error(s) (task id: 46945665)", e.getMessage());
    }

    String output = outputCapture.toString();
    assertTrue(output.contains("AI translate failed for 1 text unit(s):"));
    assertTrue(output.contains("locale: bn-IN"));
    assertTrue(output.contains("tmTextUnitIds: [36486276]"));
    assertTrue(output.contains("SocketTimeoutException"));
    assertFalse(output.contains("Finished"));
  }

  @Test
  public void finishesWhenNonBatchReportHasNoErrors() throws Exception {
    stubSuccessfulTranslate(100L);
    when(repositoryAiTranslateClient.getReport(100L))
        .thenReturn(new ProtoAiTranslateGetReportResponse(List.of("100/locale/bn-IN")));
    when(repositoryAiTranslateClient.getReportLocale("100/locale/bn-IN"))
        .thenReturn(
            new ProtoAiTranslateGetReportLocaleResponse(
                """
                [{"tmTexUnitId":36486276,"locale":"bn-IN","error":null,"tmTextUnitCurrentVariantUpdated":true}]
                """));

    command.execute();

    String output = outputCapture.toString();
    assertTrue(output.contains("Finished"));
    assertFalse(output.contains("AI translate failed"));
  }

  @Test
  public void finishesWhenReportHasNoLocaleUrls() throws Exception {
    stubSuccessfulTranslate(200L);
    when(repositoryAiTranslateClient.getReport(200L))
        .thenReturn(new ProtoAiTranslateGetReportResponse(List.of()));

    command.execute();

    assertTrue(outputCapture.toString().contains("Finished"));
  }

  @Test
  public void writesReportFileBeforeFailingWhenDownloadReportAndError() throws Exception {
    Path reportDir = temporaryFolder.newFolder("report-out").toPath();
    String reportLocaleUrl = reportDir.resolve("locale").resolve("bn-IN").toString();

    stubSuccessfulTranslate(300L);
    command.downloadReport = true;
    when(repositoryAiTranslateClient.getReport(300L))
        .thenReturn(new ProtoAiTranslateGetReportResponse(List.of(reportLocaleUrl)));
    when(repositoryAiTranslateClient.getReportLocale(reportLocaleUrl))
        .thenReturn(
            new ProtoAiTranslateGetReportLocaleResponse(
                """
                [{"tmTexUnitId":36486276,"locale":"bn-IN","error":"Read timed out","tmTextUnitCurrentVariantUpdated":false}]
                """));

    try {
      command.execute();
      fail("Expected CommandException");
    } catch (CommandException e) {
      assertEquals("AI translate completed with 1 error(s) (task id: 300)", e.getMessage());
    }

    Path written = Path.of(reportLocaleUrl + ".json");
    assertTrue(Files.exists(written));
    assertTrue(Files.readString(written).contains("Read timed out"));

    String output = outputCapture.toString();
    assertTrue(output.contains(written.toAbsolutePath().toString()));
    assertTrue(output.contains("tmTextUnitId: 36486276"));
    assertTrue(output.contains("Read timed out"));
    assertFalse(output.contains("Finished"));
  }

  @Test
  public void aggregatesErrorsAcrossLocalesAndOmitsSuccessfulLines() {
    stubSuccessfulTranslate(400L);
    command.locales = List.of("bn-IN", "hi-IN");
    when(repositoryAiTranslateClient.getReport(400L))
        .thenReturn(
            new ProtoAiTranslateGetReportResponse(
                List.of("400/locale/bn-IN", "400/locale/hi-IN")));
    when(repositoryAiTranslateClient.getReportLocale("400/locale/bn-IN"))
        .thenReturn(
            new ProtoAiTranslateGetReportLocaleResponse(
                """
                [{"tmTexUnitId":1,"locale":"bn-IN","error":null,"tmTextUnitCurrentVariantUpdated":true},{"tmTexUnitId":2,"locale":"bn-IN","error":"timeout A","tmTextUnitCurrentVariantUpdated":false}]
                """));
    when(repositoryAiTranslateClient.getReportLocale("400/locale/hi-IN"))
        .thenReturn(
            new ProtoAiTranslateGetReportLocaleResponse(
                """
                [{"tmTexUnitId":3,"locale":"hi-IN","error":"timeout B","tmTextUnitCurrentVariantUpdated":false},{"tmTexUnitId":4,"locale":"hi-IN","error":null,"tmTextUnitCurrentVariantUpdated":true}]
                """));

    try {
      command.execute();
      fail("Expected CommandException");
    } catch (CommandException e) {
      assertEquals("AI translate completed with 2 error(s) (task id: 400)", e.getMessage());
    }

    String output = outputCapture.toString();
    assertTrue(output.contains("AI translate failed for 2 text unit(s):"));
    assertTrue(output.contains("tmTextUnitIds: [2]"));
    assertTrue(output.contains("tmTextUnitIds: [3]"));
    assertTrue(output.contains("timeout A"));
    assertTrue(output.contains("timeout B"));
    assertFalse(output.contains("tmTextUnitIds: [1]"));
    assertFalse(output.contains("tmTextUnitIds: [4]"));
    assertFalse(output.contains("Finished"));
  }

  private void stubSuccessfulTranslate(long pollableTaskId) {
    PollableTask pollableTask = new PollableTask();
    pollableTask.setId(pollableTaskId);
    pollableTask.setAllFinished(true);
    when(repositoryAiTranslateClient.translateRepository(any()))
        .thenReturn(new ProtoAiTranslateResponse(pollableTask));
    doNothing().when(commandHelper).waitForPollableTask(anyLong());
  }
}
