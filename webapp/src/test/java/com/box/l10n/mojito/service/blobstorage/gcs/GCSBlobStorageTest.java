package com.box.l10n.mojito.service.blobstorage.gcs;

import com.box.l10n.mojito.gcs.GCSConfiguration;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.BlobStorageTestShared;
import com.google.cloud.storage.Storage;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      GCSBlobStorageTest.class,
      GCSConfiguration.class,
      GCSBlobStorageConfigurationProperties.class,
      GCSBlobStorageTest.TestConfig.class,
    })
@EnableConfigurationProperties
@TestPropertySource(properties = {"l10n.gcs.enabled=true", "l10n.blob-storage.type=gcs"})
public class GCSBlobStorageTest implements BlobStorageTestShared {

  @Autowired(required = false)
  GCSBlobStorage gcsBlobStorage;

  @Override
  public BlobStorage getBlobStorage() {
    return gcsBlobStorage;
  }

  @Before
  @Override
  public void bbefore() {
    BlobStorageTestShared.super.bbefore();
    // Skip tests when GCS ADC or bucket is not configured
    try {
      getBlobStorage().exists("probe-" + UUID.randomUUID());
    } catch (Exception e) {
      Assume.assumeNoException("GCS credentials/ADC not available", e);
    }
  }

  @Test
  @Override
  public void testNoMatchString() {
    BlobStorageTestShared.super.testNoMatchString();
  }

  @Test
  @Override
  public void testNoMatchBytes() {
    BlobStorageTestShared.super.testNoMatchBytes();
  }

  @Test
  @Override
  public void testMatchString() {
    BlobStorageTestShared.super.testMatchString();
  }

  @Test
  @Override
  public void testMatchBytes() {
    BlobStorageTestShared.super.testMatchBytes();
  }

  @Test
  @Override
  public void testMatchMin1DayRetentionString() {
    BlobStorageTestShared.super.testMatchMin1DayRetentionString();
  }

  @Test
  @Override
  public void testMatchMin1DayRetentionBytes() {
    BlobStorageTestShared.super.testMatchMin1DayRetentionBytes();
  }

  @Test
  @Override
  public void testUpdatesWithPut() {
    BlobStorageTestShared.super.testUpdatesWithPut();
  }

  @Test
  @Override
  public void testExsits() {
    BlobStorageTestShared.super.testExsits();
  }

  @Test
  @Override
  public void testDelete() {
    BlobStorageTestShared.super.testDelete();
  }

  @Configuration
  static class TestConfig {

    @Bean
    public GCSBlobStorage gcsBlobStorage(
        Storage gcsStorage, GCSBlobStorageConfigurationProperties configurationProperties) {
      return new GCSBlobStorage(gcsStorage, configurationProperties);
    }
  }
}
