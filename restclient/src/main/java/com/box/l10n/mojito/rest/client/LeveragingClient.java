package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.CopyTmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jaurambault
 */
@Component
public class LeveragingClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(LeveragingClient.class);

    @Override
    public String getEntityName() {
        return "leveraging";
    }

    /**
     * Copy the TM of a repository into another repository.
     *
     * @param copyTmConfig
     *
     * @return {@link CopyTmConfig}
     */
    public CopyTmConfig copyTM(CopyTmConfig copyTmConfig) {

        String exportPath = UriComponentsBuilder
                .fromPath(getBasePathForEntity())
                .pathSegment("copyTM")
                .toUriString();

        return authenticatedRestTemplate.postForObject(
                exportPath,
                copyTmConfig,
                CopyTmConfig.class);
    }

}
