package com.box.l10n.mojito.cli.command.extractioncheck;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.thirdpartynotification.github.GithubIcon;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ExtractionCheckNotificationSenderGithubTest.class})
public class ExtractionCheckNotificationSenderGithubTest {

  @Mock GithubClients githubClientsMock;

  @Captor ArgumentCaptor<String> repoNameCaptor;

  @Captor ArgumentCaptor<Integer> prNumberCaptor;

  @Captor ArgumentCaptor<String> messageCaptor;

  GithubClient githubClientMock;

  ExtractionCheckNotificationSenderGithub extractionCheckNotificationSenderGithub;

  @Before
  public void setup() {
    githubClientMock = Mockito.mock(GithubClient.class);
    when(githubClientsMock.getClient(isA(String.class))).thenReturn(githubClientMock);
    extractionCheckNotificationSenderGithub =
        new ExtractionCheckNotificationSenderGithub(
            "{baseMessage}",
            "This is a hard failure message",
            "This is a checks skipped message",
            "testOwner",
            "testRepo",
            100);
    extractionCheckNotificationSenderGithub.githubClients = githubClientsMock;
  }

  @Test
  public void testSendFailureNotifications() {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, false);
    verify(githubClientMock, times(1))
        .addCommentToPR(
            repoNameCaptor.capture(), prNumberCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(repoNameCaptor.getValue().equals("testRepo"));
    Assert.assertTrue(prNumberCaptor.getValue().equals(100));
    Assert.assertTrue(messageCaptor.getValue().contains(GithubIcon.WARNING.toString()));
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
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, false);
    verify(githubClientMock, times(1))
        .addCommentToPR(
            repoNameCaptor.capture(), prNumberCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(repoNameCaptor.getValue().equals("testRepo"));
    Assert.assertTrue(prNumberCaptor.getValue().equals(100));
    Assert.assertTrue(messageCaptor.getValue().contains(GithubIcon.WARNING.toString()));
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
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, true);
    verify(githubClientMock, times(1))
        .addCommentToPR(
            repoNameCaptor.capture(), prNumberCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(repoNameCaptor.getValue().equals("testRepo"));
    Assert.assertTrue(prNumberCaptor.getValue().equals(100));
    Assert.assertTrue(
        messageCaptor
            .getValue()
            .contains(GithubIcon.WARNING + " **i18n source string checks failed**"));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
    Assert.assertTrue(messageCaptor.getValue().contains("This is a hard failure message"));
    Assert.assertTrue(messageCaptor.getValue().contains(GithubIcon.STOP.toString()));
  }

  @Test
  public void testSendChecksSkippedNotification() {
    extractionCheckNotificationSenderGithub.sendChecksSkippedNotification();
    verify(githubClientMock, times(1))
        .addCommentToPR(
            repoNameCaptor.capture(), prNumberCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(repoNameCaptor.getValue().equals("testRepo"));
    Assert.assertTrue(prNumberCaptor.getValue().equals(100));
    Assert.assertTrue(
        messageCaptor.getValue().equals(GithubIcon.WARNING + " This is a checks skipped message"));
  }

  @Test
  public void testNoNotificationsSentIfNoFailuresInResultList() {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(true, true, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, true);
    verify(githubClientMock, times(0))
        .addCommentToPR(isA(String.class), isA(Integer.class), isA(String.class));
  }

  @Test
  public void testNoNotificationsSentIfNullListSent() {
    extractionCheckNotificationSenderGithub.sendFailureNotification(null, true);
    verify(githubClientMock, times(0))
        .addCommentToPR(isA(String.class), isA(Integer.class), isA(String.class));
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfNoOwnerSpecified() {
    new ExtractionCheckNotificationSenderGithub("", "some template", "", "", "testRepo", 100);
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfNoRepositorySpecified() {
    new ExtractionCheckNotificationSenderGithub("", "some template", "", "testOwner", "", 100);
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfNoPRNumberProvided() {
    new ExtractionCheckNotificationSenderGithub(
        "", "some template", "", "testOwner", "testRepo", null);
  }

  @Test
  public void testQuoteMarkersAreReplaced() {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, true, "Test Check");
    result.setNotificationText(
        "Some notification text for " + QUOTE_MARKER + "some.text.id" + QUOTE_MARKER);
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, true);
    verify(githubClientMock, times(1))
        .addCommentToPR(
            repoNameCaptor.capture(), prNumberCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(repoNameCaptor.getValue().equals("testRepo"));
    Assert.assertTrue(prNumberCaptor.getValue().equals(100));
    Assert.assertTrue(
        messageCaptor
            .getValue()
            .contains(GithubIcon.WARNING + " **i18n source string checks failed**"));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
    Assert.assertTrue(messageCaptor.getValue().contains("This is a hard failure message"));
    Assert.assertTrue(messageCaptor.getValue().contains("`some.text.id`"));
  }
}
