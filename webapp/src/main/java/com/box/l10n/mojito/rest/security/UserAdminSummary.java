package com.box.l10n.mojito.rest.security;

import java.time.ZonedDateTime;
import java.util.List;

public class UserAdminSummary {

  Long id;
  String username;
  String givenName;
  String surname;
  String commonName;
  Boolean enabled;
  boolean canTranslateAllLocales;
  ZonedDateTime createdDate;
  List<UserAdminAuthority> authorities;
  List<UserAdminLocale> userLocales;

  public UserAdminSummary(
      Long id,
      String username,
      String givenName,
      String surname,
      String commonName,
      Boolean enabled,
      boolean canTranslateAllLocales,
      ZonedDateTime createdDate,
      List<UserAdminAuthority> authorities,
      List<UserAdminLocale> userLocales) {
    this.id = id;
    this.username = username;
    this.givenName = givenName;
    this.surname = surname;
    this.commonName = commonName;
    this.enabled = enabled;
    this.canTranslateAllLocales = canTranslateAllLocales;
    this.createdDate = createdDate;
    this.authorities = authorities;
    this.userLocales = userLocales;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getGivenName() {
    return givenName;
  }

  public String getSurname() {
    return surname;
  }

  public String getCommonName() {
    return commonName;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public boolean getCanTranslateAllLocales() {
    return canTranslateAllLocales;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public List<UserAdminAuthority> getAuthorities() {
    return authorities;
  }

  public List<UserAdminLocale> getUserLocales() {
    return userLocales;
  }

  public static UserAdminAuthority authority(String authority) {
    return new UserAdminAuthority(authority);
  }

  public static UserAdminLocale locale(String bcp47Tag) {
    return new UserAdminLocale(bcp47Tag);
  }

  public static class UserAdminAuthority {

    String authority;

    public UserAdminAuthority(String authority) {
      this.authority = authority;
    }

    public String getAuthority() {
      return authority;
    }
  }

  public static class UserAdminLocale {

    UserAdminLocaleInfo locale;

    public UserAdminLocale(String bcp47Tag) {
      this.locale = new UserAdminLocaleInfo(bcp47Tag);
    }

    public UserAdminLocaleInfo getLocale() {
      return locale;
    }
  }

  public static class UserAdminLocaleInfo {

    String bcp47Tag;

    public UserAdminLocaleInfo(String bcp47Tag) {
      this.bcp47Tag = bcp47Tag;
    }

    public String getBcp47Tag() {
      return bcp47Tag;
    }
  }
}
