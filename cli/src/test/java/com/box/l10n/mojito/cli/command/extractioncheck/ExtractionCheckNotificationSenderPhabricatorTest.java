package com.box.l10n.mojito.cli.command.extractioncheck;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.thirdpartynotification.phabricator.PhabricatorIcon;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ExtractionCheckNotificationSenderPhabricatorTest.class})
public class ExtractionCheckNotificationSenderPhabricatorTest {

  @TestConfiguration
  static class ExtractionCheckNotificationSenderPhabricatorTestConfiguration {

    @Bean
    public DifferentialRevision differentialRevision() {
      return Mockito.mock(DifferentialRevision.class);
    }
  }

  @MockBean DifferentialRevision differentialRevisionMock;

  @Captor ArgumentCaptor<String> objectIdCaptor;

  @Captor ArgumentCaptor<String> messageCaptor;

  ExtractionCheckNotificationSenderPhabricator extractionCheckNotificationSenderPhabricator;

  @Before
  public void setup() {
    extractionCheckNotificationSenderPhabricator =
        new ExtractionCheckNotificationSenderPhabricator(
            "D12345",
            "{baseMessage}",
            "This is a hard failure message",
            "This is a checks skipped message");
    extractionCheckNotificationSenderPhabricator.differentialRevision = differentialRevisionMock;
  }

  @Test
  public void testSendFailureNotifications() {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderPhabricator.sendFailureNotification(results, false);
    verify(differentialRevisionMock, times(1))
        .addComment(objectIdCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(objectIdCaptor.getValue().equals("D12345"));
    Assert.assertTrue(messageCaptor.getValue().contains(PhabricatorIcon.WARNING.toString()));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
  }

  @Test
  public void testSendFailureNotificationsMultipleFailures() {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    CliCheckResult result2 = new CliCheckResult(false, false, "Other Check");
    result2.setNotificationText("Some other notification text");
    results.add(result);
    results.add(result2);
    extractionCheckNotificationSenderPhabricator.sendFailureNotification(results, false);
    verify(differentialRevisionMock, times(1))
        .addComment(objectIdCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(objectIdCaptor.getValue().equals("D12345"));
    Assert.assertTrue(messageCaptor.getValue().contains(PhabricatorIcon.WARNING.toString()));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
    Assert.assertTrue(messageCaptor.getValue().contains("Other Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some other notification text"));
  }

  @Test
  public void testSendFailureNotificationsHardFail() {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, true, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderPhabricator.sendFailureNotification(results, true);
    verify(differentialRevisionMock, times(1))
        .addComment(objectIdCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(objectIdCaptor.getValue().equals("D12345"));
    Assert.assertTrue(
        messageCaptor
            .getValue()
            .contains(PhabricatorIcon.WARNING + " **i18n source string checks failed**"));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
    Assert.assertTrue(messageCaptor.getValue().contains("This is a hard failure message"));
  }

  @Test
  public void testSendChecksSkippedNotification() {
    extractionCheckNotificationSenderPhabricator.sendChecksSkippedNotification();
    verify(differentialRevisionMock, times(1))
        .addComment(objectIdCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(objectIdCaptor.getValue().equals("D12345"));
    Assert.assertTrue(
        messageCaptor
            .getValue()
            .equals(PhabricatorIcon.WARNING + " This is a checks skipped message"));
  }

  @Test
  public void testNoNotificationsSentIfNoFailuresInResultList() {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(true, true, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderPhabricator.sendFailureNotification(results, true);
    verify(differentialRevisionMock, times(0)).addComment(isA(String.class), isA(String.class));
  }

  @Test
  public void testNoNotificationsSentIfNullListSent() {
    extractionCheckNotificationSenderPhabricator.sendFailureNotification(null, true);
    verify(differentialRevisionMock, times(0)).addComment(isA(String.class), isA(String.class));
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfNoObjectIdProvided() {
    new ExtractionCheckNotificationSenderPhabricator("", "some template", "", "");
  }

  @Test
  public void testQuoteMarkersAreReplaced() {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, true, "Test Check");
    result.setNotificationText(
        "Some notification text for " + QUOTE_MARKER + "some.text.id" + QUOTE_MARKER);
    results.add(result);
    extractionCheckNotificationSenderPhabricator.sendFailureNotification(results, true);
    verify(differentialRevisionMock, times(1))
        .addComment(objectIdCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(objectIdCaptor.getValue().equals("D12345"));
    Assert.assertTrue(
        messageCaptor
            .getValue()
            .contains(PhabricatorIcon.WARNING + " **i18n source string checks failed**"));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
    Assert.assertTrue(messageCaptor.getValue().contains("This is a hard failure message"));
    Assert.assertTrue(messageCaptor.getValue().contains("`some.text.id`"));
  }
}
