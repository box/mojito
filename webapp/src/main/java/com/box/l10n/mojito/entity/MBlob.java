package com.box.l10n.mojito.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Storage for blobs with optional expiration.
 * 
 * @author jaurambault
 */
@Entity
@Table(name = "mblob",
        indexes = {
            @Index(name = "UK__MBLOB__NAME", columnList = "name", unique = true)
        }
)
public class MBlob extends SettableAuditableEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "content", length = Integer.MAX_VALUE)
    @Lob
    private byte[] content;

    @Column(name = "expire_after_seconds")
    private Long expireAfterSeconds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public long getExpireAfterSeconds() {
        return expireAfterSeconds;
    }

    public void setExpireAfterSeconds(long expireAfterSeconds) {
        this.expireAfterSeconds = expireAfterSeconds;
    }
}
