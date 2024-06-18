package com.box.l10n.mojito.service.thirdparty.smartling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.amazonaws.SdkClientException;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SmartlingResultProcessorTest {

  SmartlingResultProcessor processor;

  @Mock S3BlobStorage s3BlobStorage;

  @Captor ArgumentCaptor<String> nameCaptor;

  @Captor ArgumentCaptor<byte[]> byteArrayCaptor;

  @Captor ArgumentCaptor<Retention> retentionCaptor;

  @Before
  public void setUp() {
    processor = new SmartlingResultProcessor();
    processor.s3BlobStorage = this.s3BlobStorage;
  }

  @Test
  public void testProcessPush() throws Exception {

    List<SmartlingFile> files = fileList(5);
    String requestId = UUID.randomUUID().toString();
    SmartlingOptions smartlingOptions =
        SmartlingOptions.parseList(ImmutableList.of("dry-run=true", "request-id=" + requestId));
    processor.processPush(files, smartlingOptions);

    verify(s3BlobStorage, times(1))
        .put(nameCaptor.capture(), byteArrayCaptor.capture(), retentionCaptor.capture());

    assertThat(nameCaptor.getValue()).contains(requestId);
    assertThat(nameCaptor.getValue()).contains("_push_");
    assertThat(retentionCaptor.getValue()).isEqualTo(Retention.MIN_1_DAY);
    assertThat(byteArrayCaptor.getValue()).isNotEmpty();

    List<SmartlingFile> smartlingFiles = readFilesFromZipBytes(byteArrayCaptor.getValue());
    assertThat(smartlingFiles)
        .extracting("fileName", "fileContent")
        .containsExactlyInAnyOrder(
            tuple(files.get(0).getFileName(), files.get(0).getFileContent()),
            tuple(files.get(1).getFileName(), files.get(1).getFileContent()),
            tuple(files.get(2).getFileName(), files.get(2).getFileContent()),
            tuple(files.get(3).getFileName(), files.get(3).getFileContent()),
            tuple(files.get(4).getFileName(), files.get(4).getFileContent()));
  }

  @Test
  public void testProcessPushThrowsException() {

    doThrow(new SdkClientException("an exception"))
        .when(s3BlobStorage)
        .put(any(String.class), any(byte[].class), eq(Retention.MIN_1_DAY));

    List<SmartlingFile> files = fileList(5);
    String requestId = UUID.randomUUID().toString();
    SmartlingOptions smartlingOptions =
        SmartlingOptions.parseList(ImmutableList.of("dry-run=true", "request-id=" + requestId));

    assertThatThrownBy(() -> processor.processPush(files, smartlingOptions))
        .hasMessageContaining("An error occurred when uploading a push result zip file")
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testProcessPushTranslations() throws Exception {

    List<SmartlingFile> files = fileList(5);
    String requestId = UUID.randomUUID().toString();
    SmartlingOptions smartlingOptions =
        SmartlingOptions.parseList(ImmutableList.of("dry-run=true", "request-id=" + requestId));
    processor.processPushTranslations(files, smartlingOptions);

    verify(s3BlobStorage)
        .put(nameCaptor.capture(), byteArrayCaptor.capture(), retentionCaptor.capture());

    assertThat(nameCaptor.getValue()).contains(requestId);
    assertThat(nameCaptor.getValue()).contains("_push_");
    assertThat(retentionCaptor.getValue()).isEqualTo(Retention.MIN_1_DAY);
    assertThat(byteArrayCaptor.getValue()).isNotEmpty();

    List<SmartlingFile> smartlingFiles = readFilesFromZipBytes(byteArrayCaptor.getValue());
    assertThat(smartlingFiles)
        .extracting("fileName", "fileContent")
        .containsExactlyInAnyOrder(
            tuple(files.get(0).getFileName(), files.get(0).getFileContent()),
            tuple(files.get(1).getFileName(), files.get(1).getFileContent()),
            tuple(files.get(2).getFileName(), files.get(2).getFileContent()),
            tuple(files.get(3).getFileName(), files.get(3).getFileContent()),
            tuple(files.get(4).getFileName(), files.get(4).getFileContent()));
  }

  @Test
  public void testProcessPushNoBlobStorage() {

    processor.s3BlobStorage = null;

    List<SmartlingFile> files = fileList(2);
    SmartlingOptions smartlingOptions =
        SmartlingOptions.parseList(ImmutableList.of("dry-run=other", "request-id=test"));
    processor.processPush(files, smartlingOptions);

    verifyNoInteractions(s3BlobStorage);
  }

  @Test
  public void testProcessPushEmptyList() {

    List<SmartlingFile> files = Collections.emptyList();
    SmartlingOptions smartlingOptions =
        SmartlingOptions.parseList(ImmutableList.of("dry-run=true", "request-id=test"));
    processor.processPush(files, smartlingOptions);

    verifyNoInteractions(s3BlobStorage);
  }

  @Test
  public void testProcessDryRunOff() {

    List<SmartlingFile> files = fileList(2);
    SmartlingOptions smartlingOptions =
        SmartlingOptions.parseList(ImmutableList.of("dry-run=false", "request-id=test"));
    processor.processPush(files, smartlingOptions);

    verifyNoInteractions(s3BlobStorage);
  }

  @Test
  public void testZipFiles() throws Exception {
    List<SmartlingFile> files = fileList(4);
    Path zipFile =
        processor.zipFiles(files, "my-request-id-" + UUID.randomUUID().toString() + "-some-type");
    List<SmartlingFile> zippedFiles = readFilesFromZip(zipFile);
    assertThat(zippedFiles)
        .extracting("fileName", "fileContent")
        .containsExactlyInAnyOrder(
            tuple(files.get(0).getFileName(), files.get(0).getFileContent()),
            tuple(files.get(1).getFileName(), files.get(1).getFileContent()),
            tuple(files.get(2).getFileName(), files.get(2).getFileContent()),
            tuple(files.get(3).getFileName(), files.get(3).getFileContent()));

    Files.deleteIfExists(zipFile.toFile().toPath());
  }

  @Test
  public void testZipFilesEmpty() throws IOException {
    Path zipFile = processor.zipFiles(Collections.emptyList(), "invalid");
    assertThat(readFilesFromZip(zipFile)).isEmpty();
  }

  public static List<SmartlingFile> fileList(int fileCount) {
    return IntStream.range(0, fileCount)
        .mapToObj(
            num -> {
              SmartlingFile file = new SmartlingFile();
              file.setFileName(String.format("repositoryName%d/file%d.xml", num, num));
              file.setFileContent(
                  String.format("some random content %s %s", num, UUID.randomUUID().toString()));
              return file;
            })
        .collect(Collectors.toList());
  }

  public static List<SmartlingFile> readFilesFromZipBytes(byte[] bytes) throws IOException {
    return readFilesFromZipInputStream(new ByteArrayInputStream(bytes));
  }

  public static List<SmartlingFile> readFilesFromZip(Path zipFile) throws IOException {
    return readFilesFromZipInputStream(new FileInputStream(zipFile.toString()));
  }

  private static List<SmartlingFile> readFilesFromZipInputStream(InputStream inputStream)
      throws IOException {
    List<SmartlingFile> result = new ArrayList<>();

    try (ZipInputStream zis = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {

      StringBuilder s = new StringBuilder();
      byte[] buffer = new byte[1024];
      int read = 0;
      ZipEntry entry;

      while ((entry = zis.getNextEntry()) != null) {

        while ((read = zis.read(buffer, 0, 1024)) >= 0) {
          s.append(new String(buffer, 0, read));
        }

        SmartlingFile sf = new SmartlingFile();
        sf.setFileName(entry.getName());
        sf.setFileContent(s.toString());
        result.add(sf);
        s = new StringBuilder();
      }
    }

    return result;
  }
}
