package com.box.l10n.mojito.service.thirdparty.phrase;

import static com.box.l10n.mojito.io.Files.createDirectories;
import static com.box.l10n.mojito.io.Files.createTempDirectory;
import static com.box.l10n.mojito.io.Files.write;

import com.box.l10n.mojito.json.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.phrase.client.ApiClient;
import com.phrase.client.ApiException;
import com.phrase.client.api.KeysApi;
import com.phrase.client.api.LocalesApi;
import com.phrase.client.api.TagsApi;
import com.phrase.client.api.UploadsApi;
import com.phrase.client.auth.ApiKeyAuth;
import com.phrase.client.model.Tag;
import com.phrase.client.model.TranslationKey;
import com.phrase.client.model.Upload;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

public class PhraseClient {

  static Logger logger = LoggerFactory.getLogger(PhraseClient.class);

  static final int BATCH_SIZE = 100;

  final ApiClient apiClient;

  final RetryBackoffSpec retryBackoffSpec;

  public PhraseClient(ApiClient apiClient) {
    this.apiClient = apiClient;
    this.retryBackoffSpec =
        Retry.backoff(5, Duration.ofMillis(500)).maxBackoff(Duration.ofSeconds(5));
  }

  public Upload nativeUploadAndWait(
      String projectId,
      String localeId,
      String fileFormat,
      String fileName,
      String fileContent,
      List<String> tags,
      Map<String, String> formatOptions) {

    String uploadId =
        nativeUploadCreateFileWithRetry(
            projectId, localeId, fileFormat, fileName, fileContent, tags, formatOptions);
    return waitForUploadToFinish(projectId, uploadId);
  }

  public Upload uploadAndWait(
      String projectId,
      String localeId,
      String fileFormat,
      String fileName,
      String fileContent,
      List<String> tags,
      String formatOptions) {

    String uploadId =
        uploadCreateFile(
            projectId, localeId, fileFormat, fileName, fileContent, tags, formatOptions);
    return waitForUploadToFinish(projectId, uploadId);
  }

  Upload waitForUploadToFinish(String projectId, String uploadId) {
    UploadsApi uploadsApi = new UploadsApi(apiClient);
    try {
      logger.debug("Waiting for upload to finish: {}", uploadId);

      Stopwatch stopwatch = Stopwatch.createStarted();

      Upload upload = uploadsApi.uploadShow(projectId, uploadId, null, null);
      logger.debug(
          "Upload info, first fetch: {}", new ObjectMapper().writeValueAsStringUnchecked(upload));

      while (!ImmutableSet.of("success", "error").contains(upload.getState())) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        upload = uploadsApi.uploadShow(projectId, uploadId, null, null);
        logger.debug(
            "upload info after polling for success or error: {}",
            new ObjectMapper().writeValueAsStringUnchecked(upload));
      }

      if ("error".equals(upload.getState())) {
        throw new PhraseClientException(
            "Upload failed: %s".formatted(new ObjectMapper().writeValueAsStringUnchecked(upload)));
      }

      logger.info("Waited: {} for upload: {} to finish", stopwatch.elapsed(), uploadId);

      return upload;
    } catch (ApiException e) {
      logger.error("Error calling Phrase for waitForUploadToFinish: {}", e.getResponseBody());
      throw new PhraseClientException(e);
    }
  }

  String uploadCreateFile(
      String projectId,
      String localeId,
      String fileFormat,
      String fileName,
      String fileContent,
      List<String> tags,
      String formatOptions) {

    Path tmpWorkingDirectory = null;

    logger.info(
        "uploadCreateFile: projectId: {}, localeId: {}, fileName: {}, tags: {}",
        projectId,
        localeId,
        fileName,
        tags);

    try {
      tmpWorkingDirectory = createTempDirectory("phrase-integration");

      if (tmpWorkingDirectory.toFile().exists()) {
        logger.debug("Created temporary working directory: {}", tmpWorkingDirectory);
      }

      Path fileToUpload = tmpWorkingDirectory.resolve(fileName);

      logger.debug("Create file: {}", fileToUpload);
      createDirectories(fileToUpload.getParent());
      write(fileToUpload, fileContent);

      Upload upload =
          uploadsApiUploadCreateWithRetry(
              projectId, localeId, fileFormat, tags, fileToUpload, formatOptions);

      return upload.getId();
    } finally {
      if (tmpWorkingDirectory != null) {
        com.box.l10n.mojito.io.Files.deleteRecursivelyIfExists(tmpWorkingDirectory);
      }
    }
  }

  public String nativeUploadCreateFileWithRetry(
      String projectId,
      String localeId,
      String fileFormat,
      String fileName,
      String fileContent,
      List<String> tags,
      Map<String, String> formatOptions) {

    logger.info(
        "nativeUploadCreateFile: projectId: {}, localeId: {}, fileName: {}, tags: {}",
        projectId,
        localeId,
        fileName,
        tags);

    return Mono.fromCallable(
            () ->
                nativeUploadCreateFile(
                    projectId, localeId, fileFormat, fileName, fileContent, tags, formatOptions))
        .retryWhen(
            retryBackoffSpec.doBeforeRetry(
                doBeforeRetry ->
                    logAttempt(
                        doBeforeRetry.failure(),
                        "Retrying failed attempt to uploadCreate to Phrase, file: %s, project id: %s"
                            .formatted(fileName, projectId))))
        .doOnError(
            throwable ->
                rethrowExceptionWithLog(
                    throwable,
                    "Final error in UploadCreate from Phrase, file: %s, project id: %s"
                        .formatted(fileName, projectId)))
        .block();
  }

  /**
   * The official SDK does not support format_options properly, so adding a replacement method base
   * on pure Java client
   */
  public String nativeUploadCreateFile(
      String projectId,
      String localeId,
      String fileFormat,
      String fileName,
      String fileContent,
      List<String> tags,
      Map<String, String> formatOptions) {

    Stopwatch stopwatch = Stopwatch.createStarted();

    String urlString = String.format("%s/projects/%s/uploads", apiClient.getBasePath(), projectId);
    String boundary = UUID.randomUUID().toString();
    final String LINE_FEED = "\r\n";

    StringBuilder multipartBody = new StringBuilder();

    multipartBody.append("--").append(boundary).append(LINE_FEED);
    multipartBody
        .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
        .append(fileName)
        .append("\"")
        .append(LINE_FEED);
    multipartBody.append("Content-Type: application/xml").append(LINE_FEED);
    multipartBody.append(LINE_FEED);
    multipartBody.append(fileContent).append(LINE_FEED);

    addFormField(multipartBody, boundary, "locale_id", localeId);
    addFormField(multipartBody, boundary, "file_format", fileFormat);
    addFormField(multipartBody, boundary, "update_translations", "true");
    addFormField(multipartBody, boundary, "update_descriptions", "true");

    if (tags != null) {
      String tagsString = String.join(",", tags);
      addFormField(multipartBody, boundary, "tags", tagsString);
    }

    if (formatOptions != null) {
      for (Map.Entry<String, String> e : formatOptions.entrySet()) {
        addFormField(
            multipartBody, boundary, "format_options[%s]".formatted(e.getKey()), e.getValue());
      }
    }
    multipartBody.append("--").append(boundary).append("--").append(LINE_FEED);

    String token = ((ApiKeyAuth) apiClient.getAuthentication("Token")).getApiKey();

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(urlString))
            .header("Authorization", "token " + token)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    multipartBody.toString(), StandardCharsets.UTF_8))
            .build();

    HttpResponse<String> response;
    try (HttpClient client = HttpClient.newHttpClient()) {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    logger.info("nativeUploadCreateFile took: {}", stopwatch.elapsed());

    int statusCode = response.statusCode();
    String responseBody = response.body();

    if (statusCode == 201) {
      JsonNode rootNode = new ObjectMapper().readTreeUnchecked(responseBody);
      return rootNode.path("id").asText();
    } else {
      throw new RuntimeException("Server returned status code " + statusCode + ": " + responseBody);
    }
  }

  /**
   * Helper method to add a form field to the multipart body.
   *
   * @param builder The StringBuilder for the multipart body.
   * @param boundary The boundary string.
   * @param name The name of the form field.
   * @param value The value of the form field.
   */
  private static void addFormField(
      StringBuilder builder, String boundary, String name, String value) {
    String LINE_FEED = "\r\n";
    builder.append("--").append(boundary).append(LINE_FEED);
    builder
        .append("Content-Disposition: form-data; name=\"")
        .append(name)
        .append("\"")
        .append(LINE_FEED);
    builder.append(LINE_FEED);
    builder.append(value).append(LINE_FEED);
  }

  Upload uploadsApiUploadCreateWithRetry(
      String projectId,
      String localeId,
      String fileFormat,
      List<String> tags,
      Path fileToUpload,
      String formatOptions) {

    return Mono.fromCallable(
            () ->
                new UploadsApi(apiClient)
                    .uploadCreate(
                        projectId,
                        fileToUpload.toFile(),
                        fileFormat,
                        localeId,
                        null,
                        null,
                        tags == null ? null : String.join(",", tags),
                        true,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        formatOptions,
                        null,
                        null,
                        null))
        .retryWhen(
            retryBackoffSpec.doBeforeRetry(
                doBeforeRetry ->
                    logAttempt(
                        doBeforeRetry.failure(),
                        "Retrying failed attempt to uploadCreate to Phrase, file: %s, project id: %s"
                            .formatted(fileToUpload.toAbsolutePath(), projectId))))
        .doOnError(
            throwable ->
                rethrowExceptionWithLog(
                    throwable,
                    "Final error in UploadCreate from Phrase, file: %s, project id: %s"
                        .formatted(fileToUpload.toAbsolutePath(), projectId)))
        .block();
  }

  /**
   * Conducted tests on keysDeleteCollection using the <code>-tags:tag1,tag2</code> option, and it
   * operates as a filter for keys "not in any of the tags".
   *
   * <p>It removes keys that are not tagged with any of the provided tags.
   *
   * <p>For example:
   *
   * <ul>
   *   <li>If key <code>ka</code> has tag <code>tag1</code>,
   *   <li>If key <code>kb</code> has tag <code>tag2</code>,
   *   <li>If key <code>kc</code> has tag <code>tag3</code>,
   * </ul>
   *
   * <p>Calling <code>keysDeleteCollection</code> with <code>-tags:tag1,tag3</code> will remove
   * <code>kb</code>.
   */
  public void removeKeysNotTaggedWith(String projectId, List<String> anyOfTheseTags) {
    logger.info("Removing keys not tagged with any of the following tags: {}", anyOfTheseTags);

    Mono.fromCallable(
            () -> {
              KeysApi keysApi = new KeysApi(apiClient);
              keysApi.keysDeleteCollection(
                  projectId,
                  null,
                  null,
                  "-tags:%s".formatted(String.join(",", anyOfTheseTags)),
                  null);
              return null;
            })
        .retryWhen(
            retryBackoffSpec.doBeforeRetry(
                doBeforeRetry ->
                    logAttempt(
                        doBeforeRetry.failure(),
                        "Retrying failed attempt to removeKeysNotTaggedWith from Phrase, project id: %s"
                            .formatted(projectId))))
        .doOnError(
            throwable ->
                rethrowExceptionWithLog(
                    throwable,
                    "Final error to removeKeysNotTaggedWith from Phrase, project id: %s"
                        .formatted(projectId)))
        .block();
  }

  /**
   * @param onTagErrorRefreshCallback with concurrent update, the tags could be updated during
   *     download. We don't want to retry the whole logic, so we provide a callback to refresh the
   *     tags
   */
  public String localeDownload(
      String projectId,
      String locale,
      String fileFormat,
      String tags,
      Supplier<String> onTagErrorRefreshCallback) {
    AtomicReference<String> refTags = new AtomicReference<>(tags);
    return Mono.fromCallable(
            () -> {
              LocalesApi localesApi = new LocalesApi(apiClient);
              logger.info(
                  "Downloading locale: {} from project id: {} in file format: {}",
                  locale,
                  projectId,
                  fileFormat);
              File file =
                  localesApi.localeDownload(
                      projectId,
                      locale,
                      null,
                      null,
                      null,
                      null,
                      fileFormat,
                      refTags.get(),
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null);

              String localeDownloadContent = Files.readString(file.toPath());
              logger.debug("File: {}, Content: {}", file.toPath(), localeDownloadContent);

              return localeDownloadContent;
            })
        .retryWhen(
            retryBackoffSpec.doBeforeRetry(
                doBeforeRetry -> {
                  logAttempt(
                      doBeforeRetry.failure(),
                      "Retrying failed attempt to localeDownload from Phrase, project id: %s, locale: %s"
                          .formatted(projectId, locale));

                  if (onTagErrorRefreshCallback != null
                      && getErrorMessageFromOptionalApiException(doBeforeRetry.failure())
                          .contains("Invalid Download Options. Parameter tags ")) {
                    String newTags = onTagErrorRefreshCallback.get();
                    logger.warn(
                        "Replacing old tags: {} with new tags: {} for download locale",
                        refTags.get(),
                        newTags);
                    refTags.set(newTags);
                  }
                }))
        .doOnError(
            throwable ->
                rethrowExceptionWithLog(
                    throwable,
                    "Final error to localeDownload from Phrase, project id: %s, locale: %s"
                        .formatted(projectId, locale)))
        .block();
  }

  public String nativeLocaleDownload(
      String projectId,
      String locale,
      String fileFormat,
      String tags,
      Map<String, String> formatOptions,
      Supplier<String> onTagErrorRefreshCallback) {
    AtomicReference<String> refTags = new AtomicReference<>(tags);
    return Mono.fromCallable(
            () -> {
              logger.info(
                  "Native Downloading locale: {} from project id: {} in file format: {}",
                  locale,
                  projectId,
                  fileFormat);

              Map<String, String> parameters = new HashMap<>();
              parameters.put("file_format", fileFormat);
              parameters.put("tags", refTags.get());
              for (Map.Entry<String, String> e : formatOptions.entrySet()) {
                parameters.put("format_options[%s]".formatted(e.getKey()), e.getValue());
              }

              StringJoiner queryJoiner = new StringJoiner("&");
              for (Map.Entry<String, String> entry : parameters.entrySet()) {
                queryJoiner.add(
                    URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
              }

              String url =
                  String.format(
                      "%s/projects/%s/locales/%s/download?%s",
                      apiClient.getBasePath(),
                      URLEncoder.encode(projectId, StandardCharsets.UTF_8),
                      URLEncoder.encode(locale, StandardCharsets.UTF_8),
                      queryJoiner);

              String token = ((ApiKeyAuth) apiClient.getAuthentication("Token")).getApiKey();

              HttpRequest request =
                  HttpRequest.newBuilder()
                      .uri(URI.create(url))
                      .header("Authorization", "token " + token)
                      .header("Accept", "*")
                      .GET()
                      .build();

              HttpResponse<String> response;
              try (HttpClient client = HttpClient.newHttpClient()) {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
              } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
              }

              int statusCode = response.statusCode();
              String responseBody = response.body();

              if (statusCode == 200) {
                return responseBody;
              } else {
                throw new RuntimeException(
                    "Can't download locale. status code " + statusCode + ": " + responseBody);
              }
            })
        .retryWhen(
            retryBackoffSpec.doBeforeRetry(
                doBeforeRetry -> {
                  logAttempt(
                      doBeforeRetry.failure(),
                      "Retrying failed attempt to localeDownload from Phrase, project id: %s, locale: %s"
                          .formatted(projectId, locale));

                  if (onTagErrorRefreshCallback != null
                      && getErrorMessageFromOptionalApiException(doBeforeRetry.failure())
                          .contains("Invalid Download Options. Parameter tags ")) {
                    String newTags = onTagErrorRefreshCallback.get();
                    logger.warn(
                        "Replacing old tags: {} with new tags: {} for download locale",
                        refTags.get(),
                        newTags);
                    refTags.set(newTags);
                  }
                }))
        .doOnError(
            throwable ->
                rethrowExceptionWithLog(
                    throwable,
                    "Final error to localeDownload from Phrase, project id: %s, locale: %s"
                        .formatted(projectId, locale)))
        .block();
  }

  public List<TranslationKey> getKeys(String projectId, String tags) {
    KeysApi keysApi = new KeysApi(apiClient);
    AtomicInteger page = new AtomicInteger(0);
    int batchSize = BATCH_SIZE;
    List<TranslationKey> translationKeys = new ArrayList<>();
    while (true) {
      List<TranslationKey> translationKeysInPage =
          Mono.fromCallable(
                  () -> {
                    logger.info("Fetching keys for project: {}, page: {}", projectId, page);
                    return keysApi.keysList(
                        projectId,
                        null,
                        page.get(),
                        batchSize,
                        null,
                        null,
                        null,
                        "tags:%s".formatted(tags),
                        null);
                  })
              .retryWhen(
                  retryBackoffSpec.doBeforeRetry(
                      doBeforeRetry ->
                          logAttempt(
                              doBeforeRetry.failure(),
                              "Retrying failed attempt to fetch keys for project: %s, page: %s"
                                  .formatted(projectId, page.get()))))
              .doOnError(
                  throwable ->
                      rethrowExceptionWithLog(
                          throwable,
                          "Final error to fetch keys for project: %s, page: %s"
                              .formatted(projectId, page)))
              .block();

      translationKeys.addAll(translationKeysInPage);

      if (translationKeysInPage.size() < batchSize) {
        break;
      } else {
        page.incrementAndGet();
      }
    }
    return translationKeys;
  }

  public List<Tag> listTags(String projectId) {
    TagsApi tagsApi = new TagsApi(apiClient);
    final AtomicInteger page = new AtomicInteger(0);
    List<Tag> tags = new ArrayList<>();
    while (true) {
      List<Tag> tagsInPage =
          Mono.fromCallable(
                  () -> {
                    logger.info("Fetching tags for project: {}", projectId);
                    return tagsApi.tagsList(projectId, null, page.get(), BATCH_SIZE, null);
                  })
              .retryWhen(
                  retryBackoffSpec.doBeforeRetry(
                      doBeforeRetry ->
                          logAttempt(
                              doBeforeRetry.failure(),
                              "Retrying failed attempt to fetch tags for project: %s, page: %d"
                                  .formatted(projectId, page.get()))))
              .doOnError(
                  throwable ->
                      rethrowExceptionWithLog(
                          throwable,
                          "Final error to fetch tags for project: %s, page: %s"
                              .formatted(projectId, page)))
              .block();

      tags.addAll(tagsInPage);

      if (tagsInPage.size() < BATCH_SIZE) {
        break;
      } else {
        page.incrementAndGet();
      }
    }

    return tags;
  }

  public void deleteTags(String projectId, List<String> tagNames) {

    logger.debug("Delete tags: {}", tagNames);

    TagsApi tagsApi = new TagsApi(apiClient);
    Map<String, Throwable> exceptions = new LinkedHashMap<>();
    for (String tagName : tagNames) {
      Mono.fromCallable(
              () -> {
                logger.debug("Deleting tag: %s in project id: %s".formatted(tagName, projectId));
                tagsApi.tagDelete(projectId, tagName, null, null);
                return null;
              })
          .retryWhen(
              retryBackoffSpec.doBeforeRetry(
                  doBeforeRetry -> {
                    logAttempt(
                        doBeforeRetry.failure(),
                        "Retrying failed attempt to delete tag: %s in project id: %s"
                            .formatted(tagName, projectId));
                  }))
          .doOnError(
              throwable -> {
                exceptions.put(tagName, throwable);
                rethrowExceptionWithLog(
                    throwable,
                    "Final error to delete tag: %s in project id: %s"
                        .formatted(tagName, projectId));
              })
          .block();
    }

    if (!exceptions.isEmpty()) {
      List<String> tagsWithErrors = exceptions.keySet().stream().limit(10).toList();
      String andMore = (tagsWithErrors.size() < exceptions.size()) ? " and more." : "";
      throw new PhraseClientException(
          String.format("Can't delete tagNames: %s%s", tagsWithErrors, andMore));
    }
  }

  private void logAttempt(Throwable throwable, String message) {
    String errorMessage = getErrorMessageFromOptionalApiException(throwable);
    logger.info("%s, error: %s".formatted(message, errorMessage), throwable);
  }

  private void rethrowExceptionWithLog(Throwable throwable, String message) {
    String errorMessage = getErrorMessageFromOptionalApiException(throwable);
    logger.error("%s, error: %s".formatted(message, errorMessage));
    if (throwable.getCause() instanceof ApiException) {
      throw new PhraseClientException(errorMessage, (ApiException) throwable.getCause());
    } else {
      throw new RuntimeException(errorMessage, throwable);
    }
  }

  private String getErrorMessageFromOptionalApiException(Throwable t) {
    String errorMessage;
    if (t instanceof ApiException) {
      errorMessage = ((ApiException) t).getResponseBody();
    } else {
      errorMessage = t.getMessage();
    }
    return errorMessage;
  }
}
