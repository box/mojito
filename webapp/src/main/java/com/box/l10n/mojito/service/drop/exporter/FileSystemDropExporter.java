package com.box.l10n.mojito.service.drop.exporter;

import com.box.l10n.mojito.json.ObjectMapper;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_IMPORTED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_LOCALIZED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_QUERIES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_QUOTES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_SOURCE_FILES_NAME;
import com.box.l10n.mojito.service.drop.importer.DropImporter;
import com.box.l10n.mojito.service.drop.importer.FileSystemDropImporter;
import java.nio.charset.StandardCharsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.FileSystemUtils;

/**
 * A {@link DropExporter} that uses the file system as storage.
 *
 * @author jaurambault
 */
@Configurable
public class FileSystemDropExporter implements DropExporter {

    static Logger logger = LoggerFactory.getLogger(FileSystemDropExporter.class);

    final String DROP_FOLDER_COMMENTS_NAME = "Comments";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FileSystemDropExporterConfigFromProperties fileSystemDropExporterConfigFromProperties;

    /**
     * Contains the exporter configuration and is also used to keep track of the
     * state of exporter: initialized or not.
     */
    FileSystemDropExporterConfig fileSystemDropExporterConfig;

    @Override
    public DropExporterType getDropExporterType() {
        return DropExporterType.FILE_SYSTEM;
    }

    @Override
    public String getConfig() {
        return objectMapper.writeValueAsStringUnsafe(fileSystemDropExporterConfig);
    }

    @Override
    public void setConfig(String config) throws AlreadyInitializedExporterException, DropExporterException {

        if (fileSystemDropExporterConfig != null) {
            throw new AlreadyInitializedExporterException();
        }

        logger.debug("Set config: {}", config);

        Preconditions.checkNotNull(config, "config for FileSytemDropExporterConfig must not be null");

        try {
            fileSystemDropExporterConfig = objectMapper.readValue(config, FileSystemDropExporterConfig.class);
        } catch (IOException ioe) {
            String msg = "Cannot transform the config String into a FileSytemDropExporterConfig";
            logger.error(msg, ioe);
            throw new DropExporterInstantiationException(msg, ioe);
        }
    }

    public FileSystemDropExporterConfig getFileSystemDropExporterConfig() {
        return fileSystemDropExporterConfig;
    }

    @Override
    public void init(String dropGroupName, String dropName) throws DropExporterInstantiationException {

        Preconditions.checkNotNull(dropGroupName, "dropGroup name must not be null");

        if (fileSystemDropExporterConfig != null) {
            logger.debug("fileSystemDropExporterConfig was set, the filter is already initialized and ready to be used");
        } else {
            logger.debug("No config is set, the exporter must be initalized (create target folders)");
            try {
                createDropFolderTree(dropGroupName, dropName, new Date());
            } catch (IOException ioe) {
                String msg = "Couldn't create the BoxDropFolderTree";
                logger.error(msg, ioe);
                throw new DropExporterInstantiationException(msg, ioe);
            }
        }
    }

    /**
     * Converts a string into a valid file name.
     *
     * @param string to be converted, not {@code null}
     * @return a valid file name
     */
    private String convertToFileName(String string) {
        return string.replace("/", "_").replace("\\", "_");
    }

    /**
     * Creates the drop folder tree that will be used to upload files and sets
     * the {@link #fileSystemDropExporterConfig} accordingly.
     *
     * @param dropGroupName group name associated with this exporter
     * @param dropName name of the drop
     * @param uploadDate
     * @throws IOException
     */
    private void createDropFolderTree(String dropGroupName, String dropName, Date uploadDate) throws IOException {

        logger.debug("Create DropFolderTree");

        File repositoryFolder = getDropGroupFolder(dropGroupName);

        File dropFolder = new File(repositoryFolder, convertToFileName(dropName));
        dropFolder.mkdir();

        File sourceFilesFolder = new File(dropFolder, DROP_FOLDER_SOURCE_FILES_NAME);
        File localizedFilesFolder = new File(dropFolder, DROP_FOLDER_LOCALIZED_FILES_NAME);
        File importedFilesFolder = new File(dropFolder, DROP_FOLDER_IMPORTED_FILES_NAME);
        File queriesFolder = new File(dropFolder, DROP_FOLDER_QUERIES_NAME);
        File quotesFolder = new File(dropFolder, DROP_FOLDER_QUOTES_NAME);

        sourceFilesFolder.mkdir();
        localizedFilesFolder.mkdir();
        importedFilesFolder.mkdir();
        queriesFolder.mkdir();
        quotesFolder.mkdir();

        logger.debug("Sets fileSystemDropExporterConfig based on created drop folder tree");
        fileSystemDropExporterConfig = new FileSystemDropExporterConfig();
        fileSystemDropExporterConfig.setDropFolderPath(dropFolder.getPath());
        fileSystemDropExporterConfig.setUploadDate(uploadDate);
    }

    /**
     * Gets the drop group folder that contains the drops. If the folder doesn't
     * exist yet, create it.
     *
     * @param dropGroupName the repository name
     * @return the folder that contains drop for a given repository
     */
    private File getDropGroupFolder(String dropGroupName) {

        String folderName = convertToFileName(dropGroupName);

        logger.debug("Get repository folder for repository: {}", folderName);

        File dropGroupFolder = fileSystemDropExporterConfigFromProperties.getBasePath().resolve(folderName).toFile();

        if (!dropGroupFolder.exists()) {
            logger.debug("Folder for repository: {} doesn't exist, create it", folderName);
            dropGroupFolder.mkdirs();
        }

        return dropGroupFolder;
    }

    @Override
    public void exportSourceFile(String bcp47tag, String fileContent) throws DropExporterException {
        checkIsInitialized();

        try {
            Path sourceFilePath = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_SOURCE_FILES_NAME, getSourceFileName(fileSystemDropExporterConfig.getUploadDate(), bcp47tag));
            Files.write(fileContent, sourceFilePath.toFile(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            String msg = "Cannot export file";
            logger.error(msg, ex);
            throw new DropExporterException(msg, ex);
        }
    }

    @Override
    public void exportImportedFile(String filename, String fileContent, String comment) throws DropExporterException {
        checkIsInitialized();

        try {
            Path importedFilePath = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_IMPORTED_FILES_NAME, filename);
            Files.write(fileContent, importedFilePath.toFile(), StandardCharsets.UTF_8);

            if (comment != null) {
                addCommentToFile(importedFilePath.toString(), comment);
            }
        } catch (IOException ex) {
            String msg = "Cannot export file";
            logger.error(msg, ex);
            throw new DropExporterException(msg, ex);
        }
    }

    @Override
    public void addCommentToFile(String fileId, String comment) throws DropExporterException {

        try {
            File commentFile = getPathToCommentsFile(fileId).toFile();
            Files.createParentDirs(commentFile);
            Files.append(comment, commentFile, StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            throw new DropExporterException("Cannot add comment to file", ioe);
        }
    }

    Path getPathToCommentsFile(String fileId) {
        Path dropPath = Paths.get(fileSystemDropExporterConfig.getDropFolderPath());
        Path relativeFileId = dropPath.relativize(Paths.get(fileId));
        Path commentPath = dropPath.resolve(DROP_FOLDER_COMMENTS_NAME).resolve(relativeFileId);
        return commentPath;
    }

    @Override
    public DropImporter getDropImporter() {
        checkIsInitialized();

        Path localizedFolderPath = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_LOCALIZED_FILES_NAME);
        return new FileSystemDropImporter(localizedFolderPath);
    }

    @Override
    public void deleteDrop() throws DropExporterException {
        checkIsInitialized();

        Path deletePath = Paths.get(fileSystemDropExporterConfig.getDropFolderPath());
        boolean deleteRecursively = FileSystemUtils.deleteRecursively(deletePath.toFile());

        if (!deleteRecursively) {
            String msg = "Cannot delete the drop folder";
            logger.error(msg);
            throw new DropExporterException(msg);
        }
    }

    /**
     * Gets the filename of the source file to be saved in Box.
     *
     * @param uploadDate the date the XLIFF file was uploaded
     * @param bcp47tag the target language
     * @return the filename of the source file be saved in Box
     */
    String getSourceFileName(Date uploadDate, String bcp47tag) {
        return bcp47tag + "_" + new SimpleDateFormat("MM-dd-yy").format(uploadDate) + ".xliff";
    }

    /**
     * Checks that the exporter has been initialized
     */
    private void checkIsInitialized() {
        Preconditions.checkNotNull(fileSystemDropExporterConfig, getClass().getSimpleName() + " must be initialized first");
    }

}
