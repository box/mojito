package com.box.l10n.mojito.service.drop.importer;

import com.box.l10n.mojito.boxsdk.BoxSDKService;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.service.drop.exporter.BoxDropExporter;
import com.box.sdk.BoxFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DropImporter} that read content from Box, see related
 * {@link BoxDropExporter}.
 *
 * @author jaurambault
 */
@Configurable
public class BoxDropImporter implements DropImporter {

    static Logger logger = LoggerFactory.getLogger(BoxDropImporter.class);

    @Autowired
    BoxSDKService boxSDKService;

    /**
     * Id of the folder that contains the localized files to be imported
     */
    String localizedFolderId;

    /**
     * Creates an instance to import files from the specified folder.
     *
     * @param localizedFolderId id of the folder that contains the localized
     * files to be imported
     */
    public BoxDropImporter(String localizedFolderId) {
        this.localizedFolderId = localizedFolderId;
    }

    public String getLocalizedFolderId() {
        return localizedFolderId;
    }

    @Override
    public void downloadFileContent(DropFile dropFile) throws DropImporterException {

        logger.debug("Download file content for dropFile: {}", dropFile.getId());

        try {
            String content = boxSDKService.getFileContent(dropFile.getId());
            dropFile.setContent(content);
        } catch (BoxSDKServiceException bsdkse) {
            throw new DropImporterException("Cannot download drop file content", bsdkse);
        }
    }

    @Override
    public List<DropFile> getFiles() throws DropImporterException {

        logger.debug("Gets drop files to be imported");
        List<DropFile> dropFiles = new ArrayList<>();

        List<BoxFile> boxFiles;
        try {
            boxFiles = boxSDKService.listFiles(localizedFolderId);
        } catch (BoxSDKServiceException bsdkse) {
            throw new DropImporterException("Couldn't list drop files to be imported", bsdkse);
        }

        for (BoxFile boxFile : boxFiles) {
            dropFiles.add(boxFileToDropFile(boxFile));
        }

        return dropFiles;
    }

    /**
     * Converts a {@link BoxFile} to {@link DropFile}
     *
     * @param boxFile file to be converted
     * @return {@link DropFile} resulting of {@link BoxFile} conversion
     */
    private DropFile boxFileToDropFile(BoxFile boxFile) {
        String name = boxFile.getInfo().getName();
        String bcp47Tag = getBcp47TagFromFileName(name);
        return new DropFile(boxFile.getID(), bcp47Tag, name, boxFile.getInfo().getExtension());
    }

    /**
     * Gets the BCP47 tag from the localized file filename.
     *
     * <p>
     * The tag is at beginning of the filename and is delimited with '_'
     *
     * @param fileName
     * @return the language of the localized file
     */
    String getBcp47TagFromFileName(String fileName) {
        return fileName.substring(0, fileName.indexOf("_"));
    }

}
