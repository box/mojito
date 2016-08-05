package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.CopyTmConfig;
import com.box.l10n.mojito.rest.entity.ExportDropConfig;
import com.box.l10n.mojito.rest.entity.Repository;
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
     * @param sourceRepositoryId {@link Repository#id} of the source repository
     * @param targetRepositoryId {@link Repository#id} of the target repository
     * @return {@link ExportDropConfig} that contains information about the drop
     * being created
     */
    public CopyTmConfig copyTM(Long sourceRepositoryId, Long targetRepositoryId, CopyTmConfig.Mode mode) {
        
        CopyTmConfig copyTmConfig = new CopyTmConfig();
        copyTmConfig.setSourceRepositoryId(sourceRepositoryId);
        copyTmConfig.setTargetRepositoryId(targetRepositoryId);
        
        if (mode != null) {
            copyTmConfig.setMode(mode);
        }
        
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
