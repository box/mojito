package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import org.hibernate.annotations.BatchSize;

/**
 * Entity that contains the cache entry details for a database-backed application cache. Each entity
 * is identified by a specific {@Link ApplicationCacheType} and MD5 hash of the key. A TTL / expiry
 * date can also be configured on the entries, the application being responsible for the entry
 * eviction.
 *
 * @author garion
 */
@Entity
@Table(
    name = "application_cache",
    indexes = {
      @Index(
          name = "UK__APPLICATION_CACHE__CACHE_TYPE_ID__KEY_MD5",
          columnList = "cache_type_id, key_md5",
          unique = true),
      @Index(name = "I__APPLICATION_CACHE__EXPIRY_DATE", columnList = "expiry_date")
    })
@BatchSize(size = 1000)
public class ApplicationCache extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "cache_type_id",
      foreignKey = @ForeignKey(name = "FK__APPLICATION_CACHE__CACHE_TYPE_ID"))
  private ApplicationCacheType applicationCacheType;

  @Column(name = "key_md5", length = 32)
  private String keyMD5;

  @Column(name = "value", length = Integer.MAX_VALUE)
  @Lob
  private byte[] value;

  @Column(name = "created_date")
  private ZonedDateTime createdDate;

  @Column(name = "expiry_date")
  private ZonedDateTime expiryDate;

  public ApplicationCache() {}

  public ApplicationCache(
      ApplicationCacheType applicationCacheType,
      String keyMD5,
      byte[] value,
      ZonedDateTime expiryDate) {
    this.applicationCacheType = applicationCacheType;
    this.keyMD5 = keyMD5;
    this.value = value;
    this.expiryDate = expiryDate;
  }

  public byte[] getValue() {
    return value;
  }

  public void setValue(byte[] value) {
    this.value = value;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ZonedDateTime getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(ZonedDateTime expireDate) {
    this.expiryDate = expireDate;
  }
}
