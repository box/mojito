package com.box.l10n.mojito.aws.elasticache;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

public class IAMAuthTokenRequestTest {
  @Test
  public void testToSignedRequestUri() {
    Pattern authTokenPattern =
        Pattern.compile(
            "replicationGroupId/\\?Action=connect&User=userId&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=\\d{8}T\\d{6}Z&X-Amz-SignedHeaders=host&X-Amz-Expires=\\d+&X-Amz-Credential=accessKeyId%2F[\\d]{8}%2Fus-east-1%2Felasticache%2Faws4_request&X-Amz-Signature=[a-f0-9]{64}");
    IAMAuthTokenRequest authTokenRequest =
        new IAMAuthTokenRequest(new IAMAuthTokenConfigurationProperties());

    String authToken =
        authTokenRequest.toSignedRequestUri(
            "userId",
            "replicationGroupId",
            "us-east-1",
            AwsBasicCredentials.create("accessKeyId", "secretAccessKey"));

    Assertions.assertTrue(authTokenPattern.matcher(authToken).matches());
  }
}
