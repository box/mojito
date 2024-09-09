package com.box.l10n.mojito.common.notification;

import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.SlackClients;
import com.box.l10n.mojito.slack.request.Message;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sends warning notifications to the specified warning channel on Slack. Selects default Slack
 * client specified in app properties. The channel name for warnings must be placed in the app
 * properties under: l10n.integrity-check-notifier.slackChannel
 *
 * @author mattwilshire
 */
@Component
@ConditionalOnProperty(value = "l10n.integrity-check-notifier.enabled", havingValue = "true")
public class IntegrityCheckNotifier {

  static Logger logger = LoggerFactory.getLogger(IntegrityCheckNotifier.class);

  private final SlackClients slackClients;
  private final TMTextUnitRepository tmTextUnitRepository;
  private final IntegrityCheckNotifierConfiguration integrityCheckNotifierConfiguration;
  private final SlackMessageBuilder slackMessageBuilder;
  private SlackClient slackClient;

  @Autowired
  public IntegrityCheckNotifier(
      SlackClients slackClients,
      TMTextUnitRepository tmTextUnitRepository,
      IntegrityCheckNotifierConfiguration integrityCheckNotifierConfiguration,
      SlackMessageBuilder slackMessageBuilder) {
    this.slackClients = slackClients;
    this.tmTextUnitRepository = tmTextUnitRepository;
    this.integrityCheckNotifierConfiguration = integrityCheckNotifierConfiguration;
    this.slackMessageBuilder = slackMessageBuilder;
  }

  @PostConstruct
  public void init() throws IntegrityCheckNotifierException {
    // This method will only be called if slack-warnings are enabled in app properties.
    String slackClientId = integrityCheckNotifierConfiguration.getSlackClientId();

    if (slackClientId == null)
      throw new IntegrityCheckNotifierException("Slack client id not defined.");

    if (integrityCheckNotifierConfiguration.getSlackChannel() == null)
      throw new IntegrityCheckNotifierException("Slack channel not defined.");

    if (!integrityCheckNotifierConfiguration.getSlackChannel().startsWith("#"))
      throw new IntegrityCheckNotifierException("Slack channel must start with #.");

    this.slackClient = slackClients.getById(slackClientId);

    if (slackClient == null) {
      throw new IntegrityCheckNotifierException("Slack client id defined but doesn't exist.");
    }
  }

  public void sendWarning(Message warning) throws SlackClientException {
    if (slackClient == null) {
      logger.warn("Attempted to send Slack warning but there was no slack client.");
      return;
    }
    if (integrityCheckNotifierConfiguration.getSlackChannel() == null) {
      logger.warn("Attempted to send Slack warning but there was no slack channel defined.");
      return;
    }

    warning.setChannel(integrityCheckNotifierConfiguration.getSlackChannel());
    slackClient.sendInstantMessage(warning);
  }

  public IntegrityCheckNotifierConfiguration getConfiguration() {
    return integrityCheckNotifierConfiguration;
  }
}
