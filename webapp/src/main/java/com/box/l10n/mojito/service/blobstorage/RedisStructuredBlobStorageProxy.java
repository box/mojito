package com.box.l10n.mojito.service.blobstorage;

import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.service.blobstorage.redis.RedisClient;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisStructuredBlobStorageProxy {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RedisStructuredBlobStorageProxy.class);

  private final StructuredBlobStorage structuredBlobStorage;

  private final Optional<RedisClient> redisClientOptional;

  private final ExecutorService executorService;

  public RedisStructuredBlobStorageProxy(
      StructuredBlobStorage structuredBlobStorage,
      @Autowired(required = false) RedisClient redisClient,
      @Value("${l10n.redis.redundancy-thread-pool.num-threads:4}") int numberOfThreads) {
    this.structuredBlobStorage = structuredBlobStorage;
    this.redisClientOptional = ofNullable(redisClient);
    this.executorService = Executors.newFixedThreadPool(numberOfThreads);
  }

  public Optional<String> getString(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.structuredBlobStorage.getFullName(prefix, name);
    Optional<String> redisValue =
        this.redisClientOptional.flatMap(
            redisClient -> {
              LOGGER.debug(
                  "RedisStructuredBlobStorageProxy: Retrieve string from Redis for key: {}", key);
              return redisClient.get(key);
            });
    if (redisValue.isPresent()) {
      return redisValue;
    } else {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Retrieve string from BlobStorage for key: {}", key);
      Optional<String> result = this.structuredBlobStorage.getString(prefix, name);
      if (result.isPresent() && this.redisClientOptional.isPresent()) {
        RedisClient redisClient = this.redisClientOptional.get();
        String value = result.get();
        CompletableFuture.runAsync(
            () ->
                redisClient.put(key, value, this.structuredBlobStorage.getRetention(prefix, name)),
            this.executorService);
      }
      return result;
    }
  }

  public void put(
      StructuredBlobStorage.Prefix prefix, String name, String value, Retention retention) {
    String key = this.structuredBlobStorage.getFullName(prefix, name);
    if (this.redisClientOptional.isPresent()) {
      LOGGER.debug("RedisStructuredBlobStorageProxy: Store string in Redis for key: {}", key);
      RedisClient redisClient = this.redisClientOptional.get();
      redisClient.put(key, value, retention);
      CompletableFuture.runAsync(
          () -> this.structuredBlobStorage.put(prefix, name, value, retention),
          this.executorService);
    } else {
      LOGGER.debug("RedisStructuredBlobStorageProxy: Store string in BlobStorage for key: {}", key);
      this.structuredBlobStorage.put(prefix, name, value, retention);
    }
  }

  public void delete(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.structuredBlobStorage.getFullName(prefix, name);
    if (this.redisClientOptional.isPresent()) {
      LOGGER.debug("RedisStructuredBlobStorageProxy: Deleting string from Redis for key: {}", key);
      RedisClient redisClient = this.redisClientOptional.get();
      redisClient.delete(key);
      CompletableFuture.runAsync(
          () -> this.structuredBlobStorage.delete(prefix, name), this.executorService);
    } else {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Deleting string from BlobStorage for key: {}", key);
      this.structuredBlobStorage.delete(prefix, name);
    }
  }

  public Optional<byte[]> getBytes(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.structuredBlobStorage.getFullName(prefix, name);
    Optional<byte[]> redisValue =
        this.redisClientOptional.flatMap(
            redisClient -> {
              LOGGER.debug(
                  "RedisStructuredBlobStorageProxy: Retrieve binary object from Redis for key: {}",
                  key);
              return redisClient.getBytes(key);
            });
    if (redisValue.isPresent()) {
      return redisValue;
    } else {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Retrieve binary object from BlobStorage for key: {}",
          key);
      Optional<byte[]> result = this.structuredBlobStorage.getBytes(prefix, name);
      if (result.isPresent() && this.redisClientOptional.isPresent()) {
        RedisClient redisClient = this.redisClientOptional.get();
        byte[] value = result.get();
        CompletableFuture.runAsync(
            () ->
                redisClient.put(key, value, this.structuredBlobStorage.getRetention(prefix, name)),
            this.executorService);
      }
      return result;
    }
  }

  public void putBytes(
      StructuredBlobStorage.Prefix prefix, String name, byte[] value, Retention retention) {
    String key = this.structuredBlobStorage.getFullName(prefix, name);
    if (this.redisClientOptional.isPresent()) {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Store binary object in Redis for key: {}", key);
      RedisClient redisClient = this.redisClientOptional.get();
      redisClient.put(key, value, retention);
      CompletableFuture.runAsync(
          () -> this.structuredBlobStorage.putBytes(prefix, name, value, retention),
          this.executorService);
    } else {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Store binary object in BlobStorage for key: {}", key);
      this.structuredBlobStorage.putBytes(prefix, name, value, retention);
    }
  }
}
