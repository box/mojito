package com.box.l10n.mojito.github;

import java.util.Date;

public class GithubJWT {

  private final String token;

  private final Date expiryTime;

  public GithubJWT(String token, Date expiryTime) {
    this.token = token;
    this.expiryTime = expiryTime;
  }

  public String getToken() {
    return token;
  }

  public Date getExpiryTime() {
    return expiryTime;
  }
}
