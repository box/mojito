package com.box.l10n.mojito.aws.elasticache;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;

@Component
public class IAMAuthTokenRequest {
  private final IAMAuthTokenConfigurationProperties iamAuthTokenConfigurationProperties;

  public IAMAuthTokenRequest(
      IAMAuthTokenConfigurationProperties iamAuthTokenConfigurationProperties) {
    this.iamAuthTokenConfigurationProperties = iamAuthTokenConfigurationProperties;
  }

  private URI getRequestUri(String replicationGroupId) {
    return URI.create(
        String.format(
            "%s%s/",
            this.iamAuthTokenConfigurationProperties.getRequestProtocol(), replicationGroupId));
  }

  private SdkHttpFullRequest getSignableRequest(String userId, String replicationGroupId) {
    return SdkHttpFullRequest.builder()
        .method(this.iamAuthTokenConfigurationProperties.getRequestMethod())
        .uri(getRequestUri(replicationGroupId))
        .appendRawQueryParameter(
            this.iamAuthTokenConfigurationProperties.getActionParameter(),
            this.iamAuthTokenConfigurationProperties.getActionName())
        .appendRawQueryParameter(
            this.iamAuthTokenConfigurationProperties.getUserParameter(), userId)
        .build();
  }

  private SdkHttpFullRequest sign(
      String region, SdkHttpFullRequest request, AwsCredentials credentials) {
    Instant expiryInstant =
        Instant.now()
            .plus(
                Duration.ofSeconds(
                    this.iamAuthTokenConfigurationProperties.getTokenExpiryDurationSeconds()));
    Aws4Signer signer = Aws4Signer.create();
    Aws4PresignerParams signerParams =
        Aws4PresignerParams.builder()
            .signingRegion(Region.of(region))
            .awsCredentials(credentials)
            .signingName(this.iamAuthTokenConfigurationProperties.getServiceName())
            .expirationTime(expiryInstant)
            .build();
    return signer.presign(request, signerParams);
  }

  public String toSignedRequestUri(
      String userId, String replicationGroupId, String region, AwsCredentials credentials) {
    SdkHttpFullRequest request = getSignableRequest(userId, replicationGroupId);
    request = sign(region, request, credentials);
    return request
        .getUri()
        .toString()
        .replace(this.iamAuthTokenConfigurationProperties.getRequestProtocol(), "");
  }
}
