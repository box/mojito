package com.box.l10n.mojito.service.sla.email;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.mustache.MustacheTemplateEngine; 
import static com.box.l10n.mojito.service.sla.email.SlaCheckerEmailService.CLOSE_INCIDENT_TEMPLATE;
import static com.box.l10n.mojito.service.sla.email.SlaCheckerEmailService.OPEN_INCIDENT_TEMPLATE;
import com.box.l10n.mojito.utils.DateTimeUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.joda.time.DateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 *
 * @author jeanaurambault
 */
@RunWith(MockitoJUnitRunner.class)
public class SlaCheckerEmailServiceTest {

    @Spy
    @InjectMocks
    SlaCheckerEmailService slaCheckerEmailService;

    @Mock
    DateTimeUtils dateTimeUtils;

    @Spy
    JavaMailSenderImpl emailSender;

    @Spy
    SlaCheckerEmailConfig slaCheckerEmailConfig;

    @Mock
    MustacheTemplateEngine mustacheTemplateEngine;

    @Before
    public void before() {
        slaCheckerEmailConfig.from = "from@test.com";
        slaCheckerEmailConfig.to = new String[]{"to@test.com"};

        doNothing().when(emailSender).send(Matchers.any(MimeMessage.class));
    }

    @Test
    public void testSendCloseIncidentEmail() {
        String emailContent = "close content";
        long incidentId = 0L;
        doNothing().when(slaCheckerEmailService).sendEmail(incidentId, emailContent);
        doReturn(emailContent).when(slaCheckerEmailService).getCloseIncidentEmailContent(incidentId);

        slaCheckerEmailService.sendCloseIncidentEmail(incidentId);

        Mockito.verify(slaCheckerEmailService).sendEmail(incidentId, emailContent);
    }

    @Test
    public void testSendOpenIncidentEmail() {
        String emailContent = "open content";
        long incidentId = 0L;
        List<Repository> repositories = new ArrayList<>();
        doNothing().when(slaCheckerEmailService).sendEmail(incidentId, emailContent);
        doReturn(emailContent).when(slaCheckerEmailService).getOpenIncidentEmailContent(incidentId, repositories);

        slaCheckerEmailService.sendOpenIncidentEmail(incidentId, repositories);

        Mockito.verify(slaCheckerEmailService).sendEmail(incidentId, emailContent);
    }

    @Test
    public void testShouldResendEmailYes() {
        DateTime now = new DateTime("2018-06-08T14:00:00.000-07:00");
        doReturn(now).when(dateTimeUtils).now();

        DateTime previousEmailDateTime = new DateTime("2018-06-08T12:00:00.000-07:00");
        boolean shouldResendEmail = slaCheckerEmailService.shouldResendEmail(previousEmailDateTime);
        assertTrue(shouldResendEmail);
    }

    @Test
    public void testShouldResendEmailNo() {
        DateTime now = new DateTime("2018-06-08T14:00:00.000-07:00");
        doReturn(now).when(dateTimeUtils).now();

        DateTime previousEmailDateTime = new DateTime("2018-06-08T13:30:00.000-07:00");
        boolean shouldResendEmail = slaCheckerEmailService.shouldResendEmail(previousEmailDateTime);
        assertFalse(shouldResendEmail);
    }

    @Test
    public void testGetCloseIncidentEmailContent() {
        long incidentId = 0L;
        String renderedContent = "rendered content";

        doReturn(renderedContent).when(mustacheTemplateEngine).render(eq(CLOSE_INCIDENT_TEMPLATE), any(OpenIncidentContext.class));
        String result = slaCheckerEmailService.getCloseIncidentEmailContent(incidentId);

        ArgumentCaptor<CloseIncidentContext> acCloseIncidentEmailContext = ArgumentCaptor.forClass(CloseIncidentContext.class);
        verify(mustacheTemplateEngine).render(eq(CLOSE_INCIDENT_TEMPLATE), acCloseIncidentEmailContext.capture());

        assertEquals(renderedContent, result);
        assertEquals((long) incidentId, (long) acCloseIncidentEmailContext.getValue().incidentId);
    }

    @Test
    public void testGetOpenIncidentEmailContent() {
        long incidentId = 0L;
        String renderedContent = "rendered content";

        doReturn(renderedContent).when(mustacheTemplateEngine).render(eq(OPEN_INCIDENT_TEMPLATE), any(OpenIncidentContext.class));
        List<Repository> repositoriesForTest = new ArrayList<>();
        String result = slaCheckerEmailService.getOpenIncidentEmailContent(incidentId, repositoriesForTest);

        ArgumentCaptor<OpenIncidentContext> acOpenIncidentEmailContext = ArgumentCaptor.forClass(OpenIncidentContext.class);
        verify(mustacheTemplateEngine).render(eq(OPEN_INCIDENT_TEMPLATE), acOpenIncidentEmailContext.capture());

        assertEquals(renderedContent, result);
        assertEquals((long) incidentId, (long) acOpenIncidentEmailContext.getValue().incidentId);
        assertEquals(slaCheckerEmailConfig.mojitoUrl, acOpenIncidentEmailContext.getValue().mojitoUrl);
        assertEquals(repositoriesForTest, acOpenIncidentEmailContext.getValue().repositories);
    }

    @Test
    public void testSendEmail() throws MessagingException, IOException {
        long incidentId = 0L;
        String message = "some message";

        slaCheckerEmailService.sendEmail(incidentId, message);

        ArgumentCaptor<MimeMessage> argument = ArgumentCaptor.forClass(MimeMessage.class);
        verify(emailSender).send(argument.capture());

        assertEquals("Out of SLA strings, incident #0", argument.getValue().getSubject());
        assertEquals(slaCheckerEmailConfig.from, argument.getValue().getFrom()[0].toString());
        assertEquals(slaCheckerEmailConfig.to[0], argument.getValue().getAllRecipients()[0].toString());

        MimeMultipart mimeMultipart = ((MimeMultipart) argument.getValue().getContent());
        MimeMultipart mimeMultipart2 = (MimeMultipart) mimeMultipart.getBodyPart(0).getContent();
        String emailMessage = (String) mimeMultipart2.getBodyPart(0).getContent();
        assertEquals(message, emailMessage);
    }

    @Test
    public void testSendEmailError() throws MessagingException, IOException {
        long incidentId = 0L;
        String message = "some message";

        MailException mailException = mock(MailException.class);
        doThrow(mailException).when(emailSender).send(any(MimeMessage.class));

        SlaCheckerEmailService.logger = mock(Logger.class);

        slaCheckerEmailService.sendEmail(incidentId, message);

        verify(SlaCheckerEmailService.logger).error("Can't send OOSLA email", mailException);
    }

    @Test
    public void testGetSubject() {
        long incidentId = 0L;
        String expResult = "Out of SLA strings, incident #0";
        String result = slaCheckerEmailService.getSubject(incidentId);
        assertEquals(expResult, result);
    }
    
}
