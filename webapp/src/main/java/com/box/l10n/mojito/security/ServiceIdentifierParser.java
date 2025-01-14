package com.box.l10n.mojito.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceIdentifierParser {
  private final ServiceKeyValueParser keyValueParser = new ServiceKeyValueParser();

  @Autowired ServiceIdentifierParserConfig serviceIdentifierParserConfig;

  String parseHeader(String header) throws ServiceNotIdentifiableException {
    if (serviceIdentifierParserConfig.isEnabled()) {
      return keyValueParser.parseHeader(
          header,
          serviceIdentifierParserConfig.getServiceInstanceDelimiter(),
          serviceIdentifierParserConfig.getKeyValueDelimiter(),
          serviceIdentifierParserConfig.getValueDelimiter(),
          serviceIdentifierParserConfig.getIdentifierKey());
    }

    return header;
  }
}
