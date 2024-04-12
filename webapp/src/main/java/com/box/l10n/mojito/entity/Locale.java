package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NaturalId;

/**
 * Entity that contains the Locales supported by the system globally. A locale is uniquely
 * identified by its BCP47 tag.
 *
 * @author jaurambault
 */
@Entity
@Table(
    name = "locale",
    indexes = {@Index(name = "UK__LOCALE__BCP47_TAG", columnList = "bcp47_tag", unique = true)})
// The batch size should be the size of the table, it's set to 2000 to allow
// for growth without needing further updates.
@BatchSize(size = 2000)
public class Locale extends BaseEntity {

  @Basic(optional = false)
  @Column(name = "bcp47_tag")
  @NaturalId
  @JsonView(View.LocaleSummary.class)
  private String bcp47Tag;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Locale locale = (Locale) o;

    if (bcp47Tag != null ? !bcp47Tag.equals(locale.bcp47Tag) : locale.bcp47Tag != null)
      return false;
    if (id != null ? !id.equals(locale.id) : locale.id != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (bcp47Tag != null ? bcp47Tag.hashCode() : 0);
    return result;
  }

  public String getBcp47Tag() {
    return bcp47Tag;
  }

  public void setBcp47Tag(String bcp47Tag) {
    this.bcp47Tag = bcp47Tag;
  }
}
