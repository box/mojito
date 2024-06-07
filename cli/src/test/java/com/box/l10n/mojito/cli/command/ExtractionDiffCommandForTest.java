package com.box.l10n.mojito.cli.command;

import static java.util.Optional.empty;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffNotificationSender;
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
  private Optional<ExtractionDiffNotificationSender> mockedNotificationSender = empty();

  @Override
  protected Optional<ExtractionDiffNotificationSender> getNotificationSender() {
    Optional<ExtractionDiffNotificationSender> notificationSender = super.getNotificationSender();
    this.mockedNotificationSender = notificationSender.map(Mockito::spy);
    this.mockedNotificationSender.ifPresent(
        extractionDiffNotificationSender ->
            doNothing().when(extractionDiffNotificationSender).sendMessage(anyString()));
    return this.mockedNotificationSender;
  }

  public Optional<ExtractionDiffNotificationSender> getMockedNotificationSender() {
    return this.mockedNotificationSender;
  }
}
