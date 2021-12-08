package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.ThirdPartySyncAction;
import com.box.l10n.mojito.rest.entity.PollableTask;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Client to upload to trigger tasks related to third party synchronization.
 *
 * @author jaurambault
 */
@Component
public class ThirdPartyClient extends BaseClient {

    @Override
    public String getEntityName() {
        return "thirdparty";
    }

    public PollableTask sync(Long repositoryId,
                             String projectId,
                             String pluralSeparator,
                             String localeMapping,
                             List<ThirdPartySyncAction> actions,
                             String skipTextUnitsWithPattern,
                             String skipAssetsWithPathPattern,
                             String includeTextUnitsWithPattern,
                             List<String> options) {

        ThirdPartySync thirdPartySync = new ThirdPartySync();

        thirdPartySync.setRepositoryId(repositoryId);
        thirdPartySync.setProjectId(projectId);
        thirdPartySync.setActions(actions);
        thirdPartySync.setPluralSeparator(pluralSeparator);
        thirdPartySync.setLocaleMapping(localeMapping);
        thirdPartySync.setSkipTextUnitsWithPattern(skipTextUnitsWithPattern);
        thirdPartySync.setSkipAssetsWithPathPattern(skipAssetsWithPathPattern);
        thirdPartySync.setIncludeTextUnitsWithPattern(includeTextUnitsWithPattern);
        thirdPartySync.setOptions(options);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(getBasePathForEntity()).pathSegment("sync");
        return authenticatedRestTemplate.postForObject(uriBuilder.toUriString(), thirdPartySync, PollableTask.class);
    }

}
