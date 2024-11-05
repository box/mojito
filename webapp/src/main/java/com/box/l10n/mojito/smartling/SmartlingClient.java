package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.iterators.PageFetcherOffsetAndLimitSplitIterator;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.Context;
import com.box.l10n.mojito.smartling.response.ContextResponse;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.smartling.response.FileUploadResponse;
import com.box.l10n.mojito.smartling.response.FilesResponse;
import com.box.l10n.mojito.smartling.response.GetGlossaryDetailsResponse;
import com.box.l10n.mojito.smartling.response.GlossaryDetails;
import com.box.l10n.mojito.smartling.response.Items;
import com.box.l10n.mojito.smartling.response.Response;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import com.box.l10n.mojito.smartling.response.StringInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import reactor.util.retry.RetryBackoffSpec;

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

  /** logger */
  static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

  static final String BASE_URL = "https://api.smartling.com/";
  static final String API_SOURCE_STRINGS =
      "strings-api/v2/projects/{projectId}/source-strings?fileUri={fileUri}&offset={offset}&offset={limit}";
  static final String API_FILES_LIST = "files-api/v2/projects/{projectId}/files/list";
  static final String API_FILES_UPLOAD = "files-api/v2/projects/{projectId}/file";
  static final String API_FILES_UPLOAD_LOCALIZED =
      "files-api/v2/projects/{projectId}/locales/{localeId}/file/import";
  static final String API_FILES_DOWNLOAD =
      "files-api/v2/projects/{projectId}/locales/{locale_id}/file?fileUri={fileUri}&includeOriginalStrings={includeOriginalStrings}&retrievalType={retrievalType}";
  static final String API_FILES_DELETE = "files-api/v2/projects/{projectId}/file/delete";
  static final String API_CONTEXTS = "context-api/v2/projects/{projectId}/contexts";
  static final String API_CONTEXTS_DETAILS =
      "context-api/v2/projects/{projectId}/contexts/{contextId}";
  static final String API_BINDINGS = "context-api/v2/projects/{projectId}/bindings";
  static final String API_GLOSSARY_DETAILS =
      "glossary-api/v2/accounts/{accountId}/glossaries/{glossaryId}";
  static final String API_GLOSSARY_SOURCE_TBX_DOWNLOAD =
      "glossary-api/v2/accounts/{accountId}/glossaries/{glossaryId}/download?format=tbx&localeIds={locale}";
  static final String API_GLOSSARY_TRANSLATED_TBX_DOWNLOAD =
      "glossary-api/v2/accounts/{accountId}/glossaries/{glossaryId}/download?format=tbx&localeIds={locale},{sourceLocale}";
  static final String API_GLOSSARY_DOWNLOAD_TBX =
      "glossary-api/v2/accounts/{accountId}/glossaries/{glossaryId}/download?format=tbx";

  static final String ERROR_CANT_GET_FILES = "Can't get files";
  static final String ERROR_CANT_GET_SOURCE_STRINGS = "Can't get source strings";
  static final String ERROR_CANT_DOWNLOAD_FILE =
      "Can't download file: %s, projectId: %s, locale: %s";
  static final String ERROR_CANT_UPLOAD_FILE = "Can't upload file: %s";
  static final String ERROR_CANT_DELETE_FILE = "Can't delete file: %s";
  static final String ERROR_CANT_UPLOAD_CONTEXT = "Can't upload context: %s";
  static final String ERROR_CANT_DELETE_CONTEXT = "Can't delete context: %s";
  static final String ERROR_CANT_GET_CONTEXT = "Can't get context: %s";
  static final String ERROR_CANT_CREATE_BINDINGS = "Can't create bindings: %s";
  static final String ERROR_CANT_GET_GLOSSARY_DETAILS =
      "Can't retrieve glossary details accountId: %s, glossaryId: %s";
  static final String ERROR_CANT_DOWNLOAD_GLOSSARY_FILE_WITH_LOCALE =
      "Can't download glossary file accountId: %s, glossaryId: %s, locale: %s";
  static final String ERROR_CANT_DOWNLOAD_GLOSSARY_FILE =
      "Can't download glossary file accountId: %s, glossaryId: %s";
  static final String ERROR_CANT_GET_GLOSSARY_SOURCE_TERMS =
      "Can't retrieve glossary source terms accountId: %s, glossaryId: %s";
  static final String ERROR_CANT_GET_GLOSSARY_TARGET_TERMS =
      "Can't retrieve glossary target terms accountId: %s, glossaryId: %s, locale: %s";

  final ObjectMapper objectMapper;

  static final String API_SUCCESS_CODE = "SUCCESS";

  static final int LIMIT = 500;

  SmartlingOAuth2TokenService smartlingOAuth2TokenService;

  RetryBackoffSpec retryConfiguration;

  final HttpClient httpClient = HttpClient.newHttpClient();

  public SmartlingClient(
      SmartlingOAuth2TokenService smartlingOAuth2TokenService,
      RetryBackoffSpec retryConfiguration) {
    this.smartlingOAuth2TokenService = smartlingOAuth2TokenService;
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    this.retryConfiguration = retryConfiguration;
  }

  public RetryBackoffSpec getRetryConfiguration() {
    return this.retryConfiguration;
  }

  public Stream<StringInfo> getStringInfos(String projectId, String fileUri) {
    PageFetcherOffsetAndLimitSplitIterator<StringInfo> iterator =
        new PageFetcherOffsetAndLimitSplitIterator<StringInfo>(
            (offset, limit) -> {
              Items<StringInfo> stringInfoItems =
                  getSourceStrings(projectId, fileUri, offset, limit);

              return stringInfoItems.getItems();
            },
            LIMIT);

    return StreamSupport.stream(iterator, false);
  }

  public Items<File> getFiles(String projectId) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL + API_FILES_LIST.replace("{projectId}", projectId)))
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .build();

      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      FilesResponse filesResponse =
          objectMapper.readValue(httpResponse.body(), FilesResponse.class);

      throwExceptionOnError(filesResponse, ERROR_CANT_GET_FILES);

      return filesResponse.getData();
    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_GET_FILES);
    }
  }

  public String downloadFile(
      String projectId,
      String locale,
      String fileUri,
      boolean includeOriginalStrings,
      RetrievalType retrievalType) {
    try {
      String url =
          BASE_URL
              + API_FILES_DOWNLOAD
                  .replace("{projectId}", projectId)
                  .replace("{locale_id}", locale)
                  .replace("{fileUri}", fileUri)
                  .replace("{includeOriginalStrings}", Boolean.toString(includeOriginalStrings))
                  .replace("{retrievalType}", retrievalType.getValue());

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return response.body();
    } catch (InterruptedException | IOException e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_DOWNLOAD_FILE, fileUri, projectId, locale);
    }
  }

  public String downloadGlossaryFile(String accountId, String glossaryId) {
    try {
      String url =
          BASE_URL
              + API_GLOSSARY_DOWNLOAD_TBX
                  .replace("{accountId}", accountId)
                  .replace("{glossaryId}", glossaryId);

      HttpRequest request =
          HttpRequest.newBuilder()
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .uri(URI.create(url))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return response.body();
    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_DOWNLOAD_GLOSSARY_FILE, accountId, glossaryId);
    }
  }

  public String downloadSourceGlossaryFile(String accountId, String glossaryId, String locale) {
    try {
      String url =
          BASE_URL
              + API_GLOSSARY_SOURCE_TBX_DOWNLOAD
                  .replace("{accountId}", accountId)
                  .replace("{glossaryId}", glossaryId)
                  .replace("{locale}", locale);

      HttpRequest request =
          HttpRequest.newBuilder()
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .uri(URI.create(url))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return response.body();
    } catch (Exception e) {
      throw wrapIntoSmartlingException(
          e, ERROR_CANT_DOWNLOAD_GLOSSARY_FILE_WITH_LOCALE, accountId, glossaryId, locale);
    }
  }

  public String downloadGlossaryFileWithTranslations(
      String accountId, String glossaryId, String locale, String sourceLocale) {
    try {
      String url =
          BASE_URL
              + API_GLOSSARY_TRANSLATED_TBX_DOWNLOAD
                  .replace("{accountId}", accountId)
                  .replace("{glossaryId}", glossaryId)
                  .replace("{locale}", locale)
                  .replace("{sourceLocale}", sourceLocale);

      HttpRequest request =
          HttpRequest.newBuilder()
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .uri(URI.create(url))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return response.body();
    } catch (Exception e) {
      throw wrapIntoSmartlingException(
          e, ERROR_CANT_DOWNLOAD_GLOSSARY_FILE_WITH_LOCALE, accountId, glossaryId, locale);
    }
  }

  public String downloadPublishedFile(
      String projectId, String locale, String fileUri, boolean includeOriginalStrings) {
    return downloadFile(
        projectId, locale, fileUri, includeOriginalStrings, RetrievalType.PUBLISHED);
  }

  public void deleteFile(String projectId, String fileUri) {
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

      String url = BASE_URL + API_FILES_DELETE.replace("{projectId}", projectId);
      HttpPost deleteFileMethod = new HttpPost(url);

      deleteFileMethod.setHeader(
          "Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken());

      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
      multipartEntityBuilder.addTextBody("fileUri", fileUri);

      deleteFileMethod.setEntity(multipartEntityBuilder.build());

      try (CloseableHttpResponse response = httpclient.execute(deleteFileMethod)) {
        String responseBody = EntityUtils.toString(response.getEntity());

        Response resp = objectMapper.readValue(responseBody, Response.class);

        throwExceptionOnError(resp, ERROR_CANT_DELETE_FILE, fileUri);
      }
    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_DELETE_FILE, fileUri);
    }
  }

  public FileUploadResponse uploadFile(
      String projectId,
      String fileUri,
      String fileType,
      String fileContent,
      String placeholderFormat,
      String placeholderFormatCustom,
      String stringFormat) {

    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpPost uploadFileMethod =
          new HttpPost(BASE_URL + API_FILES_UPLOAD.replace("{projectId}", projectId));
      uploadFileMethod.setHeader(
          "Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken());

      NamedByteArrayResource fileContentAsResource =
          new NamedByteArrayResource(fileContent.getBytes(Charsets.UTF_8), fileUri);

      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

      multipartEntityBuilder.addTextBody("fileUri", fileUri);
      multipartEntityBuilder.addTextBody("fileType", fileType);
      if (!Strings.isNullOrEmpty(placeholderFormat)) {
        multipartEntityBuilder.addTextBody("smartling.placeholder_format", placeholderFormat);
      }
      if (!Strings.isNullOrEmpty(placeholderFormatCustom)) {
        multipartEntityBuilder.addTextBody(
            "smartling.placeholder_format_custom", placeholderFormatCustom);
      }
      if (!Strings.isNullOrEmpty(stringFormat)) {
        multipartEntityBuilder.addTextBody("smartling.string_format", stringFormat);
      }
      multipartEntityBuilder.addTextBody("smartling.instruction_comments_enabled", "on");
      multipartEntityBuilder.addBinaryBody(
          "file",
          fileContentAsResource.getByteArray(),
          ContentType.APPLICATION_OCTET_STREAM,
          fileContentAsResource.getFilename());

      uploadFileMethod.setEntity(multipartEntityBuilder.build());

      try (CloseableHttpResponse response = httpclient.execute(uploadFileMethod)) {
        String responseBody = EntityUtils.toString(response.getEntity());
        if (response.getCode() != 200 && response.getCode() != 202) {
          logger.error("Error uploading file with code '{}': {}", response.getCode(), responseBody);
        }
        FileUploadResponse fileUploadResponse =
            objectMapper.readValue(responseBody, FileUploadResponse.class);

        throwExceptionOnError(fileUploadResponse, ERROR_CANT_UPLOAD_FILE, fileUri);
        return fileUploadResponse;
      }

    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_UPLOAD_FILE, fileUri);
    }
  }

  public FileUploadResponse uploadLocalizedFile(
      String projectId,
      String fileUri,
      String fileType,
      String localeId,
      String fileContent,
      String placeholderFormat,
      String placeholderFormatCustom) {
    return uploadLocalizedFile(
        projectId,
        fileUri,
        fileType,
        localeId,
        fileContent,
        placeholderFormat,
        placeholderFormatCustom,
        "PUBLISHED");
  }

  public FileUploadResponse uploadLocalizedFile(
      String projectId,
      String fileUri,
      String fileType,
      String localeId,
      String fileContent,
      String placeholderFormat,
      String placeholderFormatCustom,
      String translationState) {

    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpPost uploadFileMethod =
          new HttpPost(
              BASE_URL
                  + API_FILES_UPLOAD_LOCALIZED
                      .replace("{projectId}", projectId)
                      .replace("{localeId}", localeId));
      uploadFileMethod.setHeader(
          "Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken());

      NamedByteArrayResource fileContentAsResource =
          new NamedByteArrayResource(fileContent.getBytes(Charsets.UTF_8), fileUri);

      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

      multipartEntityBuilder.addTextBody("fileUri", fileUri);
      multipartEntityBuilder.addTextBody("fileType", fileType);
      multipartEntityBuilder.addTextBody("translationState", translationState);
      multipartEntityBuilder.addTextBody("overwrite", "true");
      if (!Strings.isNullOrEmpty(placeholderFormat)) {
        multipartEntityBuilder.addTextBody("smartling.placeholder_format", placeholderFormat);
      }
      if (!Strings.isNullOrEmpty(placeholderFormatCustom)) {
        multipartEntityBuilder.addTextBody(
            "smartling.placeholder_format_custom", placeholderFormatCustom);
      }
      multipartEntityBuilder.addBinaryBody(
          "file",
          fileContentAsResource.getByteArray(),
          ContentType.APPLICATION_OCTET_STREAM,
          fileContentAsResource.getFilename());

      uploadFileMethod.setEntity(multipartEntityBuilder.build());

      try (CloseableHttpResponse response = httpclient.execute(uploadFileMethod)) {
        String responseBody = EntityUtils.toString(response.getEntity());
        FileUploadResponse fileUploadResponse =
            objectMapper.readValue(responseBody, FileUploadResponse.class);

        throwExceptionOnError(fileUploadResponse, ERROR_CANT_UPLOAD_FILE, fileUri);
        return fileUploadResponse;
      }

    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_UPLOAD_FILE, fileUri);
    }
  }

  public Items<StringInfo> getSourceStrings(
      String projectId, String fileUri, Integer offset, Integer limit) {
    try {
      String url =
          BASE_URL
              + API_SOURCE_STRINGS
                  .replace("{projectId}", projectId)
                  .replace("{fileUri}", fileUri)
                  .replace("{offset}", offset.toString())
                  .replace("{limit}", limit.toString());

      HttpRequest request =
          HttpRequest.newBuilder()
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .uri(URI.create(url))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      SourceStringsResponse sourceStringsResponse =
          objectMapper.readValue(response.body(), SourceStringsResponse.class);

      throwExceptionOnError(sourceStringsResponse, ERROR_CANT_GET_SOURCE_STRINGS);
      return sourceStringsResponse.getData();
    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_GET_SOURCE_STRINGS);
    }
  }

  public Context uploadContext(String projectId, String name, byte[] content) {
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpPost uploadContextMethod =
          new HttpPost(BASE_URL + API_CONTEXTS.replace("{projectId}", projectId));
      uploadContextMethod.setHeader(
          "Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken());

      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

      multipartEntityBuilder.addTextBody("name", name);
      multipartEntityBuilder.addBinaryBody(
          "content", content, ContentType.APPLICATION_OCTET_STREAM, name);

      uploadContextMethod.setEntity(multipartEntityBuilder.build());

      try (CloseableHttpResponse response = httpclient.execute(uploadContextMethod)) {
        String responseBody = EntityUtils.toString(response.getEntity());

        ContextResponse contextResponse =
            objectMapper.readValue(responseBody, ContextResponse.class);

        throwExceptionOnError(contextResponse, ERROR_CANT_UPLOAD_CONTEXT, name);
        return contextResponse.getData();
      }
    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_UPLOAD_CONTEXT, name);
    }
  }

  public void createBindings(Bindings bindings, String projectId) {
    try {
      String url = BASE_URL + API_BINDINGS.replace("{projectId}", projectId);
      String requestBody = objectMapper.writeValueAsString(bindings);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      logger.debug("create binding: {}", response.body());
    } catch (Exception e) {
      throw wrapIntoSmartlingException(
          e, ERROR_CANT_CREATE_BINDINGS, objectMapper.writeValueAsStringUnchecked(bindings));
    }
  }

  public GlossaryDetails getGlossaryDetails(String accountId, String glossaryId) {
    try {
      String url =
          BASE_URL
              + API_GLOSSARY_DETAILS
                  .replace("{accountId}", accountId)
                  .replace("{glossaryId}", glossaryId);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      GetGlossaryDetailsResponse getGlossaryDetailsResponse =
          objectMapper.readValue(response.body(), GetGlossaryDetailsResponse.class);

      throwExceptionOnError(
          getGlossaryDetailsResponse, ERROR_CANT_GET_GLOSSARY_DETAILS, accountId, glossaryId);
      return getGlossaryDetailsResponse.getData();
    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_GET_GLOSSARY_DETAILS, accountId, glossaryId);
    }
  }

  /** To throw an exception when Smartling returns a 200 but still is not successful */
  <T> void throwExceptionOnError(Response<T> response, String msg, Object... vars) {
    if (!API_SUCCESS_CODE.equals(response.getCode())) {
      String errorsAsString = objectMapper.writeValueAsStringUnchecked(response.getErrors());
      throw new SmartlingClientException(
          String.format(msg, vars)
              + "(code: "
              + response.getCode()
              + ", errors: "
              + errorsAsString
              + ")");
    }
  }

  /**
   * For error raised through HTTP error.
   *
   * <p>Note that 200 is not always success, {@see throwExceptionOnError}
   */
  SmartlingClientException wrapIntoSmartlingException(
      Exception exception, String messageSummary, Object... vars) throws SmartlingClientException {
    String msg = String.format(messageSummary, vars) + "\nMessage: " + exception.getMessage();
    return new SmartlingClientException(msg, exception);
  }

  static class NamedByteArrayResource extends ByteArrayResource {
    private final String filename;

    public NamedByteArrayResource(byte[] content, String filename) {
      super(content);
      this.filename = getNameForMultipart(filename);
    }

    /**
     * For some reason (not investigated) spring doesn't map "PNG" (uppercase) to proper type when
     * doing multipart
     *
     * <p>it uses: Content-Disposition: form-data; name="content"; filename="caseissuewithpng.PNG"
     * Content-Type: application/octet-stream
     *
     * <p>instead of: Content-Disposition: form-data; name="content";
     * filename="caseissuewithpng.png" Content-Type: image/x-png
     *
     * <p>So make the filename lower case here to have proper content-type.
     *
     * <p>This should not change the filename uploaded to Smartling - but need to keep an eye on it.
     * Might be better to look more into doing that with properly in Spring
     */
    String getNameForMultipart(String name) {
      return name.replaceAll("PNG$", "png");
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }

  public void deleteContext(String projectId, String contextId) {
    try {
      String url =
          BASE_URL
              + API_CONTEXTS_DETAILS
                  .replace("{projectId}", projectId)
                  .replace("{contextId}", contextId);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .DELETE()
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed to delete context");
      }
    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_DELETE_CONTEXT, contextId);
    }
  }

  public Context getContext(String projectId, String contextId) {
    try {
      String url =
          BASE_URL
              + API_CONTEXTS_DETAILS
                  .replace("{projectId}", projectId)
                  .replace("{contextId}", contextId);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Authorization", "Bearer " + smartlingOAuth2TokenService.getAccessToken())
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      ContextResponse contextResponse =
          objectMapper.readValue(response.body(), ContextResponse.class);

      throwExceptionOnError(contextResponse, ERROR_CANT_GET_CONTEXT, contextId);
      return contextResponse.getData();
    } catch (Exception e) {
      throw wrapIntoSmartlingException(e, ERROR_CANT_GET_CONTEXT, contextId);
    }
  }
}
