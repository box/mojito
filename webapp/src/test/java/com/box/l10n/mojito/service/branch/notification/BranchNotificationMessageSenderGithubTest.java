package com.box.l10n.mojito.service.branch.notification;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.service.branch.notification.github.BranchNotificationMessageBuilderGithub;
import com.box.l10n.mojito.service.branch.notification.github.BranchNotificationMessageSenderGithub;
import com.box.l10n.mojito.service.branch.notification.github.GithubBranchDetails;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class BranchNotificationMessageSenderGithubTest {

  GithubClient githubClientMock = Mockito.mock(GithubClient.class);

  BranchNotificationMessageBuilderGithub branchNotificationMessageBuilderGithubMock =
      Mockito.mock(BranchNotificationMessageBuilderGithub.class);

  BranchNotificationMessageSenderGithub branchNotificationMessageSenderGithub;

  List<String> sourceStrings;

  String branchName = "testOwner/testRepo/pulls/1";

  GithubBranchDetails githubBranchDetails = new GithubBranchDetails(branchName);

  String commentRegex;

  @Before
  public void setup() {
    final String newStringMsg = "Test new message";
    final String updatedStringMsg = "Test updated message";
    sourceStrings = new ArrayList<>();
    sourceStrings.add("Test string");
    when(branchNotificationMessageBuilderGithubMock.getNewMessage(branchName, sourceStrings))
        .thenReturn(newStringMsg);
    when(branchNotificationMessageBuilderGithubMock.getUpdatedMessage(branchName, sourceStrings))
        .thenReturn(updatedStringMsg);
    when(branchNotificationMessageBuilderGithubMock.getTranslatedMessage(
            branchName, githubBranchDetails))
        .thenReturn("Test translated message");
    when(branchNotificationMessageBuilderGithubMock.getScreenshotMissingMessage())
        .thenReturn("Test screenshot missing message");
    when(branchNotificationMessageBuilderGithubMock.getScreenshotMissingMessage())
        .thenReturn("Test screenshot missing message");
    when(branchNotificationMessageBuilderGithubMock.getNewStringMsg()).thenReturn(newStringMsg);
    when(branchNotificationMessageBuilderGithubMock.getUpdatedStringMsg())
        .thenReturn(updatedStringMsg);
    when(githubClientMock.isLabelAppliedToPR(
            isA(String.class), isA(Integer.class), isA(String.class)))
        .thenReturn(true);
    branchNotificationMessageSenderGithub =
        new BranchNotificationMessageSenderGithub(
            "testId", githubClientMock, branchNotificationMessageBuilderGithubMock);
    commentRegex =
        String.format("(%s|%s).*", Pattern.quote(newStringMsg), Pattern.quote(updatedStringMsg));
  }

  @Test
  public void testSendNewMessage() throws BranchNotificationMessageSenderException {
    when(githubClientMock.isLabelAppliedToPR(
            isA(String.class), isA(Integer.class), isA(String.class)))
        .thenReturn(false);
    branchNotificationMessageSenderGithub.sendNewMessage(branchName, "testUser", sourceStrings);
    verify(githubClientMock, times(1)).addCommentToPR("testRepo", 1, "Test new message");
    verify(githubClientMock, times(0))
        .removeLabelFromPR(isA(String.class), isA(Integer.class), isA(String.class));
    verify(githubClientMock, times(1)).addLabelToPR("testRepo", 1, "translations-required");
  }

  @Test
  public void testSendUpdatedMessage() throws BranchNotificationMessageSenderException {
    when(githubClientMock.isLabelAppliedToPR("testRepo", 1, "translations-required"))
        .thenReturn(false);
    branchNotificationMessageSenderGithub.sendUpdatedMessage(
        branchName, "testUser", "1", sourceStrings);
    verify(githubClientMock, times(1))
        .updateOrAddCommentToPR("testRepo", 1, "Test updated message", this.commentRegex);
    verify(githubClientMock, times(1)).removeLabelFromPR("testRepo", 1, "translations-ready");
    verify(githubClientMock, times(1)).addLabelToPR("testRepo", 1, "translations-required");
  }

  @Test
  public void testTranslatedMessage() throws BranchNotificationMessageSenderException {
    when(githubClientMock.isLabelAppliedToPR("testRepo", 1, "translations-ready"))
        .thenReturn(false);
    branchNotificationMessageSenderGithub.sendTranslatedMessage(branchName, "testUser", "1");
    verify(githubClientMock, times(1)).addCommentToPR("testRepo", 1, "Test translated message");
    verify(githubClientMock, times(1)).removeLabelFromPR("testRepo", 1, "translations-required");
    verify(githubClientMock, times(1)).addLabelToPR("testRepo", 1, "translations-ready");
  }

  @Test
  public void testScreenshotMissingMessage() throws BranchNotificationMessageSenderException {
    branchNotificationMessageSenderGithub.sendScreenshotMissingMessage(branchName, "1", "testUser");
    verify(githubClientMock, times(1))
        .addCommentToPR("testRepo", 1, "Test screenshot missing message");
    verify(githubClientMock, times(0))
        .removeLabelFromPR(isA(String.class), isA(Integer.class), isA(String.class));
    verify(githubClientMock, times(0))
        .addLabelToPR(isA(String.class), isA(Integer.class), isA(String.class));
  }

  @Test(expected = BranchNotificationMessageSenderException.class)
  public void testInvalidBranchNameFormat() throws BranchNotificationMessageSenderException {
    branchNotificationMessageSenderGithub.sendNewMessage(
        "branchName-in-invalid-format", "testUser", sourceStrings);
  }
}
