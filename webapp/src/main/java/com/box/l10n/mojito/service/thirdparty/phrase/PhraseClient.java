package com.box.l10n.mojito.service.thirdparty.phrase;

import static com.box.l10n.mojito.io.Files.createDirectories;
import static com.box.l10n.mojito.io.Files.createTempDirectory;
import static com.box.l10n.mojito.io.Files.write;

import com.box.l10n.mojito.json.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.phrase.client.ApiClient;
import com.phrase.client.ApiException;
import com.phrase.client.api.KeysApi;
import com.phrase.client.api.LocalesApi;
import com.phrase.client.api.TagsApi;
import com.phrase.client.api.UploadsApi;
import com.phrase.client.model.Tag;
import com.phrase.client.model.TranslationKey;
import com.phrase.client.model.Upload;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

  public Upload uploadAndWait(
      String projectId,
      String localeId,
      String fileFormat,
      String fileName,
      String fileContent,
      List<String> tags) {

    String uploadId =
        uploadCreateFile(projectId, localeId, fileFormat, fileName, fileContent, tags);
    return waitForUploadToFinish(projectId, uploadId);
  }

  Upload waitForUploadToFinish(String projectId, String uploadId) {
    UploadsApi uploadsApi = new UploadsApi(apiClient);

    try {
      logger.debug("Waiting for upload to finish: {}", uploadId);

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

      return upload;
    } catch (ApiException e) {
      logger.error("Error calling Phrase for waitForUploadToFinish: {}", e.getResponseBody());
      throw new PhraseClientException(e);
    }
  }

  public String uploadCreateFile(
      String projectId,
      String localeId,
      String fileFormat,
      String fileName,
      String fileContent,
      List<String> tags) {

    Path tmpWorkingDirectory = null;

    try {
      tmpWorkingDirectory = createTempDirectory("phrase-integration");

      if (tmpWorkingDirectory.toFile().exists()) {
        logger.info("Created temporary working directory: {}", tmpWorkingDirectory);
      }

      Path fileToUpload = tmpWorkingDirectory.resolve(fileName);

      logger.info("Create file: {}", fileToUpload);
      createDirectories(fileToUpload.getParent());
      write(fileToUpload, fileContent);

      Upload upload =
          uploadsApiUploadCreateWithRetry(projectId, localeId, fileFormat, tags, fileToUpload);

      return upload.getId();
    } finally {
      if (tmpWorkingDirectory != null) {
        com.box.l10n.mojito.io.Files.deleteRecursivelyIfExists(tmpWorkingDirectory);
      }
    }
  }

  Upload uploadsApiUploadCreateWithRetry(
      String projectId, String localeId, String fileFormat, List<String> tags, Path fileToUpload) {

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
                        null,
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

  public void removeKeysNotTaggedWith(String projectId, String tag) {
    logger.info("Removing keys not tagged with: {}", tag);

    Mono.fromCallable(
            () -> {
              KeysApi keysApi = new KeysApi(apiClient);
              keysApi.keysDeleteCollection(projectId, null, null, "-tags:%s".formatted(tag), null);
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

  public String localeDownload(String projectId, String locale, String fileFormat) {
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
                      null,
                      null);

              String localeDownloadContent = Files.readString(file.toPath());
              logger.debug("File: {}, Content: {}", file.toPath(), localeDownloadContent);

              return localeDownloadContent;
            })
        .retryWhen(
            retryBackoffSpec.doBeforeRetry(
                doBeforeRetry ->
                    logAttempt(
                        doBeforeRetry.failure(),
                        "Retrying failed attempt to localeDownload from Phrase, project id: %s, locale: %s"
                            .formatted(projectId, locale))))
        .doOnError(
            throwable ->
                rethrowExceptionWithLog(
                    throwable,
                    "Final error to localeDownload from Phrase, project id: %s, locale: %s"
                        .formatted(projectId, locale)))
        .block();
  }

  public List<TranslationKey> getKeys(String projectId) {
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
                        projectId, null, page.get(), batchSize, null, null, null, null, null);
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

  public void deleteTags(String projectId, List<Tag> tags) {
    TagsApi tagsApi = new TagsApi(apiClient);
    Map<String, Throwable> exceptions = new LinkedHashMap<>();
    for (Tag tag : tags) {
      Mono.fromCallable(
              () -> {
                logger.debug(
                    "Deleting tag: %s in project id: %s".formatted(tag.getName(), projectId));
                tagsApi.tagDelete(projectId, tag.getName(), null, null);
                return null;
              })
          .retryWhen(
              retryBackoffSpec.doBeforeRetry(
                  doBeforeRetry -> {
                    logAttempt(
                        doBeforeRetry.failure(),
                        "Retrying failed attempt to delete tag: %s in project id: %s"
                            .formatted(tag.getName(), projectId));
                  }))
          .doOnError(
              throwable -> {
                exceptions.put(tag.getName(), throwable);
                rethrowExceptionWithLog(
                    throwable,
                    "Final error to delete tag: %s in project id: %s"
                        .formatted(tag.getName(), projectId));
              })
          .block();
    }

    if (!exceptions.isEmpty()) {
      List<String> tagsWithErrors = exceptions.keySet().stream().limit(10).toList();
      String andMore = (tagsWithErrors.size() < exceptions.size()) ? " and more." : "";
      throw new PhraseClientException(
          String.format("Can't delete tags: %s%s", tagsWithErrors, andMore));
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
