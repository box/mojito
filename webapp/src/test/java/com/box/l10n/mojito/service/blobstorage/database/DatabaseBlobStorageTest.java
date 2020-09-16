package com.box.l10n.mojito.service.blobstorage.database;

import com.box.l10n.mojito.entity.MBlob;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.BlobStorageTestShared;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DatabaseBlobStorageTest extends ServiceTestBase implements BlobStorageTestShared {

    @Autowired(required = false)
    DatabaseBlobStorage databaseBlobStorage;

    @Autowired
    MBlobRepository mBlobRepository;

    @Override
    public BlobStorage getBlobStorage() {
        return databaseBlobStorage;
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

    @Test
    @Override
    public void testUpdatesWithPut() {
        BlobStorageTestShared.super.testUpdatesWithPut();
    }

    @Test
    public void testCleanup() {

        DateTime now = DateTime.now();
        MBlob notExperied = new MBlob();
        notExperied.setCreatedDate(now);
        notExperied.setExpireAfterSeconds(1000);
        notExperied = mBlobRepository.save(notExperied);

        MBlob experied = new MBlob();
        experied.setCreatedDate(now.minusDays(1));
        experied.setExpireAfterSeconds(1000);
        experied = mBlobRepository.save(experied);

        databaseBlobStorage.deleteExpired();

        assertNull(mBlobRepository.findById(experied.getId()).orElse(null));
        assertNotNull(mBlobRepository.findById(notExperied.getId()).orElse(null));
    }

}