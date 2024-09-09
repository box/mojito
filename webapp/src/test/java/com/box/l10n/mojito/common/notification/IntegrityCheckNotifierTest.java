package com.box.l10n.mojito.common.notification;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.SlackClients;
import com.box.l10n.mojito.slack.request.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IntegrityCheckNotifierTest {

  private IntegrityCheckNotifier integrityCheckNotifier;
  private IntegrityCheckNotifierConfiguration integrityCheckNotifierConfiguration;
  private SlackClients slackClients;
  private SlackClient slackClient;

  @BeforeEach
  public void setUp() {
    // Setup Slack clients
    slackClients = mock(SlackClients.class);
    slackClient = mock(SlackClient.class, withSettings().useConstructor("xxx_xxx_xxx"));
    when(slackClients.getById("test-slack-id")).thenReturn(slackClient);

    integrityCheckNotifierConfiguration = new IntegrityCheckNotifierConfiguration();
    integrityCheckNotifierConfiguration.setSlackChannel("#test-channel");
    integrityCheckNotifierConfiguration.setSlackClientId("test-slack-id");

    integrityCheckNotifier =
        new IntegrityCheckNotifier(slackClients, null, integrityCheckNotifierConfiguration, null);
  }

  @Test
  public void testDefaults() {
    try {
      integrityCheckNotifier.init();
    } catch (Exception e) {
      fail("IntegrityCheckNotifier should not be catching exceptions with default setup.");
    }
  }

  @Test
  public void testNullSlackClientId() {
    integrityCheckNotifierConfiguration.setSlackClientId(null);
    try {
      integrityCheckNotifier.init();
      fail("IntegrityCheckNotifier should be thrown for null slack clientId.");
    } catch (Exception e) {
      assertEquals(e.getMessage(), "Slack client id not defined.");
    }
  }

  @Test
  public void testNullSlackChannel() {
    integrityCheckNotifierConfiguration.setSlackChannel(null);
    try {
      integrityCheckNotifier.init();
      fail("IntegrityCheckNotifier should be thrown for null slack channel.");
    } catch (Exception e) {
      assertEquals(e.getMessage(), "Slack channel not defined.");
    }
  }

  @Test
  public void testBadSlackChannelFormat() {
    integrityCheckNotifierConfiguration.setSlackChannel("testing-bad-channel-format");
    try {
      integrityCheckNotifier.init();
      fail("IntegrityCheckNotifier should be thrown for null slack channel.");
    } catch (Exception e) {
      assertEquals(e.getMessage(), "Slack channel must start with #.");
    }
  }

  @Test
  public void testSlackClientNotFound() {
    when(slackClients.getById("test-slack-id")).thenReturn(null);
    try {
      integrityCheckNotifier.init();
      fail("IntegrityCheckNotifier should be thrown for null slack client..");
    } catch (Exception e) {
      assertEquals(e.getMessage(), "Slack client id defined but doesn't exist.");
    }
  }

  @Test
  public void testSendsMessageToSlackClient()
      throws IntegrityCheckNotifierException, SlackClientException {
    integrityCheckNotifier.init();
    Message message = new Message();
    when(slackClient.sendInstantMessage(message)).thenReturn(null);
    integrityCheckNotifier.sendWarning(message);
    verify(slackClient, times(1)).sendInstantMessage(message);
  }

  @Test
  public void testSendMessageNoChannel()
      throws IntegrityCheckNotifierException, SlackClientException {
    integrityCheckNotifier.init();
    integrityCheckNotifierConfiguration.setSlackChannel(null);
    Message message = new Message();
    when(slackClient.sendInstantMessage(message)).thenReturn(null);
    integrityCheckNotifier.sendWarning(message);
    verify(slackClient, never()).sendInstantMessage(message);
  }
}
