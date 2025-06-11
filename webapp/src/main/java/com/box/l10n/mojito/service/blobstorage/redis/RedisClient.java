package com.box.l10n.mojito.service.blobstorage.redis;

import static com.box.l10n.mojito.service.blobstorage.Retention.PERMANENT;
import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.service.blobstorage.Retention;
import com.google.common.annotations.VisibleForTesting;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

@Component
@ConditionalOnProperty("l10n.redis.connection.endpoint")
public class RedisClient {
  private static final Logger LOG = LoggerFactory.getLogger(RedisClient.class);

  @VisibleForTesting static final int ONE_DAY_IN_SECONDS = 24 * 60 * 60;

  private final RedisPoolManager redisPoolManager;

  public RedisClient(RedisPoolManager redisPoolManager) {
    this.redisPoolManager = redisPoolManager;
  }

  public Optional<String> get(String key) {
    try (Jedis redisClient = this.redisPoolManager.getJedis()) {
      return ofNullable(redisClient.get(key));
    } catch (JedisException e) {
      LOG.error("Unable to retrieve key from Redis: {}", key, e);
      return Optional.empty();
    }
  }

  public void put(String key, String value, Retention retention) {
    try (Jedis redisClient = this.redisPoolManager.getJedis()) {
      if (retention == PERMANENT) {
        redisClient.set(key, value);
      } else {
        redisClient.setex(key, ONE_DAY_IN_SECONDS, value);
      }
    } catch (JedisException e) {
      LOG.error("Unable to save key to Redis: {}", key, e);
    }
  }

  public void delete(String key) {
    try (Jedis redisClient = this.redisPoolManager.getJedis()) {
      redisClient.del(key);
    } catch (JedisException e) {
      LOG.error("Unable to delete key from Redis: {}", key, e);
    }
  }

  public Optional<byte[]> getBytes(String key) {
    try (Jedis redisClient = this.redisPoolManager.getJedis()) {
      return ofNullable(redisClient.get(key.getBytes(StandardCharsets.UTF_8)));
    } catch (JedisException e) {
      LOG.error("Unable to retrieve binary key from Redis: {}", key, e);
      return Optional.empty();
    }
  }

  public void put(String key, byte[] value, Retention retention) {
    try (Jedis redisClient = this.redisPoolManager.getJedis()) {
      byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
      if (retention == PERMANENT) {
        redisClient.set(binaryKey, value);
      } else {
        redisClient.setex(binaryKey, ONE_DAY_IN_SECONDS, value);
      }
    } catch (JedisException e) {
      LOG.error("Unable to save key to Redis: {}", key, e);
    }
  }
}
