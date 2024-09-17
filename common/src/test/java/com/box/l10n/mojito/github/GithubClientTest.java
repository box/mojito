package com.box.l10n.mojito.github;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GithubClientTest.class, GithubClientTest.TestConfig.class})
@EnableConfigurationProperties
public class GithubClientTest {

  @Autowired(required = false)
  GithubClient githubClient;

  @Autowired TestConfig testConfig;

  @Mock GitHub gitHubMock;

  @Mock GHAppInstallationToken ghAppInstallationTokenMock;

  @Mock GHRepository ghRepoMock;

  @Mock GHPullRequest ghPullRequestMock;

  @Mock GHCommit ghCommitMock;

  @Mock GHCommitPointer ghCommitPointerMock;

  @Mock GHUser ghUserMock;

  @Mock GHIssueComment ghCommentMock1;

  @Mock GHIssueComment ghCommentMock2;

  @Before
  public void setup() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    Assume.assumeNotNull(githubClient);
    githubClient.gitHubClient = gitHubMock;
    githubClient.githubAppInstallationToken = ghAppInstallationTokenMock;
    when(gitHubMock.isCredentialValid()).thenReturn(true);
    when(gitHubMock.getRepository(isA(String.class))).thenReturn(ghRepoMock);
    when(ghRepoMock.getPullRequest(isA(Integer.class))).thenReturn(ghPullRequestMock);
    when(ghRepoMock.getCommit(isA(String.class))).thenReturn(ghCommitMock);
    when(ghPullRequestMock.getBase()).thenReturn(ghCommitPointerMock);
    when(ghCommitPointerMock.getSha()).thenReturn("mockSha");
    when(ghPullRequestMock.getUser()).thenReturn(ghUserMock);
    when(ghUserMock.getEmail()).thenReturn("some@email.com");
    when(this.ghCommentMock1.getBody()).thenReturn("Test comment 1");
    when(this.ghCommentMock2.getBody()).thenReturn("Test 2");
    when(this.ghPullRequestMock.getComments())
        .thenReturn(Arrays.asList(this.ghCommentMock1, this.ghCommentMock2));
  }

  @Test
  public void testGetPRBaseCommit() throws IOException {
    assertEquals("mockSha", githubClient.getPRBaseCommit("testRepo", 1));
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
  }

  @Test
  public void testAddCommentToPR() throws IOException {
    githubClient.addCommentToPR("testRepo", 1, "Test comment");
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).comment("Test comment");
  }

  @Test
  public void testUpdateOrAddCommentToPRWhenUpdatingComment() throws IOException {
    this.githubClient.updateOrAddCommentToPR("testRepo", 1, "Test comment", "[a-zA-Z]+\\s[\\d].*");
    verify(this.gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(this.ghRepoMock, times(1)).getPullRequest(1);
    verify(this.ghPullRequestMock, times(1)).getComments();
    verify(this.ghCommentMock1, times(1)).getBody();
    verify(this.ghCommentMock2, times(1)).getBody();
    verify(this.ghCommentMock1, times(0)).update("Test comment");
    verify(this.ghCommentMock2, times(1)).update("Test comment");
  }

  @Test
  public void testUpdateOrAddCommentToPRWhenAddingComment() throws IOException {
    this.githubClient.updateOrAddCommentToPR("testRepo", 1, "Test comment", "[a-z]+\\s[\\d]{2}.*");
    verify(this.gitHubMock, times(2)).getRepository("testOwner/testRepo");
    verify(this.ghRepoMock, times(2)).getPullRequest(1);
    verify(this.ghPullRequestMock, times(1)).getComments();
    verify(this.ghCommentMock1, times(1)).getBody();
    verify(this.ghCommentMock2, times(1)).getBody();
    verify(this.ghCommentMock1, times(0)).update("Test comment");
    verify(this.ghCommentMock2, times(0)).update("Test comment");
    verify(this.githubClient, times(1)).addCommentToPR("testRepo", 1, "Test comment");
  }

  @Test
  public void testAddCommentToCommit() throws IOException {
    githubClient.addCommentToCommit("testRepo", "shatest", "Test comment");
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getCommit("shatest");
    verify(ghCommitMock, times(1)).createComment("Test comment");
  }

  @Test
  public void testGetAuthorEmail() throws IOException {
    assertEquals("some@email.com", githubClient.getPRAuthorEmail("testRepo", 1));
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).getUser();
    verify(ghUserMock, times(1)).getEmail();
  }

  @Test
  public void testAddLabelToPR() throws IOException {
    githubClient.addLabelToPR("testRepo", 1, "translations-needed");
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).addLabels("translations-needed");
  }

  @Test
  public void testRemoveLabelFromPR() throws IOException {
    githubClient.removeLabelFromPR("testRepo", 1, "translations-needed");
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).removeLabel("translations-needed");
  }

  @Test
  public void testGetPRComments() throws IOException {
    List<GHIssueComment> comments = Lists.newArrayList(new GHIssueComment(), new GHIssueComment());
    when(ghPullRequestMock.getComments()).thenReturn(comments);

    assertEquals(comments, githubClient.getPRComments("testRepo", 1));
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).getComments();
  }

  @Test
  public void testClientRefreshWhenCredsInvalid()
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    assertEquals("mockSha", githubClient.getPRBaseCommit("testRepo", 1));
    when(gitHubMock.isCredentialValid()).thenReturn(false);
    doReturn(gitHubMock).when(githubClient).createGithubClient(isA(String.class));
    assertEquals("mockSha", githubClient.getPRBaseCommit("testRepo", 1));
    verify(githubClient, times(1)).createGithubClient("testRepo");
  }

  @Configuration
  @ConfigurationProperties("l10n.github")
  static class TestConfig {

    @Bean
    public GithubClient getGithubClient() throws NoSuchAlgorithmException, InvalidKeySpecException {
      GithubClient ghClient = Mockito.spy(new GithubClient("testAppId", "someKey", "testOwner"));
      PrivateKey privateKeyMock = Mockito.mock(PrivateKey.class);
      doReturn(privateKeyMock).when(ghClient).getSigningKey();
      return ghClient;
    }
  }
}
