package com.box.l10n.mojito.service.blobstorage.redis;

import static com.box.l10n.mojito.service.blobstorage.Retention.MIN_1_DAY;
import static com.box.l10n.mojito.service.blobstorage.Retention.PERMANENT;
import static com.box.l10n.mojito.service.blobstorage.redis.RedisClient.ONE_DAY_IN_SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class RedisClientTest {
  static final String KEY = "key";

  static final String VALUE = "value";

  static final byte[] KEY_BYTES = KEY.getBytes(StandardCharsets.UTF_8);

  static final byte[] VALUE_BYTES = VALUE.getBytes(StandardCharsets.UTF_8);

  Jedis jedisMock;

  RedisPoolManager redisPoolManagerMock;

  RedisClient redisClient;

  @BeforeEach
  public void setUp() {
    this.redisPoolManagerMock = mock(RedisPoolManager.class);
    this.jedisMock = mock(Jedis.class);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    this.redisClient = new RedisClient(redisPoolManagerMock);
  }

  @Test
  public void testGet_ReturnsNonEmptyValue() {
    when(this.jedisMock.get(anyString())).thenReturn(VALUE);

    Optional<String> value = this.redisClient.get(KEY);

    assertTrue(value.isPresent());
    assertEquals(VALUE, value.get());
  }

  @Test
  public void testGet_ReturnsEmptyValue() {
    when(this.jedisMock.get(anyString())).thenReturn(null);

    Optional<String> value = this.redisClient.get(KEY);

    assertTrue(value.isEmpty());
  }

  @Test
  public void testGet_ReturnsEmptyValueWhenThrowingException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    Optional<String> value = this.redisClient.get(KEY);

    assertTrue(value.isEmpty());

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.get(anyString())).thenThrow(new JedisException("jedis client exception"));

    value = this.redisClient.get(KEY);

    assertTrue(value.isEmpty());
  }

  @Test
  public void testPut_DoesNotThrowException() {
    this.redisClient.put(KEY, VALUE, MIN_1_DAY);

    verify(this.jedisMock).setex(KEY, ONE_DAY_IN_SECONDS, VALUE);

    this.redisClient.put(KEY, VALUE, PERMANENT);

    verify(this.jedisMock).set(KEY, VALUE);
  }

  @Test
  public void testPut_ThrowsException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    this.redisClient.put(KEY, VALUE, MIN_1_DAY);

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.setex(anyString(), anyLong(), anyString()))
        .thenThrow(new JedisException("jedis client exception"));

    this.redisClient.put(KEY, VALUE, MIN_1_DAY);

    reset(this.jedisMock);
    when(this.jedisMock.set(anyString(), anyString()))
        .thenThrow(new JedisException("jedis client exception"));

    this.redisClient.put(KEY, VALUE, PERMANENT);
  }

  @Test
  public void testDelete_DoesNotThrowException() {
    this.redisClient.delete(KEY);

    verify(this.jedisMock).del(KEY);
  }

  @Test
  public void testDelete_ThrowsException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    this.redisClient.delete(KEY);

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.del(anyString())).thenThrow(new JedisException("jedis client exception"));

    this.redisClient.delete(KEY);
  }

  @Test
  public void testGetBytes_ReturnsNonEmptyValue() {
    when(this.jedisMock.get(any(byte[].class)))
        .thenReturn("value".getBytes(StandardCharsets.UTF_8));

    Optional<byte[]> value = this.redisClient.getBytes(KEY);

    assertTrue(value.isPresent());
    assertArrayEquals(VALUE_BYTES, value.get());
  }

  @Test
  public void testGetBytes_ReturnsEmptyValue() {
    when(this.jedisMock.get(any(byte[].class))).thenReturn(null);

    Optional<String> value = this.redisClient.get(KEY);

    assertTrue(value.isEmpty());
  }

  @Test
  public void testGetBytes_ReturnsEmptyValueWhenThrowingException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    Optional<byte[]> value = this.redisClient.getBytes(KEY);

    assertTrue(value.isEmpty());

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.get(any(byte[].class)))
        .thenThrow(new JedisException("jedis client exception"));

    value = this.redisClient.getBytes(KEY);

    assertTrue(value.isEmpty());
  }

  @Test
  public void testPutBytes_DoesNotThrowException() {
    this.redisClient.put(KEY, VALUE_BYTES, MIN_1_DAY);

    verify(this.jedisMock).setex(KEY_BYTES, ONE_DAY_IN_SECONDS, VALUE_BYTES);

    this.redisClient.put(KEY, VALUE_BYTES, PERMANENT);

    verify(this.jedisMock).set(KEY_BYTES, VALUE_BYTES);
  }

  @Test
  public void testPutBytes_ThrowsException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    this.redisClient.put(KEY, VALUE_BYTES, MIN_1_DAY);

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.setex(any(byte[].class), anyLong(), any(byte[].class)))
        .thenThrow(new JedisException("jedis client exception"));

    this.redisClient.put(KEY, VALUE_BYTES, MIN_1_DAY);

    reset(this.jedisMock);
    when(this.jedisMock.set(any(byte[].class), any(byte[].class)))
        .thenThrow(new JedisException("jedis client exception"));

    this.redisClient.put(KEY, VALUE_BYTES, PERMANENT);
  }
}
