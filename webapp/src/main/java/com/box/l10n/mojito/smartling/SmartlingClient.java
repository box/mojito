package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.FilesResponse;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import com.box.l10n.mojito.smartling.response.StringToContextBindingResponse;
import com.box.l10n.mojito.smartling.response.UploadContextResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

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
    private static final String API_PUSH_NEW_CONTEXT = "context-api/v2/projects/{projectId}/contexts";
    private static final String API_BIND_STRING_TO_CONTEXT = "context-api/v2/projects/{projectId}/bindings";

    @Autowired
    @Qualifier("smartling")
    OAuth2RestTemplate restTemplate;

    public FilesResponse getFiles(String projectId) {
        return restTemplate.getForObject(API_FILES_LIST, FilesResponse.class, projectId);
    }

    public SourceStringsResponse getSourceStrings(String projectId, String file, Integer offset) {
        return restTemplate.getForObject(API_PULL_SOURCE_STRINGS, SourceStringsResponse.class, projectId, file, offset);
    }

    public UploadContextResponse uploadContext(String projectId, byte[] content, String name, MediaType mediaType) {
        String contentDisposition = "form-data; name=\"content\"; filename=\"filename\"";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
        ByteArrayResource imageFile = new ByteArrayResource(content);
        HttpEntity<Resource> imageEntity = new HttpEntity<>(imageFile, headers);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("content", imageEntity);
        // will overwrite filename in content-disposition
        body.add("name", name);

        return restTemplate.postForObject(API_PUSH_NEW_CONTEXT, body, UploadContextResponse.class, projectId);
    }

    public StringToContextBindingResponse createStringToContextBindings(List<Map<String, String>> contextBindings, String projectId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        MultiValueMap<String, Map<String, String>> map = new LinkedMultiValueMap<>();
        map.put("bindings", contextBindings);
        HttpEntity<MultiValueMap<String, Map<String, String>>> request = new HttpEntity<>(map, headers);
        return restTemplate.postForObject(API_BIND_STRING_TO_CONTEXT, request, StringToContextBindingResponse.class, projectId);
    }

}
