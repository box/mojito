package com.box.l10n.mojito.service.blobstorage.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.box.l10n.mojito.aws.s3.AmazonS3Configuration;
import com.box.l10n.mojito.aws.s3.AmazonS3ConfigurationProperties;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.BlobStorageTestShared;
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
@SpringBootTest(classes = {
        S3BlobStorageTest.class,
        S3BlobStorageConfigurationProperties.class,
        AmazonS3Configuration.class,
        AmazonS3ConfigurationProperties.class,
        S3BlobStorageTest.TestConfig.class,
})
@EnableConfigurationProperties
public class S3BlobStorageTest implements BlobStorageTestShared {

    @Autowired(required = false)
    S3BlobStorage s3BlobStorage;

    @Override
    public BlobStorage getBlobStorage() {
        return s3BlobStorage;
    }

    // Junit 4 doesn't seem to support test in interface, might be fixed in Junit 5 - revisit with spring migration
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
        AmazonS3 amazonS3;

        @Autowired
        S3BlobStorageConfigurationProperties s3BlobStorageConfigurationProperties;

        @Bean
        @ConditionalOnBean(AmazonS3.class)
        public S3BlobStorage s3BlobStorage() {
            return new S3BlobStorage(amazonS3, s3BlobStorageConfigurationProperties);
        }
    }
}
