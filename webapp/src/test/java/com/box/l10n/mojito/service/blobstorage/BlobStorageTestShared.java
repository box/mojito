package com.box.l10n.mojito.service.blobstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public interface BlobStorageTestShared {

  String CONTENT = "コンテンツ";

  BlobStorage getBlobStorage();

  @Before
  default void bbefore() { // don't call it "before", aspectj would not compile :-|
    Assume.assumeNotNull(getBlobStorage());
  }

  @Test
  default void testNoMatchString() {
    Optional<String> string = getBlobStorage().getString(UUID.randomUUID().toString());
    assertFalse(string.isPresent());
  }

  @Test
  default void testNoMatchBytes() {
    Optional<byte[]> bytes = getBlobStorage().getBytes(UUID.randomUUID().toString());
    assertFalse(bytes.isPresent());
  }

  @Test
  default void testMatchString() {
    String name = "test-match-string-" + UUID.randomUUID().toString();
    getBlobStorage().put(name, CONTENT);
    Optional<String> string = getBlobStorage().getString(name);
    assertTrue(string.isPresent());
    assertEquals(CONTENT, string.get());
  }

  @Test
  default void testMatchBytes() {
    String name = "test-match-bytes-" + UUID.randomUUID().toString();
    getBlobStorage().put(name, CONTENT.getBytes(StandardCharsets.UTF_8));
    Optional<byte[]> bytes = getBlobStorage().getBytes(name);
    assertTrue(bytes.isPresent());
    assertEquals(CONTENT, new String(bytes.get(), StandardCharsets.UTF_8));
  }

  @Test
  default void testMatchMin1DayRetentionString() {
    String name = "test-match-min_1_day-string-" + UUID.randomUUID().toString();
    getBlobStorage().put(name, CONTENT, Retention.MIN_1_DAY);
    Optional<String> string = getBlobStorage().getString(name);
    assertTrue(string.isPresent());
    assertEquals(CONTENT, string.get());
  }

  @Test
  default void testMatchMin1DayRetentionBytes() {
    String name = "test-match-min_1_day-bytes-" + UUID.randomUUID().toString();
    getBlobStorage().put(name, CONTENT.getBytes(StandardCharsets.UTF_8), Retention.MIN_1_DAY);
    Optional<byte[]> bytes = getBlobStorage().getBytes(name);
    assertTrue(bytes.isPresent());
    assertEquals(CONTENT, new String(bytes.get(), StandardCharsets.UTF_8));
  }

  @Test
  default void testUpdatesWithPut() {
    String name = "test-updates-wiht-put" + UUID.randomUUID().toString();
    getBlobStorage().put(name, CONTENT, Retention.PERMANENT);
    getBlobStorage().put(name, CONTENT, Retention.PERMANENT);
  }

  @Test
  default void testExsits() {
    String name = "test-exists" + UUID.randomUUID().toString();
    getBlobStorage().put(name, CONTENT, Retention.MIN_1_DAY);
    assertTrue(getBlobStorage().exists(name));
  }

  @Test
  default void testDelete() {
    String name = "test-delete" + UUID.randomUUID().toString();
    getBlobStorage().put(name, CONTENT, Retention.MIN_1_DAY);
    assertTrue(getBlobStorage().exists(name));
    getBlobStorage().delete(name);
    assertFalse(getBlobStorage().exists(name));
  }
}
