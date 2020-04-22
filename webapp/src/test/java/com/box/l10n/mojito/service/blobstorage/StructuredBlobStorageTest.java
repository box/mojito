package com.box.l10n.mojito.service.blobstorage;

import com.box.l10n.mojito.common.Mocks;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.IMAGE;
import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.POLLABLE_TASK;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class StructuredBlobStorageTest {

    @Mock
    BlobStorage blobStorage;

    @InjectMocks
    StructuredBlobStorage structuredBlobStorage;

    @Test
    public void getPollableTask() {
        assertEquals("pollable_task/test1", structuredBlobStorage.getFullName(POLLABLE_TASK, "test1"));
    }

    @Test
    public void getImage() {
        assertEquals("image/test1.jpg", structuredBlobStorage.getFullName(IMAGE, "test1.jpg"));
    }

    @Test
    public void testRetention() {
        structuredBlobStorage.put(POLLABLE_TASK, "testretention", "testrentention-content", Retention.MIN_1_DAY);
        Mockito.verify(blobStorage).put("pollable_task/testretention", "testrentention-content", Retention.MIN_1_DAY);
    }
}