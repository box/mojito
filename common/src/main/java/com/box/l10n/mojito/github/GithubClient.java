package com.box.l10n.mojito.github;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GithubClient {

  /**
   * Maximum allowed Github JWT is 10 minutes
   *
   * @see <a
   *     https://docs.github.com/en/developers/apps/building-github-apps/authenticating-with-github-apps#authenticating-as-a-github-app<a/>
   */
  private static final long MAX_GITHUB_JWT_TTL = 10 * 60000;

  private static Logger logger = LoggerFactory.getLogger(GithubClient.class);

  private final String appId;

  private final String owner;

  private final long tokenTTL;

  private final String key;

  private GithubJWT githubJWT;

  private PrivateKey signingKey;

  private final String endpoint;

  protected GHAppInstallationToken githubAppInstallationToken;

  protected GitHub gitHubClient;

  public GithubClient(String appId, String key, String owner, long tokenTTL, String endpoint) {
    this.appId = appId;
    this.key = key;
    if (owner == null || owner.isEmpty()) {
      throw new GithubException(
          "Github integration requires that the 'owner' property is configured for each client.");
    }
    this.owner = owner;
    this.tokenTTL = tokenTTL;
    this.endpoint =
        endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
  }

  public GithubClient(String appId, String key, String owner) {
    this(appId, key, owner, 60000L, "https://api.github.com");
  }

  private PrivateKey createPrivateKey(String key)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] encodedKey = Base64.decodeBase64(key);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encodedKey);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(spec);
  }

  public GHIssueComment addCommentToPR(String repository, int prNumber, String comment) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      return getGithubClient(repository)
          .getRepository(getRepositoryPath(repository))
          .getPullRequest(prNumber)
          .comment(comment);
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      logger.error(
          String.format(
              "Error adding comment to PR %d in repository '%s': %s",
              prNumber, repoFullPath, e.getMessage()),
          e);
    }

    return null;
  }

  public GHIssueComment updateOrAddCommentToPR(
      String repository, int prNumber, String comment, String commentRegex) {
    Pattern commentPattern = Pattern.compile(commentRegex, Pattern.DOTALL);
    try {
      Optional<GHIssueComment> githubComment =
          this.getGithubClient(repository)
              .getRepository(this.getRepositoryPath(repository))
              .getPullRequest(prNumber)
              .getComments()
              .stream()
              .filter(actualComment -> commentPattern.matcher(actualComment.getBody()).matches())
              .findFirst();
      if (githubComment.isPresent()) {
        githubComment.get().update(comment);
        return githubComment.get();
      } else {
        return this.addCommentToPR(repository, prNumber, comment);
      }
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      logger.error(
          String.format(
              "Error updating/adding a comment to PR %d in repository '%s': %s",
              prNumber, this.getRepositoryPath(repository), e.getMessage()),
          e);
    }
    return null;
  }

  public void addStatusToCommit(
      String repository,
      String commitSha,
      GHCommitState statusState,
      String statusDescription,
      String statusContext,
      String targetUrl) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      getGithubClient(repository)
          .getRepository(repoFullPath)
          .createCommitStatus(commitSha, statusState, targetUrl, statusDescription, statusContext);
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format(
              "Error adding status to commit %s in repository '%s': %s",
              commitSha, repoFullPath, e.getMessage());
      logger.error(message);
      throw new GithubException(message, e);
    }
  }

  public void addCommentToCommit(String repository, String commitSha1, String comment) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      getGithubClient(repository)
          .getRepository(getRepositoryPath(repository))
          .getCommit(commitSha1)
          .createComment(comment);
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format(
              "Error adding comment to commit %s in repository '%s': %s",
              commitSha1, repoFullPath, e.getMessage());
      logger.error(message, e);
      throw new GithubException(message, e);
    }
  }

  public String getPRBaseCommit(String repository, int prNumber) {
    String repoFullPath = getRepositoryPath(repository);

    try {
      return getGithubClient(repository)
          .getRepository(repoFullPath)
          .getPullRequest(prNumber)
          .getBase()
          .getSha();
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format(
              "Error retrieving base commit for PR %d in repository '%s': %s",
              prNumber, repoFullPath, e.getMessage());
      logger.error(message, e);
      throw new GithubException(message, e);
    }
  }

  public String getPRAuthorEmail(String repository, int prNumber) {
    String repoFullPath = getRepositoryPath(repository);

    try {
      return getGithubClient(repository)
          .getRepository(repoFullPath)
          .getPullRequest(prNumber)
          .getUser()
          .getEmail();
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format(
              "Error getting author email for PR %d in repository '%s': %s",
              prNumber, repoFullPath, e.getMessage());
      logger.error(message, e);
      throw new GithubException(message, e);
    }
  }

  public void addLabelToPR(String repository, int prNumber, String labelName) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      getGithubClient(repository)
          .getRepository(repoFullPath)
          .getPullRequest(prNumber)
          .addLabels(labelName);
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format(
              "Error adding label '%s' to PR %d in repository '%s': %s",
              labelName, prNumber, repoFullPath, e.getMessage());
      logger.error(message, e);
      throw new GithubException(message, e);
    }
  }

  public void removeLabelFromPR(String repository, int prNumber, String labelName) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      getGithubClient(repository)
          .getRepository(repoFullPath)
          .getPullRequest(prNumber)
          .removeLabel(labelName);
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format(
              "Error removing label '%s' from PR %d in repository '%s': %s",
              labelName, prNumber, repoFullPath, e.getMessage());
      logger.error(message, e);
      throw new GithubException(message, e);
    }
  }

  public boolean isLabelAppliedToPR(String repository, int prNumber, String labelName) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      return getGithubClient(repository)
          .getRepository(repoFullPath)
          .getPullRequest(prNumber)
          .getLabels()
          .stream()
          .anyMatch(ghLabel -> ghLabel.getName().equals(labelName));
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format(
              "Error reading labels for PR %d in repository '%s' : '%s'",
              prNumber, repoFullPath, e.getMessage());
      logger.error(message);
      throw new GithubException(message, e);
    }
  }

  public List<GHIssueComment> getPRComments(String repository, int prNumber) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      return getGithubClient(repository)
          .getRepository(repoFullPath)
          .getPullRequest(prNumber)
          .getComments();
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format(
              "Error retrieving comments for PR %d in repository '%s': %s",
              prNumber, repoFullPath, e.getMessage());
      logger.error(message, e);
      throw new GithubException(message, e);
    }
  }

  public String getOwner() {
    return owner;
  }

  public String getAppId() {
    return appId;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public GHAppInstallationToken getGithubAppInstallationToken(String repository)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    if (githubAppInstallationToken == null
        || githubAppInstallationToken.getExpiresAt().getTime()
            <= System.currentTimeMillis() - 30000) {
      // Existing installation token has less than 30 seconds before expiry, get new token
      GitHub gitHub =
          new GitHubBuilder()
              .withEndpoint(getEndpoint())
              .withJwtToken(getGithubJWT(tokenTTL).getToken())
              .build();
      githubAppInstallationToken =
          gitHub.getApp().getInstallationByRepository(owner, repository).createToken().create();
    }

    return githubAppInstallationToken;
  }

  protected GitHub createGithubClient(String repository)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    return new GitHubBuilder()
        .withEndpoint(getEndpoint())
        .withAppInstallationToken(getGithubAppInstallationToken(repository).getToken())
        .build();
  }

  private String getRepositoryPath(String repository) {
    return owner != null && !owner.isEmpty() ? owner + "/" + repository : repository;
  }

  private GithubJWT getGithubJWT(long ttlMillis)
      throws NoSuchAlgorithmException, InvalidKeySpecException {

    Date now = new Date(System.currentTimeMillis());

    if (githubJWT != null && now.getTime() <= (githubJWT.getExpiryTime().getTime() - 30000)) {
      return githubJWT;
    } else {
      // Existing JWT has less than 30 seconds before expiry, create new token
      githubJWT = createGithubJWT(ttlMillis, now);
    }

    return githubJWT;
  }

  private GithubJWT createGithubJWT(long ttlMillis, Date now)
      throws NoSuchAlgorithmException, InvalidKeySpecException {

    JwtBuilder builder =
        Jwts.builder()
            .setIssuedAt(now)
            .setIssuer(appId)
            .signWith(getSigningKey(), SignatureAlgorithm.RS256);

    Date expiry = new Date(now.getTime() + ttlMillis);
    if (ttlMillis > MAX_GITHUB_JWT_TTL) {
      long expMillis = now.getTime() + MAX_GITHUB_JWT_TTL;
      expiry = new Date(expMillis);
    }
    builder.setExpiration(expiry);

    return new GithubJWT(builder.compact(), expiry);
  }

  protected PrivateKey getSigningKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (signingKey == null) {
      signingKey = createPrivateKey(key);
    }

    return signingKey;
  }

  private GitHub getGithubClient(String repository)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    if (gitHubClient == null || !gitHubClient.isCredentialValid()) {
      gitHubClient = createGithubClient(repository);
    }

    return gitHubClient;
  }
}
