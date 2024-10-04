package com.box.l10n.mojito.cli.command;

import static java.util.Optional.empty;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.utils.SlackNotificationSender;
import java.util.Optional;
import org.mockito.Mockito;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"extract-diff-test"},
    commandDescription = "Class to test Slack notifications for the extract-diff command")
public class ExtractionDiffCommandForTest extends ExtractionDiffCommand {
  private Optional<SlackNotificationSender> mockedNotificationSender = empty();

  @Override
  protected Optional<SlackNotificationSender> getNotificationSender() {
    Optional<SlackNotificationSender> notificationSender = super.getNotificationSender();
    this.mockedNotificationSender = notificationSender.map(Mockito::spy);
    this.mockedNotificationSender.ifPresent(
        slackNotificationSender ->
            doNothing().when(slackNotificationSender).sendMessage(anyString(), anyString()));
    return this.mockedNotificationSender;
  }

  public Optional<SlackNotificationSender> getMockedNotificationSender() {
    return this.mockedNotificationSender;
  }
}
