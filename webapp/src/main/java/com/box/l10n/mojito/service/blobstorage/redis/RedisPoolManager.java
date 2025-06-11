package com.box.l10n.mojito.service.blobstorage.redis;

import com.box.l10n.mojito.aws.elasticache.IAMAuthTokenRequest;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Component
@ConditionalOnProperty("l10n.redis.connection.endpoint")
public class RedisPoolManager {
  private static final Logger LOG = LoggerFactory.getLogger(RedisPoolManager.class);

  private JedisPool jedisPool;

  private final RedisConfigurationProperties redisConfigurationProperties;

  private final RedisPoolConfigurationProperties redisPoolConfigurationProperties;

  private final ScheduledThreadPoolConfigProperties scheduledThreadPoolConfigProperties;

  private final ScheduledExecutorService scheduler;

  private final IAMAuthTokenRequest iamAuthTokenRequest;

  public RedisPoolManager(
      RedisConfigurationProperties redisConfigurationProperties,
      RedisPoolConfigurationProperties redisPoolConfigurationProperties,
      ScheduledThreadPoolConfigProperties scheduledThreadPoolConfigProperties,
      IAMAuthTokenRequest iamAuthTokenRequest) {
    this.redisConfigurationProperties = redisConfigurationProperties;
    this.redisPoolConfigurationProperties = redisPoolConfigurationProperties;
    this.scheduledThreadPoolConfigProperties = scheduledThreadPoolConfigProperties;
    this.scheduler =
        Executors.newScheduledThreadPool(this.scheduledThreadPoolConfigProperties.getPoolSize());
    this.iamAuthTokenRequest = iamAuthTokenRequest;
  }

  @VisibleForTesting
  AwsCredentials getAwsCredentials() {
    if (this.redisConfigurationProperties.getAccessKey() != null
        && this.redisConfigurationProperties.getSecretKey() != null) {
      LOG.info("Using AWS credentials from the properties file");
      return AwsBasicCredentials.create(
          this.redisConfigurationProperties.getAccessKey(),
          this.redisConfigurationProperties.getSecretKey());
    }
    try (DefaultCredentialsProvider defaultCredentialsProvider =
        DefaultCredentialsProvider.create()) {
      LOG.info("Using default AWS credentials");
      return defaultCredentialsProvider.resolveCredentials();
    }
  }

  @VisibleForTesting
  void refreshJedisPool() {
    LOG.info("Refreshing JedisPool");
    if (this.jedisPool != null) {
      this.jedisPool.close();
    }
    String authToken =
        this.iamAuthTokenRequest.toSignedRequestUri(
            this.redisConfigurationProperties.getUserId(),
            this.redisConfigurationProperties.getReplicationGroupId(),
            this.redisConfigurationProperties.getRegion(),
            this.getAwsCredentials());
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(this.redisPoolConfigurationProperties.getMaxTotal());
    poolConfig.setMaxIdle(this.redisPoolConfigurationProperties.getMaxIdle());
    poolConfig.setMinIdle(this.redisPoolConfigurationProperties.getMinIdle());
    DefaultJedisClientConfig clientConfig =
        DefaultJedisClientConfig.builder()
            .user(this.redisConfigurationProperties.getUserId())
            .password(authToken)
            .ssl(true)
            .timeoutMillis(this.redisPoolConfigurationProperties.getTimeoutMillis())
            .build();
    this.jedisPool =
        new JedisPool(
            poolConfig,
            new HostAndPort(
                this.redisConfigurationProperties.getEndpoint(),
                this.redisConfigurationProperties.getPort()),
            clientConfig);
  }

  @PostConstruct
  public void init() {
    LOG.info("Initializing RedisPoolManager");
    this.refreshJedisPool();
    this.scheduler.scheduleAtFixedRate(
        this::refreshJedisPool,
        this.scheduledThreadPoolConfigProperties.getPeriodInMinutes(),
        this.scheduledThreadPoolConfigProperties.getPeriodInMinutes(),
        TimeUnit.MINUTES);
  }

  public Jedis getJedis() {
    LOG.info("Getting Jedis");
    return this.jedisPool.getResource();
  }

  @PreDestroy
  public void shutdown() {
    LOG.info("Shutting down RedisPoolManager");
    if (this.jedisPool != null) {
      this.jedisPool.close();
    }
    this.scheduler.shutdown();
  }

  @VisibleForTesting
  JedisPool getJedisPool() {
    return this.jedisPool;
  }
}
