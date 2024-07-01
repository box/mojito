package com.box.l10n.mojito.service.ai;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the tiered cache for the AI Checks service. The second tier cache (database) is
 * optional and can be enabled/disabled. By default, the TTL is 0 (disabled).
 *
 * @author maallen
 */
@Configuration
@ConfigurationProperties(prefix = "l10n.ai.checks.cache")
public class AIChecksCacheConfiguration {

  Database database = new Database();
  InMemory inMemory = new InMemory();

  public static class Database {
    boolean enabled = false;
    private boolean evictEntryOnDeserializationFailure;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration ttl = Duration.ZERO;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getTtl() {
      return ttl;
    }

    public void setTtl(Duration ttl) {
      this.ttl = ttl;
    }

    public boolean isEvictEntryOnDeserializationFailure() {
      return evictEntryOnDeserializationFailure;
    }

    public void setEvictEntryOnDeserializationFailure(boolean evictEntryOnDeserializationFailure) {
      this.evictEntryOnDeserializationFailure = evictEntryOnDeserializationFailure;
    }
  }

  public static class InMemory {
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration ttl = Duration.ZERO;

    private long maximumSize = Long.MAX_VALUE;

    public long getMaximumSize() {
      return maximumSize;
    }

    public void setMaximumSize(long maximumSize) {
      this.maximumSize = maximumSize;
    }

    public Duration getTtl() {
      return ttl;
    }

    public void setTtl(Duration ttl) {
      this.ttl = ttl;
    }
  }

  public Database getDatabase() {
    return database;
  }

  public void setDatabase(Database database) {
    this.database = database;
  }

  public InMemory getInMemory() {
    return inMemory;
  }

  public void setInMemory(InMemory inMemory) {
    this.inMemory = inMemory;
  }
}
