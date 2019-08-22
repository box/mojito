package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.ContextUpload;
import com.box.l10n.mojito.smartling.response.ContextUploadResponse;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.smartling.response.FileUploadResponse;
import com.box.l10n.mojito.smartling.response.FilesResponse;
import com.box.l10n.mojito.smartling.response.Items;
import com.box.l10n.mojito.smartling.response.Response;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import com.box.l10n.mojito.smartling.response.StringInfo;
import com.box.l10n.mojito.utils.PageFetcherSplitIterator;
import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SmartlingClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

    static final String API_SOURCE_STRINGS = "strings-api/v2/projects/{projectId}/source-strings?fileUri={fileUri}&offset={offset}&offset={limit}";
    static final String API_FILES_LIST = "files-api/v2/projects/{projectId}/files/list";
    static final String API_FILES_UPLOAD = "files-api/v2/projects/{projectId}/file";
    static final String API_CONTEXTS = "context-api/v2/projects/{projectId}/contexts";
    static final String API_BINDINGS = "context-api/v2/projects/{projectId}/bindings";

    static final String API_SUCCESS_CODE = "SUCCESS";

    static final int LIMIT = 500;

    OAuth2RestTemplate oAuth2RestTemplate;

    public SmartlingClient(OAuth2RestTemplate oAuth2RestTemplate) {
        this.oAuth2RestTemplate = oAuth2RestTemplate;
    }

    public Stream<StringInfo> getStringInfos(String projectId, String fileUri) {
        PageFetcherSplitIterator<StringInfo> stringInfoPageFetcherSplitIterator = new PageFetcherSplitIterator<StringInfo>((offset, limit) -> {
            Items<StringInfo> stringInfoItems = getSourceStrings(
                    projectId,
                    fileUri,
                    offset,
                    limit);

            return stringInfoItems.getItems();
        }, LIMIT);

        return StreamSupport.stream(stringInfoPageFetcherSplitIterator, false);
    }

    public Stream<StringInfo> getStringInfosFromFiles(String projectId, List<File> files) {
        Stream<StringInfo> stringInfoStream = files.stream().flatMap(file -> getStringInfos(projectId, file.getFileUri()));
        return stringInfoStream;
    }

    public Items<File> getFiles(String projectId) {
        FilesResponse filesResponse = oAuth2RestTemplate.getForObject(API_FILES_LIST, FilesResponse.class, projectId);
        throwExceptionOnError(filesResponse, "Can't get files");
        return filesResponse.getData();
    }

    public FileUploadResponse uploadFile(String projectId, String fileUri, String fileType, String fileContent, String placeholderFormat, String placeholderFormatCustom) {

        NamedByteArrayResource fileContentAsResource = new NamedByteArrayResource(fileContent.getBytes(Charsets.UTF_8), fileUri);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("fileUri", fileUri);
        body.add("fileType", fileType);
        body.add("smartling.placeholder_format", placeholderFormat);
        body.add("smartling.placeholder_format_custom", placeholderFormatCustom);
        body.add("smartling.instruction_comments_enabled", "on");
        body.add("file", fileContentAsResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        FileUploadResponse response = oAuth2RestTemplate.postForObject(API_FILES_UPLOAD, requestEntity, FileUploadResponse.class, projectId);
        throwExceptionOnError(response, "Can't upload file");
        return response;
    }

    public Items<StringInfo> getSourceStrings(String projectId, String fileUri, Integer offset, Integer limit) throws SmartlingClientException {

        SourceStringsResponse sourceStringsResponse = oAuth2RestTemplate.getForObject(
                API_SOURCE_STRINGS,
                SourceStringsResponse.class,
                projectId, fileUri, offset, limit);
        throwExceptionOnError(sourceStringsResponse, "Can't get source strings");
        return sourceStringsResponse.getData();
    }


    public ContextUpload uploadContext(String projectId, String name, byte[] content) {

        ByteArrayResource contentAsResource = new NamedByteArrayResource(content, name);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("content", contentAsResource);
        body.add("name", name);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ContextUploadResponse contextUploadResponse = oAuth2RestTemplate.postForObject(API_CONTEXTS, requestEntity, ContextUploadResponse.class, projectId);
        throwExceptionOnError(contextUploadResponse, "Can't upload context");

        return contextUploadResponse.getData();
    }

    public void createBindings(Bindings bindings, String projectId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Bindings> requestEntity = new HttpEntity<>(bindings, headers);
        String s = oAuth2RestTemplate.postForObject(API_BINDINGS, requestEntity, String.class, projectId);
        logger.debug("create binding: {}", s);
    }

    <T> void throwExceptionOnError(Response<T> response, String msg) {
        if (!API_SUCCESS_CODE.equals(response.getCode())) {
            throw new SmartlingClientException(msg + "(code: " + response.getCode() + ")");
        }
    }

    static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        public NamedByteArrayResource(byte[] content, String filename) {
            super(content);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
