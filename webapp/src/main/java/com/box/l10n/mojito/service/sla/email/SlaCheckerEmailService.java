package com.box.l10n.mojito.service.sla.email;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.mustache.MustacheTemplateEngine;
import com.box.l10n.mojito.utils.DateTimeUtils;
import com.box.l10n.mojito.utils.ServerConfig;
import com.ibm.icu.text.MessageFormat;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * @author jeanaurambault
 */
@Component
public class SlaCheckerEmailService {

  static Logger logger = LoggerFactory.getLogger(SlaCheckerEmailService.class);

  static final String EMAIL_SUBJECT = "Out of SLA strings, incident #{incidentId}";
  static final String OPEN_INCIDENT_TEMPLATE = "email/sla/openIncident.html";
  static final String CLOSE_INCIDENT_TEMPLATE = "email/sla/closeIncident.html";

  @Autowired SlaCheckerEmailConfig slaCheckerEmailConfig;

  @Autowired JavaMailSender emailSender;

  @Autowired ServerConfig serverConfig;

  @Autowired MustacheTemplateEngine mustacheTemplateEngine;

  @Autowired DateTimeUtils dateTimeUtils;

  public void sendCloseIncidentEmail(long incidentId) {
    logger.debug("Send close incident email");
    String closeIncidentEmailContent = getCloseIncidentEmailContent(incidentId);
    sendEmail(incidentId, closeIncidentEmailContent);
  }

  public void sendOpenIncidentEmail(long incidentId, List<Repository> ooslaRepositories) {
    logger.debug("Send open incident email");
    sendEmail(incidentId, getOpenIncidentEmailContent(incidentId, ooslaRepositories));
  }

  public boolean shouldResendEmail(ZonedDateTime previousEmailDateTime) {
    ZonedDateTime now = dateTimeUtils.now();
    return previousEmailDateTime.isBefore(now.minus(slaCheckerEmailConfig.getPeriodBetweenEmail()));
  }

  String getCloseIncidentEmailContent(long incidentId) {
    CloseIncidentContext slaCloseIncidentContext = new CloseIncidentContext(incidentId);
    return mustacheTemplateEngine.render(CLOSE_INCIDENT_TEMPLATE, slaCloseIncidentContext);
  }

  String getOpenIncidentEmailContent(long incidentId, List<Repository> repositories) {
    OpenIncidentContext slaEmailContext =
        new OpenIncidentContext(incidentId, repositories, serverConfig.getUrl());
    String emailContent = mustacheTemplateEngine.render(OPEN_INCIDENT_TEMPLATE, slaEmailContext);
    return emailContent;
  }

  void sendEmail(long incidentId, String message) {
    MimeMessage mailMessage = emailSender.createMimeMessage();

    try {
      mailMessage.setSubject(getSubject(incidentId), StandardCharsets.UTF_8.toString());

      MimeMessageHelper helper =
          new MimeMessageHelper(mailMessage, true, StandardCharsets.UTF_8.toString());
      helper.setFrom(slaCheckerEmailConfig.getFrom());
      helper.setTo(slaCheckerEmailConfig.getTo());
      helper.setText(message, true);

      emailSender.send(mailMessage);
    } catch (Exception ex) {
      logger.error("Can't send OOSLA email", ex);
    }
  }

  String getSubject(long incidentId) {
    Map<String, Object> params = new HashMap<>();
    params.put("incidentId", incidentId);
    return MessageFormat.format(EMAIL_SUBJECT, params);
  }
}
