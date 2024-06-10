package com.box.l10n.mojito.rest.ai.openai;

import com.box.l10n.mojito.openai.OpenAIClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "l10n.ai.openai")
public class OpenAIConfig {

  String apiKey;

  String host;

  Map<String, String> customHeaders = new HashMap<>();

  List<String> allowRestrictedHeaders;

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public void setCustomHeaders(Map<String, String> customHeaders) {
    this.customHeaders = customHeaders;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setAllowRestrictedHeaders(List<String> allowRestrictedHeaders) {
    this.allowRestrictedHeaders = allowRestrictedHeaders;
  }

  @Bean
  @ConditionalOnProperty(value = "l10n.ai.service.type", havingValue = "OpenAI")
  public OpenAIClient openAIClient() {
    if (allowRestrictedHeaders != null && !allowRestrictedHeaders.isEmpty()) {
      /**
       * Allow modification of a restricted header prior to sending the request.
       *
       * <p>Provided list here is a list of header names which are set as unrestricted in the JRE.
       */
      System.setProperty(
          "jdk.httpclient.allowRestrictedHeaders",
          allowRestrictedHeaders.stream().reduce((a, b) -> a + "," + b).get());
    }
    return OpenAIClient.builder().apiKey(apiKey).host(host).customHeaders(customHeaders).build();
  }
}
