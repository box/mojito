package com.box.l10n.mojito.service.thirdparty.smartling;

import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingResultProcessorTest.fileList;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingResultProcessorTest.readFilesFromZipBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.box.l10n.mojito.aws.s3.AmazonS3Configuration;
import com.box.l10n.mojito.aws.s3.AmazonS3ConfigurationProperties;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorageConfigurationProperties;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      S3BlobStorageConfigurationProperties.class,
      AmazonS3Configuration.class,
      AmazonS3ConfigurationProperties.class,
      SmartlingResultProcessorITest.TestConfig.class,
    })
@EnableConfigurationProperties
public class SmartlingResultProcessorITest {

  SmartlingResultProcessor processor;

  @Autowired(required = false)
  S3BlobStorage s3BlobStorage;

  @Before
  public void setUp() {
    processor = new SmartlingResultProcessor();
    processor.s3BlobStorage = this.s3BlobStorage;
  }

  @Test
  public void testProcessPush() throws IOException {

    Assume.assumeNotNull(s3BlobStorage);

    String requestId = UUID.randomUUID().toString();
    List<SmartlingFile> files = fileList(4);
    SmartlingOptions smartlingOptions =
        SmartlingOptions.parseList(ImmutableList.of("dry-run=true", "request-id=" + requestId));
    String s3Url = processor.processPush(files, smartlingOptions);

    AmazonS3URI uri = new AmazonS3URI(s3Url);
    List<String> parts = Arrays.asList(uri.getKey().split("/"));
    String name = parts.get(parts.size() - 1);

    List<SmartlingFile> zippedFiles = readFilesFromZipBytes(s3BlobStorage.getBytes(name).get());

    assertThat(name).contains("_push_");
    assertThat(name).contains(requestId);
    assertThat(zippedFiles)
        .extracting("fileName", "fileContent")
        .containsExactlyInAnyOrder(
            tuple(files.get(0).getFileName(), files.get(0).getFileContent()),
            tuple(files.get(1).getFileName(), files.get(1).getFileContent()),
            tuple(files.get(2).getFileName(), files.get(2).getFileContent()),
            tuple(files.get(3).getFileName(), files.get(3).getFileContent()));
  }

  @Test
  public void testProcessPushTranslations() throws IOException {

    Assume.assumeNotNull(s3BlobStorage);

    String requestId = UUID.randomUUID().toString();
    List<SmartlingFile> files = fileList(4);
    SmartlingOptions smartlingOptions =
        SmartlingOptions.parseList(ImmutableList.of("dry-run=true", "request-id=" + requestId));
    String s3Url = processor.processPushTranslations(files, smartlingOptions);

    AmazonS3URI uri = new AmazonS3URI(s3Url);
    List<String> parts = Arrays.asList(uri.getKey().split("/"));
    String name = parts.get(parts.size() - 1);

    List<SmartlingFile> zippedFiles = readFilesFromZipBytes(s3BlobStorage.getBytes(name).get());

    assertThat(name).contains("_push_translations_");
    assertThat(name).contains(requestId);
    assertThat(zippedFiles)
        .extracting("fileName", "fileContent")
        .containsExactlyInAnyOrder(
            tuple(files.get(0).getFileName(), files.get(0).getFileContent()),
            tuple(files.get(1).getFileName(), files.get(1).getFileContent()),
            tuple(files.get(2).getFileName(), files.get(2).getFileContent()),
            tuple(files.get(3).getFileName(), files.get(3).getFileContent()));
  }

  @Configuration
  static class TestConfig {

    @Autowired(required = false)
    AmazonS3 amazonS3;

    @Autowired S3BlobStorageConfigurationProperties s3BlobStorageConfigurationProperties;

    @Bean
    @ConditionalOnBean(AmazonS3.class)
    public S3BlobStorage s3BlobStorage() {
      return new S3BlobStorage(amazonS3, s3BlobStorageConfigurationProperties);
    }
  }
}
