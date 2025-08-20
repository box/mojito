package com.box.l10n.mojito.rest.security;

import com.box.l10n.mojito.security.Role;
import java.util.List;

/** API representation of the authenticated user (and can be reused for user profile views). */
public class UserProfile {

  String username;
  String givenName;
  String surname;
  String commonName;
  Role role;
  boolean canTranslateAllLocales;
  List<String> userLocales;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public boolean getCanTranslateAllLocales() {
    return canTranslateAllLocales;
  }

  public void setCanTranslateAllLocales(boolean canTranslateAllLocales) {
    this.canTranslateAllLocales = canTranslateAllLocales;
  }

  public List<String> getUserLocales() {
    return userLocales;
  }

  public void setUserLocales(List<String> userLocales) {
    this.userLocales = userLocales;
  }
}
