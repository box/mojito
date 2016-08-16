package com.box.l10n.mojito.service.drop.exporter;

import com.box.l10n.mojito.boxsdk.BoxSDKService;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.json.ObjectMapper;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_IMPORTED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_LOCALIZED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_QUERIES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_QUOTES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_SOURCE_FILES_NAME;
import com.box.l10n.mojito.service.drop.importer.BoxDropImporter;
import com.box.l10n.mojito.service.drop.importer.DropImporter;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * A {@link DropExporter} that uses Box as storage and exchange platform.
 *
 * @author jaurambault
 */
@Configurable
public class BoxDropExporter implements DropExporter {

    static Logger logger = LoggerFactory.getLogger(BoxDropExporter.class);

    @Autowired
    BoxSDKService boxSDKService;

    @Autowired
    ObjectMapper objectMapper;

    /**
     * Contains the exporter configuration and is also used to keep track of the
     * state of exporter: initialized or not.
     */
    BoxDropExporterConfig boxDropExporterConfig;

    @Override
    public DropExporterType getDropExporterType() {
        return DropExporterType.BOX;
    }

    @Override
    public String getConfig() {
        return objectMapper.writeValueAsStringUnsafe(boxDropExporterConfig);
    }

    @Override
    public void setConfig(String config) throws AlreadyInitializedExporterException, DropExporterException {

        if (boxDropExporterConfig != null) {
            throw new AlreadyInitializedExporterException();
        }

        logger.debug("Set config: {}", config);

        Preconditions.checkNotNull(config, "config for BoxDropExporterConfig must not be null");

        try {
            boxDropExporterConfig = objectMapper.readValue(config, BoxDropExporterConfig.class);
        } catch (IOException ioe) {
            String msg = "Cannot transform the config String into a BoxDropExporterConfig";
            logger.error(msg, ioe);
            throw new DropExporterInstantiationException(msg, ioe);
        }
    }

    public BoxDropExporterConfig getBoxDropExporterConfig() {
        return boxDropExporterConfig;
    }

    @Override
    public void init(String dropGroupName, String dropName) throws DropExporterInstantiationException {

        Preconditions.checkNotNull(dropGroupName, "dropGroup name must not be null");

        if (boxDropExporterConfig != null) {
            logger.debug("boxDropExporterConfig was set, the filter is already initialized and ready to be used");
        } else {
            logger.debug("No config is set, the exporter must be initalized (create target folders)");
            try {
                createDropFolderTree(dropGroupName, dropName, new Date());
            } catch (BoxSDKServiceException bsdkse) {
                String msg = "Couldn't create the BoxDropFolderTree";
                logger.error(msg, bsdkse);
                throw new DropExporterInstantiationException(msg, bsdkse);
            }
        }
    }

    /**
     * Converts a string into a valid Box Item name.
     *
     * @param string to be converted, not {@code null}
     * @return a valid Box Item name
     */
    private String convertToBoxItemName(String string) {
        return string.replace("/", "_").replace("\\", "_");
    }

    /**
     * Creates the drop folder tree that will be used to upload files and sets
     * the {@link #boxDropExporterConfig} accordingly.
     *
     * @param dropGroupName group name associated with this exporter
     * @param dropName name of the drop
     * @param uploadDate  @throws BoxSDKServiceException
     */
    private void createDropFolderTree(String dropGroupName, String dropName, Date uploadDate) throws BoxSDKServiceException {

        logger.debug("Create DropFolderTree");

        BoxFolder repositoryFolder = getDropGroupFolder(dropGroupName);

        BoxFolder dropFolder = boxSDKService.createSharedFolder(convertToBoxItemName(dropName), repositoryFolder.getID());
        String dropFolderId = dropFolder.getID();

        BoxFolder sourceFilesFolder = boxSDKService.createSharedFolder(DROP_FOLDER_SOURCE_FILES_NAME, dropFolderId);
        BoxFolder localizedFilesFolder = boxSDKService.createSharedFolder(DROP_FOLDER_LOCALIZED_FILES_NAME, dropFolderId);
        BoxFolder importedFilesFolder = boxSDKService.createSharedFolder(DROP_FOLDER_IMPORTED_FILES_NAME, dropFolderId);
        BoxFolder queriesFolder = boxSDKService.createSharedFolder(DROP_FOLDER_QUERIES_NAME, dropFolderId);
        BoxFolder quotesFolder = boxSDKService.createSharedFolder(DROP_FOLDER_QUOTES_NAME, dropFolderId);

        logger.debug("Sets boxDropExporterConfig based on created drop folder tree");
        boxDropExporterConfig = new BoxDropExporterConfig();
        boxDropExporterConfig.setDropFolderId(dropFolderId);
        boxDropExporterConfig.setSourceFolderId(sourceFilesFolder.getID());
        boxDropExporterConfig.setLocalizedFolderId(localizedFilesFolder.getID());
        boxDropExporterConfig.setImportedFolderId(importedFilesFolder.getID());
        boxDropExporterConfig.setQueriesFolderId(queriesFolder.getID());
        boxDropExporterConfig.setQuotesFolderId(quotesFolder.getID());
        boxDropExporterConfig.setUploadDate(uploadDate);
    }

    /**
     * Gets the drop group folder that contains the drops. If the folder doesn't
     * exist yet, create it.
     *
     * @param dropGroupName the repository name
     * @return the folder that contains drop for a given repository
     * @throws BoxSDKServiceException
     */
    private BoxFolder getDropGroupFolder(String dropGroupName) throws BoxSDKServiceException {

        String folderName = convertToBoxItemName(dropGroupName);

        logger.debug("Get repository folder for repository: {}", folderName);

        BoxFolder dropGroupFolder = boxSDKService.getFolderWithNameAndParentFolderId(folderName, boxSDKService.getBoxSDKServiceConfig().getDropsFolderId());

        if (dropGroupFolder == null) {
            logger.debug("Folder for repository: {} doesn't exist, create it", folderName);
            dropGroupFolder = boxSDKService.createSharedFolder(folderName, boxSDKService.getBoxSDKServiceConfig().getDropsFolderId());
        }

        return dropGroupFolder;
    }

    @Override
    public void exportSourceFile(String bcp47tag, String fileContent) throws DropExporterException {
        checkIsInitialized();

        try {
            boxSDKService.uploadFile(
                    boxDropExporterConfig.getSourceFolderId(),
                    getSourceFileName(boxDropExporterConfig.getUploadDate(), bcp47tag), fileContent);

        } catch (BoxSDKServiceException bsdkse) {
            String msg = "Cannot export file";
            logger.error(msg, bsdkse);
            throw new DropExporterException(msg, bsdkse);
        }
    }

    @Override
    public void exportImportedFile(String filename, String fileContent, String comment) throws DropExporterException {
        checkIsInitialized();

        try {
            BoxFile uploadFile = boxSDKService.uploadFile(boxDropExporterConfig.getImportedFolderId(), filename, fileContent);

            if (comment != null) {
                boxSDKService.addCommentToFile(uploadFile.getID(), comment);
            }

        } catch (BoxSDKServiceException bsdkse) {
            String msg = "Cannot export the \"imported\" file";
            logger.error(msg, bsdkse);
            throw new DropExporterException(msg, bsdkse);
        }
    }

    @Override
    public void addCommentToFile(String fileId, String comment) throws DropExporterException {
        
        if (comment != null) {
            try {
                boxSDKService.addCommentToFile(fileId, comment);
            } catch (BoxSDKServiceException bsdkse) {
                throw new DropExporterException("Cannot add comment to file", bsdkse);
            }
        }
    }

    @Override
    public DropImporter getDropImporter() {
        checkIsInitialized();

        return new BoxDropImporter(boxDropExporterConfig.getLocalizedFolderId());
    }

    @Override
    public void deleteDrop() throws DropExporterException {
        checkIsInitialized();

        try {
            boxSDKService.deleteFolderAndItsContent(boxDropExporterConfig.getDropFolderId());
        } catch (BoxSDKServiceException bsdkse) {
            String msg = "Cannot delete the drop folder";
            logger.error(msg, bsdkse);
            throw new DropExporterException(msg, bsdkse);
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
        Preconditions.checkNotNull(boxDropExporterConfig, getClass().getSimpleName() + " must be initialized first");
    }

}
