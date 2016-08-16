package com.box.l10n.mojito.boxsdk;

import com.box.l10n.mojito.service.boxsdk.BoxSDKServiceConfigEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class BoxSDKServiceConfigProvider {

    @Autowired
    BoxSDKServiceConfigFromProperties boxSDKServiceConfigFromProperties;

    @Autowired
    BoxSDKServiceConfigEntityService boxSDKServiceConfigEntityService;

    /**
     * @return The config to be used
     * @throws BoxSDKServiceException
     */
    public BoxSDKServiceConfig getConfig() throws BoxSDKServiceException {
        BoxSDKServiceConfig result;

        if (boxSDKServiceConfigFromProperties.isUseConfigsFromProperties()) {
            result = boxSDKServiceConfigFromProperties;
        } else {
            result = boxSDKServiceConfigEntityService.getBoxSDKServiceConfigEntity();

            if (result == null) {
                throw new BoxSDKServiceException("Config must be set before the Box Integration can be used.");
            }
        }

        return result;
    }
}
