package com.box.l10n.mojito.service.blobstorage.autoconfigure;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class BlobStorageConfigurationTest extends ServiceTestBase {

    @Autowired
    BlobStorage blobStorage;

    @Test
    public void testProperImplementationIsLoaded() {
        assertEquals(DatabaseBlobStorage.class, blobStorage.getClass());
    }

}