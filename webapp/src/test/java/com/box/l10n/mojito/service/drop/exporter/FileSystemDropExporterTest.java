package com.box.l10n.mojito.service.drop.exporter;

import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_IMPORTED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_SOURCE_FILES_NAME;
import com.box.l10n.mojito.service.drop.importer.DropImporterException;
import com.box.l10n.mojito.service.drop.importer.FileSystemDropImporter;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaurambault
 */
public class FileSystemDropExporterTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(FileSystemDropExporterTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    /**
     * All function in one test as it takes long time to create the directories
     * on Box
     */
    @Test
    public void all() throws DropExporterException, DropImporterException, BoxSDKServiceException {

        logger.debug("Test initial creation");
        FileSystemDropExporter fileSystemDropExporter = new FileSystemDropExporter();
        String groupName = testIdWatcher.getEntityName("groupName");
        String dropName = groupName + new SimpleDateFormat(" (EEEE) - dd MMMM YYYY - HH.mm.ss").format(new Date());
        fileSystemDropExporter.init(groupName, dropName);

        logger.debug("Test re-creation from config");
        FileSystemDropExporter recreate = new FileSystemDropExporter();
        recreate.setConfig(fileSystemDropExporter.getConfig());
        recreate.init(groupName, dropName);

        assertEquals(fileSystemDropExporter.getConfig(), recreate.getConfig());

        logger.debug("Make sure the exporter can be initialized only once");
        try {
            recreate.setConfig(fileSystemDropExporter.getConfig());
            fail();
        } catch (AlreadyInitializedExporterException e) {
        }

        logger.debug("Test getDropImporter returns a FileSystemDropImporter");
        FileSystemDropImporter fileSystemDropImporter = (FileSystemDropImporter) recreate.getDropImporter();
        assertEquals(recreate.getFileSystemDropExporterConfig().getDropFolderPath() + "/Localized Files", fileSystemDropImporter.getLocalizedFolderPath().toString());

        logger.debug("Test export source file");
        recreate.exportSourceFile("fr-FR", "fake content");

        File sourceFilesDirectory = Paths.get(recreate.getFileSystemDropExporterConfig().getDropFolderPath(), DROP_FOLDER_SOURCE_FILES_NAME).toFile();
        String[] filesInSourceFilesDirectory = sourceFilesDirectory.list();
        assertEquals(1, filesInSourceFilesDirectory.length);

        logger.debug("Test export imported file");
        recreate.exportImportedFile("filename", "fake content imported", "file comment");

        File importedFilesDirectory = Paths.get(recreate.getFileSystemDropExporterConfig().getDropFolderPath(), DROP_FOLDER_IMPORTED_FILES_NAME).toFile();
        String[] filesInImportedFilesDirectory = importedFilesDirectory.list();
        assertEquals("Must be 1 file for the exported content and 1 for comments", 1, filesInImportedFilesDirectory.length);
        
        logger.debug("Test deleteDrop");
        fileSystemDropExporter.deleteDrop();
    }

    @Test
    public void testGetSourceFileName() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 0, 1, 0, 0, 0);
        
        BoxDropExporter boxDropExporter = new BoxDropExporter();
        String res = boxDropExporter.getSourceFileName(cal.getTime(), "fre");
        assertEquals("fre_01-01-13.xliff", res);
    }

}
