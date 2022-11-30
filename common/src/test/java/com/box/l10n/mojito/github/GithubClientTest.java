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
import java.util.List;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHAppInstallationToken;
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

  @Mock GHCommitPointer ghCommitPointerMock;

  @Mock GHUser ghUserMock;

  @Before
  public void setup() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    Assume.assumeNotNull(githubClient);
    githubClient.gitHubClient = gitHubMock;
    githubClient.githubAppInstallationToken = ghAppInstallationTokenMock;
    when(gitHubMock.isCredentialValid()).thenReturn(true);
    when(gitHubMock.getRepository(isA(String.class))).thenReturn(ghRepoMock);
    when(ghRepoMock.getPullRequest(isA(Integer.class))).thenReturn(ghPullRequestMock);
    when(ghPullRequestMock.getBase()).thenReturn(ghCommitPointerMock);
    when(ghCommitPointerMock.getSha()).thenReturn("mockSha");
    when(ghPullRequestMock.getUser()).thenReturn(ghUserMock);
    when(ghUserMock.getEmail()).thenReturn("some@email.com");
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

    String owner = "testOwner";

    String key = "someKey";

    String appId = "testAppId";

    public String getOwner() {
      return owner;
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }

    @Bean
    public GithubClient getGithubClient() throws NoSuchAlgorithmException, InvalidKeySpecException {
      GithubClient ghClient = Mockito.spy(new GithubClient(appId, key, owner, 60000L));
      PrivateKey privateKeyMock = Mockito.mock(PrivateKey.class);
      doReturn(privateKeyMock).when(ghClient).getSigningKey();
      return Mockito.spy(new GithubClient(appId, key, owner, 60000L));
    }
  }
}
