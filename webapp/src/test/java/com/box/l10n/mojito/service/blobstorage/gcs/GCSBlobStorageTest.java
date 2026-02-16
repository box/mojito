package com.box.l10n.mojito.service.blobstorage.gcs;

import com.box.l10n.mojito.gcs.GCSConfiguration;
import com.box.l10n.mojito.gcs.GCSConfigurationProperties;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.BlobStorageTestShared;
import com.google.cloud.storage.Storage;
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
      GCSBlobStorageTest.class,
      GCSConfigurationProperties.class,
      GCSConfiguration.class,
      GCSBlobStorageConfigurationProperties.class,
      GCSBlobStorageTest.TestConfig.class,
    })
@EnableConfigurationProperties
public class GCSBlobStorageTest implements BlobStorageTestShared {

  @Autowired(required = false)
  GCSBlobStorage gcsBlobStorage;

  @Override
  public BlobStorage getBlobStorage() {
    return gcsBlobStorage;
  }

  // Junit 4 doesn't seem to support test in interface, might be fixed in Junit 5 - revisit with
  // spring migration
  @Before
  @Override
  public void bbefore() {
    BlobStorageTestShared.super.bbefore();
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

  @Configuration
  static class TestConfig {

    @Autowired(required = false)
    Storage storage;

    @Autowired GCSBlobStorageConfigurationProperties gcsBlobStorageConfigurationProperties;

    @Bean
    @ConditionalOnBean(Storage.class)
    public GCSBlobStorage gcsBlobStorage() {
      return new GCSBlobStorage(storage, gcsBlobStorageConfigurationProperties);
    }
  }
}
