package com.box.l10n.mojito.security;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * Service for parsing the contents of a service header for identification
 */
public class ServiceKeyValueParser {

  Logger logger = LoggerFactory.getLogger(ServiceKeyValueParser.class);

  public String parseHeader(
      String header,
      String serviceInstanceDelimiter,
      String keyValueDelimiter,
      String valueDelimiter,
      String identifierKey)
      throws ServiceNotIdentifiableException {
    logger.debug("Parsing header: {}", header);
    if (header == null || header.isEmpty()) {
      throw new ServiceNotIdentifiableException("Service identifier not found");
    }

    String[] serviceIdentifiers = header.split(serviceInstanceDelimiter);
    // We assume that only the last service identifier is relevant
    String relevantService = serviceIdentifiers[serviceIdentifiers.length - 1];

    Map<String, String> keyValueMap =
        Stream.of(relevantService.split(keyValueDelimiter))
            .map(kv -> kv.split(valueDelimiter))
            .filter(pair -> pair.length == 2)
            .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));

    String serviceIdentifierValue = keyValueMap.get(identifierKey);
    logger.debug("Service identifier: {}", serviceIdentifierValue);
    if (serviceIdentifierValue == null || serviceIdentifierValue.isEmpty()) {
      throw new ServiceNotIdentifiableException("Service identifier not found");
    }

    return serviceIdentifierValue;
  }
}
