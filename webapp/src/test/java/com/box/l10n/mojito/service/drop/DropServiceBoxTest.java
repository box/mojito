package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.boxsdk.BoxFileWithContent;
import com.box.l10n.mojito.boxsdk.BoxSDKService;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.okapi.XliffState;
import com.box.l10n.mojito.service.drop.exporter.BoxDropExporter;
import com.box.l10n.mojito.service.drop.exporter.BoxDropExporterConfig;
import com.box.l10n.mojito.service.drop.exporter.DropExporterException;
import com.box.l10n.mojito.test.XliffUtils;
import com.box.l10n.mojito.test.category.BoxSDKTest;
import com.box.l10n.mojito.test.category.SlowTest;
import com.box.sdk.BoxFile;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author jaurambault
 */
@Ignore("won't work without @DirtiesContext which makes the build to slow")
@SpringBootTest(properties = {"l10n.dropexporter.type=BOX"},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DropServiceBoxTest extends DropServiceTest {

    static Logger logger = LoggerFactory.getLogger(DropServiceBoxTest.class);

    @Autowired
    BoxSDKService boxSDKService;

    @Before
    public void checkBoxConfig() {
        //TODO(P1) here we'll need to check if Box credential are available, if not don't run tests
        Assume.assumeTrue(true);
    }

    @Test
    @Category({BoxSDKTest.class, SlowTest.class})
    @Override
    public void testCreateDrop() throws Exception {
        super.testCreateDrop();
    }

    @Test
    @Category({BoxSDKTest.class, SlowTest.class})
    @Override
    public void forTranslation() throws Exception {
        super.forTranslation();
    }

    @Test
    @Category({BoxSDKTest.class, SlowTest.class})
    @Override
    public void forTranslationWithTranslationAddedAfterExport() throws Exception {
        super.forTranslationWithTranslationAddedAfterExport();
    }

    @Test
    @Category({BoxSDKTest.class, SlowTest.class})
    @Override
    public void forReview() throws Exception {
        super.forTranslation();
    }

    @Test
    @Category({BoxSDKTest.class, SlowTest.class})
    @Override
    public void allWithSevereError() throws Exception {
        super.allWithSevereError();
    }

    @Override
    public void localizeDropFiles(Drop drop, int round) throws BoxSDKServiceException, DropExporterException {

        logger.debug("Localize files in a drop for testing");

        BoxDropExporter boxDropExporter = (BoxDropExporter) dropExporterService.recreateDropExporter(drop);
        BoxDropExporterConfig boxDropExporterConfig = boxDropExporter.getBoxDropExporterConfig();

        List<BoxFile> sourceFiles = boxSDKService.listFiles(boxDropExporterConfig.getSourceFolderId());

        for (BoxFile sourceFile : sourceFiles) {
            BoxFileWithContent fileContent = boxSDKService.getFileContent(sourceFile);
            String localizedContent = fileContent.getContent();

            if (sourceFile.getInfo().getName().startsWith("ko-KR")) {
                logger.debug("For the Korean file, don't translate but add a corrupted text unit (invalid id) at the end");
                localizedContent = localizedContent.replaceAll("</body>",
                        "<trans-unit id=\"badid\" resname=\"TEST2\">\n"
                        + "<source xml:lang=\"en\">Content2</source>\n"
                        + "<target xml:lang=\"ko-kr\">Import Drop" + round + " -  Content2 ko-kr</target>\n"
                        + "</trans-unit>\n"
                        + "</body>");
            } else if (sourceFile.getInfo().getName().startsWith("it-IT")) {
                logger.debug("For the Italien file, don't translate but add a corrupted xml");
                localizedContent = localizedContent.replaceAll("</body>", "</bod");
            } else {
                localizedContent = XliffUtils.localizeTarget(localizedContent, "Import Drop" + round);
            }

            boxSDKService.uploadFile(boxDropExporterConfig.getLocalizedFolderId(), sourceFile.getInfo().getName(), localizedContent);
        }
    }

    @Override
    public void reviewDropFiles(Drop drop) throws DropExporterException, BoxSDKServiceException {

        logger.debug("Review files in a drop for testing");

        BoxDropExporter boxDropExporter = (BoxDropExporter) dropExporterService.recreateDropExporter(drop);
        BoxDropExporterConfig boxDropExporterConfig = boxDropExporter.getBoxDropExporterConfig();

        List<BoxFile> sourceFiles = boxSDKService.listFiles(boxDropExporterConfig.getSourceFolderId());

        for (BoxFile sourceFile : sourceFiles) {
            BoxFileWithContent fileContent = boxSDKService.getFileContent(sourceFile);
            String reviewedContent = fileContent.getContent();

            reviewedContent = XliffUtils.replaceTargetState(reviewedContent, XliffState.SIGNED_OFF.toString());

            boxSDKService.uploadFile(boxDropExporterConfig.getLocalizedFolderId(), sourceFile.getInfo().getName(), reviewedContent);
        }
    }

    @Override
    public void checkImportedFilesContent(Drop drop, int round) throws BoxSDKServiceException, DropExporterException {

        logger.debug("Check imported files contains text unit variant ids");

        BoxDropExporter boxDropExporter = (BoxDropExporter) dropExporterService.recreateDropExporter(drop);
        BoxDropExporterConfig boxDropExporterConfig = boxDropExporter.getBoxDropExporterConfig();

        List<BoxFile> importedFiles = boxSDKService.listFiles(boxDropExporterConfig.getImportedFolderId());

        for (BoxFile importedFile : importedFiles) {
            BoxFileWithContent fileWithContent = boxSDKService.getFileContent(importedFile);
            checkImportedFilesContent(importedFile.getInfo().getName(), fileWithContent.getContent(), round);
        }
    }

}
