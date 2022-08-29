package com.box.l10n.mojito.aws.s3;

import com.amazonaws.PredefinedClientConfigurations;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("l10n.aws.s3.enabled")
public class AmazonS3Configuration {

  @Bean
  public AmazonS3 amazonS3Client(AmazonS3ConfigurationProperties amazonS3ConfigurationProperties) {
    AmazonS3ClientBuilder amazonS3ClientBuilder =
        AmazonS3ClientBuilder.standard()
            .withClientConfiguration(PredefinedClientConfigurations.defaultConfig().withGzip(true))
            .withRegion(Regions.fromName(amazonS3ConfigurationProperties.getRegion()));

    if (amazonS3ConfigurationProperties.getAccessKeyId() != null) {
      AWSCredentials credentials =
          new BasicAWSCredentials(
              amazonS3ConfigurationProperties.getAccessKeyId(),
              amazonS3ConfigurationProperties.getAccessKeySecret());
      amazonS3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(credentials));
    }

    AmazonS3 amazonS3 = amazonS3ClientBuilder.build();
    return amazonS3;
  }
}
