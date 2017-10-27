package com.box.l10n.mojito.rest.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class VirtualAssetClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(VirtualAssetClient.class);

    @Override
    public String getEntityName() {
        return "virtualAssets";
    }

    public VirtualAsset createOrUpdate(VirtualAsset virtualAsset) {
        return authenticatedRestTemplate.postForObject(
                getBasePathForEntity(),
                virtualAsset,
                VirtualAsset.class
        );
    }

}
