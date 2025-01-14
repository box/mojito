package com.box.l10n.mojito.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceIdentifierParserConfig {
  @Value("${l10n.spring.security.header.parser.serviceInstanceDelimiter:,}")
  protected String serviceInstanceDelimiter;

  @Value("${l10n.spring.security.header.parser.keyValueDelimiter:;}")
  protected String keyValueDelimiter;

  @Value("${l10n.spring.security.header.parser.valueDelimiter:=}")
  protected String valueDelimiter;

  @Value("${l10n.spring.security.header.parser.identifierKey:URI}")
  protected String identifierKey;

  @Value("${l10n.spring.security.header.serviceParser.enabled:false}")
  protected boolean isEnabled;

  public boolean isEnabled() {
    return isEnabled;
  }

  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  public String getKeyValueDelimiter() {
    return keyValueDelimiter;
  }

  public void setKeyValueDelimiter(String keyValueDelimiter) {
    this.keyValueDelimiter = keyValueDelimiter;
  }

  public String getValueDelimiter() {
    return valueDelimiter;
  }

  public void setValueDelimiter(String valueDelimiter) {
    this.valueDelimiter = valueDelimiter;
  }

  public String getIdentifierKey() {
    return identifierKey;
  }

  public void setIdentifierKey(String identifierKey) {
    this.identifierKey = identifierKey;
  }

  public String getServiceInstanceDelimiter() {
    return serviceInstanceDelimiter;
  }

  public void setServiceInstanceDelimiter(String serviceInstanceDelimiter) {
    this.serviceInstanceDelimiter = serviceInstanceDelimiter;
  }
}
