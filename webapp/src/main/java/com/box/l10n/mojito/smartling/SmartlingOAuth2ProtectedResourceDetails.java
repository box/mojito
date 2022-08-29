package com.box.l10n.mojito.smartling;

import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;

/** @author jaurambault */
public class SmartlingOAuth2ProtectedResourceDetails extends BaseOAuth2ProtectedResourceDetails {
  String refreshUri;

  SmartlingOAuth2ProtectedResourceDetails() {
    setGrantType("smartling");
  }

  public String getRefreshUri() {
    return refreshUri;
  }

  public void setRefreshUri(String refreshUri) {
    this.refreshUri = refreshUri;
  }

  @Override
  public boolean isClientOnly() {
    return true;
  }
}
