package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.FilesResponse;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

/**
 * Smartling client based on the Web API: https://github.com/Smartling/api-sdk-java
 */
@Component
public class SmartlingClient {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

    private static final String API_FILES_LIST = "files-api/v2/projects/{projectId}/files/list";
    private static final String API_PULL_SOURCE_STRINGS = "strings-api/v2/projects/{projectId}/source-strings?fileUri={fileUri}&offset={offset}";

    @Autowired
    @Qualifier("smartling")
    OAuth2RestTemplate restTemplate;

    public FilesResponse getFiles(String projectId) {
        return restTemplate.getForObject(API_FILES_LIST, FilesResponse.class, projectId);
    }

    public SourceStringsResponse getSourceStrings(String projectId, String file, Integer offset) {
        return restTemplate.getForObject(API_PULL_SOURCE_STRINGS, SourceStringsResponse.class, projectId, file, offset);
    }

}
