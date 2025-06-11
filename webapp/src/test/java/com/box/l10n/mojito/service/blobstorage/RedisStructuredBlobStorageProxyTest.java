package com.box.l10n.mojito.service.blobstorage;

import static com.box.l10n.mojito.service.blobstorage.Retention.MIN_1_DAY;
import static com.box.l10n.mojito.service.blobstorage.Retention.PERMANENT;
import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.TEXT_UNIT_DTOS_CACHE;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.service.blobstorage.redis.RedisClient;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

public class RedisStructuredBlobStorageProxyTest {
  static final String FILE_NAME = "file.txt";

  static final String KEY = TEXT_UNIT_DTOS_CACHE.toString().toLowerCase() + "/" + FILE_NAME;

  StructuredBlobStorage structuredBlobStorageMock;

  RedisClient redisClientMock;

  ArgumentCaptor<Runnable> runnableArgumentCaptorCaptor = ArgumentCaptor.forClass(Runnable.class);

  @BeforeEach
  public void before() {
    this.redisClientMock = mock(RedisClient.class);
    this.structuredBlobStorageMock = mock(StructuredBlobStorage.class);
    when(this.structuredBlobStorageMock.getFullName(
            any(StructuredBlobStorage.Prefix.class), anyString()))
        .thenReturn(KEY);
  }

  @Test
  public void testGetString_GetsValueFromBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "s3Value";
      when(this.structuredBlobStorageMock.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(of(value));
      RedisStructuredBlobStorageProxy redisStructuredBlobStorageProxy =
          new RedisStructuredBlobStorageProxy(this.structuredBlobStorageMock, null, 1);

      Optional<String> result =
          redisStructuredBlobStorageProxy.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.structuredBlobStorageMock, times(0))
          .getRetention(any(StructuredBlobStorage.Prefix.class), anyString());
      verify(this.redisClientMock, times(0)).put(anyString(), anyString(), any(Retention.class));
    }
  }

  @Test
  public void testGetString_GetsValueFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "redisValue";
      when(redisClientMock.get(KEY)).thenReturn(of(value));
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.structuredBlobStorageMock, times(0))
          .getString(any(StructuredBlobStorage.Prefix.class), anyString());
      verify(this.structuredBlobStorageMock, times(0))
          .getRetention(any(StructuredBlobStorage.Prefix.class), anyString());
      verify(this.redisClientMock, times(0)).put(anyString(), anyString(), any(Retention.class));
    }
  }

  @Test
  public void testGetString_GetsValueFromBlobStorageInsteadOfRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "s3Value";
      when(this.structuredBlobStorageMock.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(of(value));
      when(this.structuredBlobStorageMock.getRetention(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(MIN_1_DAY);
      when(this.redisClientMock.get(KEY)).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      verify(this.redisClientMock, times(1)).get(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock).getRetention(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME));
      verify(this.redisClientMock, times(1)).put(eq(KEY), eq(value), eq(MIN_1_DAY));
    }
  }

  @Test
  public void
      testGetString_GetsValueFromBlobStorageInsteadOfRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "s3Value";
      when(this.structuredBlobStorageMock.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(of(value));
      when(this.structuredBlobStorageMock.getRetention(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(PERMANENT);
      when(this.redisClientMock.get(KEY)).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      verify(this.redisClientMock, times(1)).get(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock).getRetention(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME));
      verify(this.redisClientMock, times(1)).put(eq(KEY), eq(value), eq(PERMANENT));
    }
  }

  @Test
  public void testGetString_GetsNoValue() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      when(this.structuredBlobStorageMock.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(empty());
      when(this.redisClientMock.get(KEY)).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isEmpty());
      verify(this.redisClientMock, times(1)).get(eq(KEY));
      verify(this.structuredBlobStorageMock, times(1))
          .getString(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.structuredBlobStorageMock, times(0))
          .getRetention(any(StructuredBlobStorage.Prefix.class), anyString());
      verify(this.redisClientMock, times(0)).put(anyString(), anyString(), any(Retention.class));
    }
  }

  @Test
  public void testPut_SavesValueToBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "s3Value";
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.structuredBlobStorageMock, null, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, Retention.MIN_1_DAY);

      verify(this.structuredBlobStorageMock)
          .put(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME), eq(value), eq(Retention.MIN_1_DAY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testPut_SavesValueToRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "redisValue";
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, MIN_1_DAY);

      verify(this.redisClientMock).put(eq(KEY), eq(value), eq(MIN_1_DAY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock, times(1))
          .put(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME), eq(value), eq(MIN_1_DAY));
    }
  }

  @Test
  public void testPut_SavesValueToRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "redisValue";
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, PERMANENT);

      verify(this.redisClientMock).put(eq(KEY), eq(value), eq(PERMANENT));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock, times(1))
          .put(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME), eq(value), eq(PERMANENT));
    }
  }

  @Test
  public void testDelete_DeletesFromBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.structuredBlobStorageMock, null, 1);

      redisStructuredBlobStorage.delete(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      verify(this.structuredBlobStorageMock).delete(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.redisClientMock, times(0)).delete(anyString());
    }
  }

  @Test
  public void testDelete_DeletesFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.delete(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      verify(this.redisClientMock).delete(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock, times(1))
          .delete(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME));
    }
  }

  @Test
  public void testGetBytes_GetsValueFromBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "s3Value".getBytes(StandardCharsets.UTF_8);
      when(this.structuredBlobStorageMock.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(of(value));
      RedisStructuredBlobStorageProxy redisStructuredBlobStorageProxy =
          new RedisStructuredBlobStorageProxy(this.structuredBlobStorageMock, null, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorageProxy.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.structuredBlobStorageMock, times(0))
          .getRetention(any(StructuredBlobStorage.Prefix.class), anyString());
      verify(this.redisClientMock, times(0))
          .put(anyString(), any(byte[].class), any(Retention.class));
    }
  }

  @Test
  public void testGetBytes_GetsValueFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "redisValue".getBytes(StandardCharsets.UTF_8);
      when(redisClientMock.getBytes(KEY)).thenReturn(of(value));
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.structuredBlobStorageMock, times(0))
          .getBytes(any(StructuredBlobStorage.Prefix.class), anyString());
      verify(this.structuredBlobStorageMock, times(0))
          .getRetention(any(StructuredBlobStorage.Prefix.class), anyString());
      verify(this.redisClientMock, times(0))
          .put(anyString(), any(byte[].class), any(Retention.class));
    }
  }

  @Test
  public void testGetBytes_GetsValueFromBlobStorageInsteadOfRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "s3Value".getBytes(StandardCharsets.UTF_8);
      when(this.structuredBlobStorageMock.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(of(value));
      when(this.structuredBlobStorageMock.getRetention(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(MIN_1_DAY);
      when(this.redisClientMock.getBytes(KEY)).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      verify(this.redisClientMock, times(1)).getBytes(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock).getRetention(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME));
      verify(this.redisClientMock, times(1)).put(eq(KEY), eq(value), eq(MIN_1_DAY));
    }
  }

  @Test
  public void
      testGetBytes_GetsValueFromBlobStorageInsteadOfRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "s3Value".getBytes(StandardCharsets.UTF_8);
      when(this.structuredBlobStorageMock.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(of(value));
      when(this.structuredBlobStorageMock.getRetention(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(PERMANENT);
      when(this.redisClientMock.getBytes(KEY)).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      verify(this.redisClientMock, times(1)).getBytes(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock).getRetention(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME));
      verify(this.redisClientMock, times(1)).put(eq(KEY), eq(value), eq(PERMANENT));
    }
  }

  @Test
  public void testGetBytes_GetsNoValue() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      when(this.structuredBlobStorageMock.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME))
          .thenReturn(empty());
      when(this.redisClientMock.getBytes(KEY)).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isEmpty());
      verify(this.redisClientMock, times(1)).getBytes(eq(KEY));
      verify(this.structuredBlobStorageMock, times(1))
          .getBytes(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.structuredBlobStorageMock, times(0))
          .getRetention(any(StructuredBlobStorage.Prefix.class), anyString());
      verify(this.redisClientMock, times(0))
          .put(anyString(), any(byte[].class), any(Retention.class));
    }
  }

  @Test
  public void testPutBytes_SavesValueToBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "s3Value".getBytes(StandardCharsets.UTF_8);
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.structuredBlobStorageMock, null, 1);

      redisStructuredBlobStorage.putBytes(
          TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, Retention.MIN_1_DAY);

      verify(this.structuredBlobStorageMock)
          .putBytes(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME), eq(value), eq(Retention.MIN_1_DAY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testPutBytes_SavesValueToRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "redisValue".getBytes(StandardCharsets.UTF_8);
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.putBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, MIN_1_DAY);

      verify(this.redisClientMock).put(eq(KEY), eq(value), eq(MIN_1_DAY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock, times(1))
          .putBytes(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME), eq(value), eq(MIN_1_DAY));
    }
  }

  @Test
  public void testPutBytes_SavesValueToRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "redisValue".getBytes(StandardCharsets.UTF_8);
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(
              this.structuredBlobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.putBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, PERMANENT);

      verify(this.redisClientMock).put(eq(KEY), eq(value), eq(PERMANENT));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.structuredBlobStorageMock, times(1))
          .putBytes(eq(TEXT_UNIT_DTOS_CACHE), eq(FILE_NAME), eq(value), eq(PERMANENT));
    }
  }
}
