package com.box.l10n.mojito.smartling;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SmartlingOAuthAccessResponse {

  @JsonProperty("response")
  private Response response;

  public Response getResponse() {
    return response;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  static class Response {
    @JsonProperty("code")
    private String code;

    @JsonProperty("data")
    private TokenData data;

    @JsonProperty("errors")
    private ErrorDetails error;

    public String getCode() {
      return code;
    }

    public TokenData getData() {
      return data;
    }

    public ErrorDetails getError() {
      return error;
    }
  }

  static class TokenData {
    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("expiresIn")
    private int expiresIn;

    @JsonProperty("refreshExpiresIn")
    private int refreshExpiresIn;

    @JsonProperty("tokenType")
    private String tokenType;

    @JsonIgnore private long refreshExpiryTime;

    @JsonIgnore private long tokenExpiryTime;

    public String getAccessToken() {
      return accessToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public int getExpiresIn() {
      return expiresIn;
    }

    public int getRefreshExpiresIn() {
      return refreshExpiresIn;
    }

    public long getRefreshExpiryTime() {
      return refreshExpiryTime;
    }

    public void setRefreshExpiryTime(long refreshExpiryTime) {
      this.refreshExpiryTime = refreshExpiryTime;
    }

    public long getTokenExpiryTime() {
      return tokenExpiryTime;
    }

    public void setTokenExpiryTime(long tokenExpiryTime) {
      this.tokenExpiryTime = tokenExpiryTime;
    }
  }

  static class ErrorDetails {
    @JsonProperty("details")
    private Object details;

    @JsonProperty("key")
    private String key;

    @JsonProperty("message")
    private String message;
  }
}
