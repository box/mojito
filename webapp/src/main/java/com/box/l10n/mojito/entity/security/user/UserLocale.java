package com.box.l10n.mojito.entity.security.user;

import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(
    name = "user_locale",
    indexes = {
      @Index(
          name = "UK__USER_LOCALE__USER_ID__LOCALE_ID",
          columnList = "user_id, locale_id",
          unique = true)
    })
@BatchSize(size = 1000)
public class UserLocale extends BaseEntity implements Serializable {

  @ManyToOne
  @JsonBackReference
  @JoinColumn(
      name = "user_id",
      foreignKey = @ForeignKey(name = "FK__USER_LOCALE__USER__ID"),
      nullable = false)
  User user;

  @JsonView(View.LocaleSummary.class)
  @ManyToOne
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__USER_LOCALE__LOCALE__ID"),
      nullable = false)
  Locale locale;

  public UserLocale() {}

  public UserLocale(User user, Locale locale) {
    this.user = user;
    this.locale = locale;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }
}
