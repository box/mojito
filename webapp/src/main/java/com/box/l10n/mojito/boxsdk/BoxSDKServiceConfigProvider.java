package com.box.l10n.mojito.boxsdk;

import com.box.l10n.mojito.service.boxsdk.BoxSDKServiceConfigEntityRepository;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class BoxSDKServiceConfigProvider {

  private final BoxSDKServiceConfigFromProperties boxSDKServiceConfigFromProperties;

  private final BoxSDKServiceConfigEntityRepository boxSDKServiceConfigEntityRepository;

  public BoxSDKServiceConfigProvider(
      BoxSDKServiceConfigFromProperties boxSDKServiceConfigFromProperties,
      BoxSDKServiceConfigEntityRepository boxSDKServiceConfigEntityRepository) {
    this.boxSDKServiceConfigFromProperties = boxSDKServiceConfigFromProperties;
    this.boxSDKServiceConfigEntityRepository = boxSDKServiceConfigEntityRepository;
  }

  /**
   * @return The config to be used
   * @throws BoxSDKServiceException
   */
  public BoxSDKServiceConfig getConfig() throws BoxSDKServiceException {
    BoxSDKServiceConfig result;

    if (boxSDKServiceConfigFromProperties.isUseConfigsFromProperties()) {
      result = boxSDKServiceConfigFromProperties;
    } else {
      result = boxSDKServiceConfigEntityRepository.findFirstByOrderByIdAsc();

      if (result == null) {
        throw new BoxSDKServiceException(
            "Config must be set before the Box Integration can be used.");
      }
    }

    return result;
  }
}
