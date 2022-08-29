package com.box.l10n.mojito.rest.entity;

/**
 * Entity that contains the Locales supported by the system globally. A locale is uniquely
 * identified by its BCP47 tag.
 *
 * @author wyau
 */
public class Locale {

  protected Long id;
  protected String bcp47Tag;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getBcp47Tag() {
    return bcp47Tag;
  }

  public void setBcp47Tag(String bcp47Tag) {
    this.bcp47Tag = bcp47Tag;
  }
}
