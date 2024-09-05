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
import org.apache.commons.codec.binary.Base64;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
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
    byte[] encodedKey = keyToBytes(key);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encodedKey);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(spec);
  }

  /**
   * Converts a PEM-formatted private key string to a byte array.
   *
   * <p>This method handles both PKCS#1 and PKCS#8 formatted private keys. It strips the PEM headers
   * and footers, decodes the base64 content, and, if necessary, converts a PKCS#1 key to a PKCS#8
   * format.
   */
  private static byte[] keyToBytes(String formattedKey) {
    boolean pkcs1Header = formattedKey.startsWith("-----BEGIN RSA PRIVATE KEY-----");
    boolean pkcs8Header = formattedKey.startsWith("-----BEGIN PRIVATE KEY-----");

    if (pkcs1Header || pkcs8Header) {
      formattedKey =
          formattedKey
              .replaceAll("-----BEGIN.*?-----", "")
              .replaceAll("-----END.*?-----", "")
              .replaceAll("\\s+", "");
    }

    byte[] encodedKey = Base64.decodeBase64(formattedKey);

    if (pkcs1Header) {
      encodedKey = convertPkcs1ToPkcs8(encodedKey);
    }

    return encodedKey;
  }

  private static byte[] convertPkcs1ToPkcs8(byte[] pkcs1Bytes) {
    // PKCS#8 header for RSA encryption
    byte[] pkcs8Header = {
      0x30,
      (byte) 0x82, // SEQUENCE + LENGTH
      (byte) ((pkcs1Bytes.length + 22) >> 8),
      (byte) ((pkcs1Bytes.length + 22) & 0xff), // PKCS#8 LENGTH
      0x02,
      0x01,
      0x00, // INTEGER (0)
      0x30,
      0x0d, // SEQUENCE
      0x06,
      0x09, // OID
      0x2a,
      (byte) 0x86,
      0x48,
      (byte) 0x86,
      (byte) 0xf7,
      0x0d,
      0x01,
      0x01,
      0x01, // rsaEncryption OID
      0x05,
      0x00, // NULL
      0x04,
      (byte) 0x82, // OCTET STRING + LENGTH
      (byte) (pkcs1Bytes.length >> 8),
      (byte) (pkcs1Bytes.length & 0xff) // PKCS#1 LENGTH
    };

    byte[] pkcs8Bytes = new byte[pkcs8Header.length + pkcs1Bytes.length];
    System.arraycopy(pkcs8Header, 0, pkcs8Bytes, 0, pkcs8Header.length);
    System.arraycopy(pkcs1Bytes, 0, pkcs8Bytes, pkcs8Header.length, pkcs1Bytes.length);

    return pkcs8Bytes;
  }

  public void addCommentToPR(String repository, int prNumber, String comment) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      getGithubClient(repository)
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

  public GHPullRequest createPR(
      String repository,
      String title,
      String head,
      String base,
      String body,
      List<String> reviewers) {
    String repoFullPath = getRepositoryPath(repository);
    try {
      GHPullRequest pullRequest =
          getGithubClient(repository)
              .getRepository(repoFullPath)
              .createPullRequest(title, head, base, body);

      if (reviewers != null) {
        List<GHUser> reviewersGH =
            reviewers.stream()
                .map(
                    s -> {
                      try {
                        return gitHubClient.getUser(s);
                      } catch (IOException e) {
                        throw new RuntimeException(e);
                      }
                    })
                .toList();

        pullRequest.requestReviewers(reviewersGH);
      }

      return pullRequest;
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      String message =
          String.format("Error creating a PR in repository '%s': %s", repoFullPath, e.getMessage());
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
