package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.iterators.PageFetcherOffsetAndLimitSplitIterator;
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
import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SmartlingClient {

    public enum RetrievalType {
        PENDING("pending"),
        PUBLISHED("published"),
        PSEUDO("pseudo"),
        CONTEXT_MATCHING_INSTRUMENTED("contextmatchinginstrumented");

        private String value;

        RetrievalType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

    static final String API_SOURCE_STRINGS = "strings-api/v2/projects/{projectId}/source-strings?fileUri={fileUri}&offset={offset}&offset={limit}";
    static final String API_FILES_LIST = "files-api/v2/projects/{projectId}/files/list";
    static final String API_FILES_UPLOAD = "files-api/v2/projects/{projectId}/file";
    static final String API_FILES_DOWNLOAD = "files-api/v2/projects/{projectId}/locales/{locale_id}/file?fileUri={fileUri}&includeOriginalStrings={includeOriginalStrings}&retrievalType={retrievalType}";
    static final String API_FILES_DELETE = "files-api/v2/projects/{projectId}/file/delete";
    static final String API_CONTEXTS = "context-api/v2/projects/{projectId}/contexts";
    static final String API_BINDINGS = "context-api/v2/projects/{projectId}/bindings";

    static final String API_SUCCESS_CODE = "SUCCESS";

    static final int LIMIT = 500;

    OAuth2RestTemplate oAuth2RestTemplate;

    public SmartlingClient(OAuth2RestTemplate oAuth2RestTemplate) {
        this.oAuth2RestTemplate = oAuth2RestTemplate;
    }

    public Stream<StringInfo> getStringInfos(String projectId, String fileUri) {
        PageFetcherOffsetAndLimitSplitIterator<StringInfo> iterator = new PageFetcherOffsetAndLimitSplitIterator<StringInfo>(
                (offset, limit) -> {
                    Items<StringInfo> stringInfoItems = getSourceStrings(
                            projectId,
                            fileUri,
                            offset,
                            limit);

                    return stringInfoItems.getItems();
                }, LIMIT);

        return StreamSupport.stream(iterator, false);
    }

    public Items<File> getFiles(String projectId) {
        FilesResponse filesResponse = oAuth2RestTemplate.getForObject(API_FILES_LIST, FilesResponse.class, projectId);
        throwExceptionOnError(filesResponse, "Can't get files");
        return filesResponse.getData();
    }

    public String downloadFile(String projectId, String local, String fileUri, boolean includeOriginalStrings, RetrievalType retrievalType) {
        ResponseEntity<String> response = oAuth2RestTemplate.getForEntity(
                API_FILES_DOWNLOAD, String.class, projectId, local, fileUri, includeOriginalStrings, retrievalType.getValue());
        throwExceptionOnError(response, "Can't download file: %s ", fileUri);
        return response.getBody();
    }

    public void deleteFile(String projectId, String fileUri) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("fileUri", fileUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        Response response = oAuth2RestTemplate.postForObject(API_FILES_DELETE, requestEntity, Response.class, projectId);
        throwExceptionOnError(response, "Can't delete file: %s ", fileUri);
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

    <T> void throwExceptionOnError(Response<T> response, String msg, Object... vars) {
        if (!API_SUCCESS_CODE.equals(response.getCode())) {
            throw new SmartlingClientException(String.format(msg, vars) + "(code: " + response.getCode() + ")");
        }
    }

    <T> void throwExceptionOnError(ResponseEntity<T> response, String msg, Object... vars) {
        if (HttpStatus.OK != response.getStatusCode()) {
            throw new SmartlingClientException(String.format(msg, vars) + "(code: " + response.getStatusCode().value() + ")");
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
