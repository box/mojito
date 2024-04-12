package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Simple storage for images.
 *
 * @author jaurambault
 */
@Entity
@Table(
    name = "image",
    indexes = {@Index(name = "UK__IMAGE__NAME", columnList = "name", unique = true)})
public class Image extends AuditableEntity {

  @Column(name = "name")
  private String name;

  @Column(name = "content", length = Integer.MAX_VALUE)
  @Lob
  private byte[] content;

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
}
