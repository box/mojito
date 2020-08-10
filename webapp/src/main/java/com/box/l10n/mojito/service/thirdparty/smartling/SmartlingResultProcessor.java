package com.box.l10n.mojito.service.thirdparty.smartling;

import com.amazonaws.SdkClientException;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class SmartlingResultProcessor {

    static Logger logger = LoggerFactory.getLogger(SmartlingResultProcessor.class);

    private static final String NAME_FORMAT = "%s_%s_%s.zip";

    @Autowired(required=false)
    S3BlobStorage s3BlobStorage;

    public String processPush(List<SmartlingFile> files,
                              SmartlingOptions options) {
        return processAction(files, options, "push");
    }

    public String processPushTranslations(List<SmartlingFile> files,
                                          SmartlingOptions options){
        return processAction(files, options, "push_translations");
    }

    private String processAction(List<SmartlingFile> files,
                                 SmartlingOptions options,
                                 String action) {
        String result = null;

        if (!files.isEmpty() && options.isDryRun() && s3BlobStorage != null){
            try {
                result = uploadFiles(files, options.getRequestId(), action);
                logger.info("{} result for request id {} uploaded to: {}", action, options.getRequestId(), result);
            } catch (IOException | SdkClientException e) {
                String errorMessage = String.format("An error ocurred when uploading a %s result zip file", action);
                logger.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        }

        return result;
    }

    String uploadFiles(List<SmartlingFile> files, String requestId, String type) throws IOException {
        String name = String.format(NAME_FORMAT, requestId, type, ZonedDateTime.now().toEpochSecond());
        Path zipFile = zipFiles(files, name);
        byte[] content = Files.readAllBytes(zipFile);
        s3BlobStorage.put(name, content, Retention.MIN_1_DAY);
        return s3BlobStorage.getS3Url(name);
    }

    Path zipFiles(List<SmartlingFile> files, String name) throws IOException {
        Path tmpDir = Files.createTempDirectory(name);
        for (SmartlingFile file : files) {
            Path filePath = Paths.get(tmpDir.toString(), file.getFileName());
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getFileContent().getBytes(StandardCharsets.UTF_8));
        }
        return zipDirectory(tmpDir);
    }

    private Path zipDirectory(Path sourceDirPath) throws IOException {
        Path zipPath = Files.createFile(Paths.get(sourceDirPath + ".zip"));

        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zs.setLevel(ZipOutputStream.STORED);
            Files.walk(sourceDirPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }

        return zipPath;
    }
}
