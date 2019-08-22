package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.PollableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client to upload to trigger tasks related to third party synchronization.
 *
 * @author jaurambault
 */
@Component
public class ThirdPartyClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ThirdPartyClient.class);

    @Override
    public String getEntityName() {
        return "thirdparty";
    }

    public PollableTask sync(Long repositoryId, String projectId) {
        ThirdPartySync thirdPartySync = new ThirdPartySync();
        thirdPartySync.setRepositoryId(repositoryId);
        thirdPartySync.setProjectId(projectId);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(getBasePathForEntity()).pathSegment("sync");
        return authenticatedRestTemplate.postForObject(uriBuilder.toUriString(), thirdPartySync, PollableTask.class);
    }

}
