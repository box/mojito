package com.box.l10n.mojito.boxsdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** @author aloison */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthAuthorizationResult {

  @JsonProperty(value = "access_token")
  private String accessToken;

  @JsonProperty(value = "refresh_token")
  private String refreshToken;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
