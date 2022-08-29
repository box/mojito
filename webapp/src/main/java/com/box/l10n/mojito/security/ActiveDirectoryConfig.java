package com.box.l10n.mojito.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Contains the configuration related to Active directory authentication.
 *
 * @author jaurambault
 */
@Component
@ConfigurationProperties(prefix = "l10n.security.ad")
public class ActiveDirectoryConfig {

  String url;
  String domain;
  String rootDn;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getRootDn() {
    return rootDn;
  }

  public void setRootDn(String rootDn) {
    this.rootDn = rootDn;
  }
}
