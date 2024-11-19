package com.box.l10n.mojito.service.branch;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.branch-sources")
public class BranchSourceConfig {

  String url;
  String notFound = "-";
  Map<String, BranchSource> repoOverride = new HashMap<>();

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getNotFound() {
    return notFound;
  }

  public void setNotFound(String notFound) {
    this.notFound = notFound;
  }

  public Map<String, BranchSource> getRepoOverride() {
    return repoOverride;
  }

  public void setRepoOverride(Map<String, BranchSource> repoOverride) {
    this.repoOverride = repoOverride;
  }
}
