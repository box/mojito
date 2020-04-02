package com.box.l10n.mojito.service.blobstorage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StructuredBlobStorageTest {

    StructuredBlobStorage structuredBlobStorage = new StructuredBlobStorage();

    @Test
    public void getPollableTask() {
        assertEquals("pollable_task/test1", structuredBlobStorage.getFullName(StructuredBlobStorage.Prefix.POLLABLE_TASK, "test1"));
    }

    @Test
    public void getImage() {
        assertEquals("image/test1.jpg", structuredBlobStorage.getFullName(StructuredBlobStorage.Prefix.IMAGE, "test1.jpg"));
    }
}