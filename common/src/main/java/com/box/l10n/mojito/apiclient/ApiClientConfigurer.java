package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.resttemplate.ResttemplateConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApiClientConfigurer {

  @Autowired private ApiClient apiClient;

  @Autowired private ResttemplateConfig restTemplateConfig;

  @PostConstruct
  public void init() {
    this.apiClient.setBasePath(
        String.format(
            "%s://%s:%d",
            this.restTemplateConfig.getScheme(),
            this.restTemplateConfig.getHost(),
            this.restTemplateConfig.getPort()));
  }
}
