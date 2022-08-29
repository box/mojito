package com.box.l10n.mojito.service.blobstorage;

import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.POLLABLE_TASK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class StructuredBlobStorageITest extends ServiceTestBase {

  @Autowired StructuredBlobStorage structuredBlobStorage;

  @Test
  public void test() {
    structuredBlobStorage.delete(POLLABLE_TASK, "generaltest");
    assertFalse(structuredBlobStorage.exists(POLLABLE_TASK, "generaltest"));
    structuredBlobStorage.put(
        POLLABLE_TASK, "generaltest", "generaltest-content", Retention.MIN_1_DAY);
    assertTrue(structuredBlobStorage.exists(POLLABLE_TASK, "generaltest"));
    String generaltest = structuredBlobStorage.getString(POLLABLE_TASK, "generaltest").get();
    assertEquals("generaltest-content", generaltest);
  }
}
