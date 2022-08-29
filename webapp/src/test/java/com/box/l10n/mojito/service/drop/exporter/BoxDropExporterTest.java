package com.box.l10n.mojito.service.drop.exporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import com.box.l10n.mojito.boxsdk.BoxSDKService;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceConfigFromProperties;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.drop.importer.BoxDropImporter;
import com.box.l10n.mojito.service.drop.importer.DropImporterException;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.category.BoxSDKTest;
import com.box.l10n.mojito.test.category.SlowTest;
import com.box.sdk.BoxFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** @author jaurambault */
public class BoxDropExporterTest extends ServiceTestBase {

  static Logger logger = LoggerFactory.getLogger(DropExporterService.class);

  @Autowired BoxSDKServiceConfigFromProperties boxSDKServiceConfigFromProperties;

  @Autowired BoxSDKService boxSDKService;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  /** All function in one test as it takes long time to create the directories on Box */
  @Test
  @Category({BoxSDKTest.class, SlowTest.class})
  public void all() throws DropExporterException, DropImporterException, BoxSDKServiceException {

    assumeTrue(StringUtils.isNotEmpty(boxSDKServiceConfigFromProperties.getRootFolderId()));

    logger.debug("Test initial creation");
    BoxDropExporter boxDropExporter = new BoxDropExporter();
    String groupName = testIdWatcher.getEntityName("groupName");
    String dropName =
        groupName + new SimpleDateFormat(" (EEEE) - dd MMMM YYYY - HH.mm.ss").format(new Date());
    boxDropExporter.init(groupName, dropName);

    logger.debug("Test re-creation from config");
    BoxDropExporter recreate = new BoxDropExporter();
    recreate.setConfig(boxDropExporter.getConfig());
    recreate.init(groupName, dropName);

    assertEquals(boxDropExporter.getConfig(), recreate.getConfig());

    logger.debug("Make sure the exporter can be initialized only once");
    try {
      recreate.setConfig(boxDropExporter.getConfig());
      fail();
    } catch (AlreadyInitializedExporterException e) {
    }

    logger.debug("Test getDropImporter returns a BoxDropImporter");
    BoxDropImporter boxDropImporter = (BoxDropImporter) recreate.getDropImporter();
    assertEquals(
        recreate.getBoxDropExporterConfig().getLocalizedFolderId(),
        boxDropImporter.getLocalizedFolderId());

    logger.debug("Test export source file");
    recreate.exportSourceFile("fr-FR", "fake content");
    List<BoxFile> exportSourceFiles =
        boxSDKService.listFiles(recreate.getBoxDropExporterConfig().getSourceFolderId());
    assertEquals(1, exportSourceFiles.size());

    logger.debug("Test export imported file");
    recreate.exportImportedFile("filename", "fake content imported", "file comment");
    List<BoxFile> exportImportedFiles =
        boxSDKService.listFiles(recreate.getBoxDropExporterConfig().getImportedFolderId());
    assertEquals(1, exportImportedFiles.size());

    logger.debug("Test deleteDrop");
    boxDropExporter.deleteDrop();
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
