package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.boxsdk.BoxSDKService;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.service.drop.DropRepository;
import com.box.l10n.mojito.service.drop.DropService;
import com.box.l10n.mojito.service.drop.exporter.DropExporterException;
import com.box.l10n.mojito.service.drop.exporter.DropExporterService;
import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporter;
import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporterConfig;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMImportService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.test.XliffUtils;
import com.google.common.io.Files;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureRule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_LOCALIZED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_SOURCE_FILES_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author jaurambault
 */
public class DropImportCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropImportCommandTest.class);

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
    private RepositoryClient repositoryClient;

    @Test
    public void dropImport() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        Asset asset2 = assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
        importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
        importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

        RepositoryStatusChecker repositoryStatusChecker = new RepositoryStatusChecker();
        waitForCondition("wait for repository stats to show forTranslationCount > 0 before exporting a drop",
                () -> repositoryStatusChecker.hasStringsForTranslationsForExportableLocales(
                        repositoryClient.getRepositoryById(repository.getId())
                ));

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        final Long dropId = getLastDropIdFromOutput(outputCapture);

        logger.debug("Mocking the console input for drop id: {}", dropId);
        Console mockConsole = mock(Console.class);
        when(mockConsole.readLine(Long.class)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                return getAvailableDropNumberForDropIdFromOutput(dropId);
            }
        });

        L10nJCommander l10nJCommander = getL10nJCommander();

        DropImportCommand dropImportCommand = l10nJCommander.getCommand(DropImportCommand.class);

        dropImportCommand.console = mockConsole;

        int numberOfFrenchTranslationsBefore = getNumberOfFrenchTranslations(repository);

        localizeDropFiles(dropRepository.findById(dropId).orElse(null));

        l10nJCommander.run(new String[]{"drop-import", "-r", repository.getName(), "--number-drop-fetched", "1000"});

        int numberOfFrenchTranslationsAfter = getNumberOfFrenchTranslations(repository);

        assertEquals("2 new french translations must be added", numberOfFrenchTranslationsBefore + 2, numberOfFrenchTranslationsAfter);

        getL10nJCommander().run("tm-export", "-r", repository.getName(),
                "-t", targetTestDir.getAbsolutePath(),
                "--target-basename", "fortest");

        modifyFilesInTargetTestDirectory(XliffUtils.replaceCreatedDateFunction());
        checkExpectedGeneratedResources();
    }

    @Test
    public void importFetched() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        Asset asset2 = assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
        importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
        importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

        RepositoryStatusChecker repositoryStatusChecker = new RepositoryStatusChecker();
        waitForCondition("wait for repository stats to show forTranslationCount > 0 before exporting a drop",
                () -> repositoryStatusChecker.hasStringsForTranslationsForExportableLocales(
                        repositoryClient.getRepositoryById(repository.getId())
                ));

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        final Long dropId = getLastDropIdFromOutput(outputCapture);

        logger.debug("Mocking the console input");
        Console mockConsole = mock(Console.class);
        verify(mockConsole, never()).readLine(Long.class);

        L10nJCommander l10nJCommander = getL10nJCommander();

        DropImportCommand dropImportCommand = l10nJCommander.getCommand(DropImportCommand.class);

        dropImportCommand.console = mockConsole;

        int numberOfFrenchTranslationsBefore = getNumberOfFrenchTranslations(repository);

        localizeDropFiles(dropRepository.findById(dropId).orElse(null));

        l10nJCommander.run(new String[]{"drop-import", "-r", repository.getName(), "--import-fetched"});

        int numberOfFrenchTranslationsAfter = getNumberOfFrenchTranslations(repository);

        assertEquals("2 new french translations must be added", numberOfFrenchTranslationsBefore + 2, numberOfFrenchTranslationsAfter);

        getL10nJCommander().run("tm-export", "-r", repository.getName(),
                "-t", targetTestDir.getAbsolutePath(),
                "--target-basename", "fortest");

        modifyFilesInTargetTestDirectory(XliffUtils.replaceCreatedDateFunction());
        checkExpectedGeneratedResources();
    }

    private int getNumberOfFrenchTranslations(Repository repository) {
        return textUnitCurrentVariantRepository.findByTmTextUnit_Tm_IdAndLocale_Id(repository.getTm().getId(), localeService.findByBcp47Tag("fr-FR").getId()).size();
    }

    public static Long getLastDropIdFromOutput(OutputCaptureRule outputCapture) {
        Pattern compile = Pattern.compile("Drop id: ([\\d]+)");
        Matcher matcher = compile.matcher(outputCapture.toString());
        String dropId = null;
        while (matcher.find()) {
            dropId = matcher.group(1);
        }
        return Long.valueOf(dropId);
    }

    private Long getAvailableDropNumberForDropIdFromOutput(Long dropId) {
        Pattern compile = Pattern.compile("  ([\\d]+) - id: " + dropId + ", name:");
        Matcher matcher = compile.matcher(outputCapture.toString());
        String dropNumber = null;
        while (matcher.find()) {
            dropNumber = matcher.group(1);
        }
        return Long.valueOf(dropNumber);
    }

    public void localizeDropFiles(Drop drop) throws BoxSDKServiceException, DropExporterException, IOException {

        logger.debug("Localize files in a drop");

        FileSystemDropExporter fileSystemDropExporter = (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
        FileSystemDropExporterConfig fileSystemDropExporterConfig = fileSystemDropExporter.getFileSystemDropExporterConfig();

        File[] sourceFiles = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_SOURCE_FILES_NAME).toFile().listFiles();

        for (File sourceFile : sourceFiles) {
            String localizedContent = Files.toString(sourceFile, StandardCharsets.UTF_8);
            localizedContent = XliffUtils.localizeTarget(localizedContent, "Import Drop");
            localizedContent = XliffUtils.replaceTargetState(localizedContent, "translated");

            Path localizedFolderPath = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_LOCALIZED_FILES_NAME, sourceFile.getName());
            Files.write(localizedContent, localizedFolderPath.toFile(), StandardCharsets.UTF_8);
        }
    }

}
