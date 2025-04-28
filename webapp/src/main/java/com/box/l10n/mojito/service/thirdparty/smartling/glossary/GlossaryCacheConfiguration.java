package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.glossary.cache")
public class GlossaryCacheConfiguration {

  List<String> repositories = new ArrayList<>();

  Boolean enabled = false;

  public List<String> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<String> repositories) {
    this.repositories = repositories;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
