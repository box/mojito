package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.boxsdk.BoxSDKService;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.service.drop.DropRepository;
import com.box.l10n.mojito.service.drop.DropService;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_SOURCE_FILES_NAME;
import com.box.l10n.mojito.service.drop.exporter.DropExporterException;
import com.box.l10n.mojito.service.drop.exporter.DropExporterService;
import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporter;
import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporterConfig;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMImportService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.test.XliffUtils;
import java.nio.charset.StandardCharsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author jaurambault
 */
public class DropXliffImportCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropXliffImportCommandTest.class);

    @Autowired
    TMImportService tmImport;

    @Autowired
    AssetClient assetClient;

    @Autowired
    TMTextUnitCurrentVariantRepository textUnitCurrentVariantRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    DropService dropService;

    @Autowired
    DropExporterService dropExporterService;

    @Autowired
    BoxSDKService boxSDKService;

    @Autowired
    DropRepository dropRepository;

    @Autowired
    DropImportCommand dropImportCommand;

    @Test
    public void dropXliffImport() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        Asset asset2 = assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
        importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
        importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        final Long dropId = DropImportCommandTest.getLastDropIdFromOutput(outputCapture);

        localizeDropFilesAndCopyLocally(dropRepository.findOne(dropId));

        getL10nJCommander().run("drop-xliff-import", "-r", repository.getName(),
                "--import-by-md5",
                "-s", getTargetTestDir("localized").getAbsolutePath(),
                "-t", getTargetTestDir("imported").getAbsolutePath());

        modifyFilesInTargetTestDirectory(XliffUtils.replaceXliffVariableContentFunction());

        removeDateFromXliffNames();

        checkExpectedGeneratedResources();
    }

    void removeDateFromXliffNames() throws IOException {
        Collection<File> files = FileUtils.listFiles(getTargetTestDir(), null, true);
        for (File file : files) {
            String newName = file.getName().replaceAll("_.*.xliff", "_.xliff");
            Files.move(file, file.toPath().resolveSibling(newName).toFile());
        }
    }

    public void localizeDropFilesAndCopyLocally(Drop drop) throws BoxSDKServiceException, DropExporterException, IOException {

        logger.debug("Localize files in a drop and copy them locally");

        FileSystemDropExporter fileSystemDropExporter = (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
        FileSystemDropExporterConfig fileSystemDropExporterConfig = fileSystemDropExporter.getFileSystemDropExporterConfig();

        File[] sourceFiles = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_SOURCE_FILES_NAME).toFile().listFiles();

        for (File sourceFile : sourceFiles) {
            String localizedContent = Files.toString(sourceFile, StandardCharsets.UTF_8);
            localizedContent = XliffUtils.localizeTarget(localizedContent, "Import Xliff Drop");
            writeToFileInDirectory(getTargetTestDir("localized"), sourceFile.getName(), localizedContent);
        }

    }

}
