package com.box.l10n.mojito.db;

import com.box.l10n.mojito.aws.s3.AmazonS3Configuration;
import com.box.l10n.mojito.aws.s3.AmazonS3ConfigurationProperties;
import com.box.l10n.mojito.service.DBUtils;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorageConfigurationProperties;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorageTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {DBUtils.class})
public class MySQLIntegrationTest {
    @Autowired
    DBUtils dbUtils;

    @Test
    public void testIntegrationTestsRunOnMysql() {
        Assert.assertTrue(dbUtils.isMysql());
    }
}
