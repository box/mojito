package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Message;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ExtractionDiffNotifierSlackTest {

  @Test
  public void sendDiffStatistics() throws SlackClientException {
    ExtractionDiffNotifierSlack extractionDiffNotifierSlack =
        getBaseExtractionDiffNotifierSlackForTest();
    SlackClient mockSlackClient = extractionDiffNotifierSlack.slackClient;
    String message =
        extractionDiffNotifierSlack.sendDiffStatistics(ExtractionDiffStatistics.builder().build());
    assertThat(message).isEqualTo("ℹ️ 0 strings removed and 0 strings added (from 0 to 0)");

    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(mockSlackClient).sendInstantMessage(messageArgumentCaptor.capture());
    Message messageArgumentCaptorValue = messageArgumentCaptor.getValue();
    assertThat(messageArgumentCaptorValue.getAttachments().get(0).getText())
        .isEqualTo("ℹ️ 0 strings removed and 0 strings added (from 0 to 0)");
  }

  @Test
  public void getAttachmentColorInfo() {
    ExtractionDiffNotifierSlack extractionDiffNotifierSlack =
        getBaseExtractionDiffNotifierSlackForTest();
    String attachmentColor =
        extractionDiffNotifierSlack.getAttachmentColor(ExtractionDiffStatistics.builder().build());
    assertThat(attachmentColor).isEqualTo(SlackClient.COLOR_GOOD);
  }

  @Test
  public void getAttachmentColorWarning() {
    ExtractionDiffNotifierSlack extractionDiffNotifierSlack =
        getBaseExtractionDiffNotifierSlackForTest();
    String attachmentColor =
        extractionDiffNotifierSlack.getAttachmentColor(
            ExtractionDiffStatistics.builder().removed(10).build());
    assertThat(attachmentColor).isEqualTo(SlackClient.COLOR_WARNING);
  }

  @Test
  public void getAttachmentColorError() {
    ExtractionDiffNotifierSlack extractionDiffNotifierSlack =
        getBaseExtractionDiffNotifierSlackForTest();
    String attachmentColor =
        extractionDiffNotifierSlack.getAttachmentColor(
            ExtractionDiffStatistics.builder().removed(100).build());
    assertThat(attachmentColor).isEqualTo(SlackClient.COLOR_DANGER);
  }

  private static ExtractionDiffNotifierSlack getBaseExtractionDiffNotifierSlackForTest() {
    return new ExtractionDiffNotifierSlack(
        mock(SlackClient.class),
        "{0}@mojito.org",
        false,
        new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
        "testname");
  }
}
