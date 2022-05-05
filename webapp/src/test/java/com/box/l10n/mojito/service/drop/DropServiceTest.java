package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant.Status;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.okapi.XliffState;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.drop.exporter.DropExporterException;
import com.box.l10n.mojito.service.drop.exporter.DropExporterService;
import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporter;
import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporterConfig;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskExecutionException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.service.translationkit.TranslationKitRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.XliffUtils;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_IMPORTED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_LOCALIZED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_SOURCE_FILES_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/** 
 * @author jaurambault
 */
public class DropServiceTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(DropServiceTest.class);

    @Autowired
    DropService dropService;

    @Autowired
    DropRepository dropRepository;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    DropExporterService dropExporterService;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    LocaleService localeService;

    @Autowired
    TranslationKitRepository translationKitRepository;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    TMService tmService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testCreateDrop() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Drop createDrop = dropService.createDrop(repository);
        assertEquals(createDrop.getRepository().getId(), repository.getId());
        assertNotNull(createDrop.getCreatedByUser());
    }

    @Test
    public void forNotTranslated() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        Repository repository = tmTestData.repository;

        List<String> bcp47Tags = new ArrayList<>();
        bcp47Tags.add("fr-FR");
        bcp47Tags.add("ko-KR");
        bcp47Tags.add("ja-JP");

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setBcp47Tags(bcp47Tags);

        logger.debug("Check inital number of untranslated units");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 4);

        logger.debug("Create an initial drop for the repository");
        PollableFuture<Drop> startExportProcess = dropService.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        PollableTask pollableTask = startExportProcess.getPollableTask();

        logger.debug("Wait for export to finish");
        pollableTaskService.waitForPollableTask(pollableTask.getId(), 600000L);

        logger.debug("Drop export finished, localize files in Box without updating the state");
        Drop drop = startExportProcess.get();
        localizeDropFiles(drop, 1, "new", false);

        logger.debug("Import drop");
        PollableFuture<Void> startImportDrop = dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        logger.debug("Wait for import to finish");
        pollableTaskService.waitForPollableTask(startImportDrop.getPollableTask().getId(), 60000L);

        logger.debug("Check everything is still untranslated");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 4);
        checkTranslationKitImported(drop.getId(), false);

        logger.debug("Force complete");
        forceCompleteDrop(drop.getId());
    }

    @Transactional
    public void forceCompleteDrop(Long dropId) {
        Drop drop = dropRepository.findById(dropId).orElse(null);
        assertTrue(drop.getPartiallyImported());
        dropService.completeDrop(drop);
        assertFalse(drop.getPartiallyImported());
    }

    @Transactional
    public void checkTranslationKitImported(Long dropId, boolean expected) {
        Drop drop = dropRepository.findById(dropId).orElse(null);
        if (expected) {
            assertFalse(drop.getPartiallyImported());
        } else {
            assertTrue(drop.getPartiallyImported());
        }
        List<TranslationKit> translationKits = translationKitRepository.findByDropId(dropId);
        for (TranslationKit translationKit : translationKits) {
            assertEquals(expected, translationKit.getImported());
        }
    }

    @Test
    public void forTranslation() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        Repository repository = tmTestData.repository;

        List<String> bcp47Tags = new ArrayList<>();
        bcp47Tags.add("fr-FR");
        bcp47Tags.add("ko-KR");
        bcp47Tags.add("ja-JP");

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setBcp47Tags(bcp47Tags);

        logger.debug("Check inital number of untranslated units");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 4);

        logger.debug("Create an initial drop for the repository");
        PollableFuture<Drop> startExportProcess = dropService.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        PollableTask pollableTask = startExportProcess.getPollableTask();

        logger.debug("Wait for export to finish");
        pollableTaskService.waitForPollableTask(pollableTask.getId(), 600000L);

        logger.debug("Drop export finished, localize files in Box");
        Drop drop = startExportProcess.get();
        localizeDropFiles(drop, 1);

        logger.debug("Import drop");
        PollableFuture<Void> startImportDrop = dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        logger.debug("Wait for import to finish");
        pollableTaskService.waitForPollableTask(startImportDrop.getPollableTask().getId(), 60000L);

        logger.debug("Check everything is now translated");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 0);
        checkImportedFilesContent(drop, 1);
        checkTranslationKitStatistics(drop);

        logger.debug("Perform a third import drop with changes (must be able to re-import as many time as wanted)");
        localizeDropFiles(drop, 2);
        PollableFuture<Void> startImportDrop3 = dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        logger.debug("Wait for import to finish");
        pollableTaskService.waitForPollableTask(startImportDrop3.getPollableTask().getId(), 60000L);

        logger.debug("Check everything is now translated");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 0);

        checkImportedFilesContent(drop, 2);
        checkTranslationKitStatistics(drop);
    }

    @Test
    public void forTranslationWithTranslationAddedAfterExport() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        Repository repository = tmTestData.repository;

        List<String> bcp47Tags = new ArrayList<>();
        bcp47Tags.add("fr-FR");
        bcp47Tags.add("ko-KR");
        bcp47Tags.add("ja-JP");

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setBcp47Tags(bcp47Tags);

        logger.debug("Check inital number of untranslated units");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 4);

        logger.debug("Create an initial drop for the repository");
        PollableFuture<Drop> startExportProcess = dropService.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        PollableTask pollableTask = startExportProcess.getPollableTask();

        logger.debug("Wait for export to finish");
        pollableTaskService.waitForPollableTask(pollableTask.getId(), 600000L);

        logger.debug("Drop export finished, localize files in Box");
        Drop drop = startExportProcess.get();
        localizeDropFiles(drop, 1);

        logger.debug("Translate one of the entry, will check later that this string wasn't overriden");

        TMTextUnitVariant translationAddedAfterTheImport = tmService.addCurrentTMTextUnitVariant(
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId(),
                tmTestData.frFR.getId(),
                "string added while the drop is translated");

        logger.debug("Import drop");
        PollableFuture<Void> startImportDrop = dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        logger.debug("Wait for import to finish");
        pollableTaskService.waitForPollableTask(startImportDrop.getPollableTask().getId(), 60000L);

        logger.debug("Check everything is now translated");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 0);

        checkImportedFilesContent(drop, 1);

        checkTranslationKitStatistics(drop);

        logger.debug("Perform a second import drop (must be able to re-import as many time as wanted)");
        PollableFuture<Void> startImportDrop2 = dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        logger.debug("Wait for import to finish");
        pollableTaskService.waitForPollableTask(startImportDrop2.getPollableTask().getId(), 60000L);

        logger.debug("Check that the current translation is the one that was added after the export and before the import and not coming from the TK");
        TMTextUnitCurrentVariant currentTranslation = tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
                tmTestData.frFR.getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId());

        assertEquals("The translation that has been added between the export and import must be kept", translationAddedAfterTheImport.getId(), currentTranslation.getTmTextUnitVariant().getId());
    }

    @Test
    public void forReview() throws Exception {

        Repository repository = createDataForReview();

        List<String> bcp47Tags = new ArrayList<>();
        bcp47Tags.add("fr-FR");
        bcp47Tags.add("ko-KR");
        bcp47Tags.add("ja-JP");

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setBcp47Tags(bcp47Tags);
        exportDropConfig.setType(TranslationKit.Type.REVIEW);

        logger.debug("Check inital number of needs review");
        checkNumberOfNeedsReviewTextUnit(repository, bcp47Tags, 1);

        logger.debug("Create an initial drop for the repository");
        PollableFuture<Drop> startExportProcess = dropService.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        PollableTask pollableTask = startExportProcess.getPollableTask();

        logger.debug("Wait for export to finish");
        pollableTaskService.waitForPollableTask(pollableTask.getId(), 600000L);

        logger.debug("Drop export finished, localize files in Box");
        Drop drop = startExportProcess.get();

        reviewDropFiles(drop);

        logger.debug("Import drop");
        PollableFuture<Void> startImportDrop = dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        logger.debug("Wait for import to finish");
        pollableTaskService.waitForPollableTask(startImportDrop.getPollableTask().getId(), 60000L);

        logger.debug("Check everything is now translated");
        checkNumberOfNeedsReviewTextUnit(repository, bcp47Tags, 0);

        checkImportedFilesForReviewContent(drop);
    }

    @Transactional
    Repository createDataForReview() {
        TMTestData tmTestData = new TMTestData(testIdWatcher);
        Repository repository = tmTestData.repository;
        logger.debug("Mark on translated string as need review");
        tmService.addTMTextUnitCurrentVariant(
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
                null,
                TMTextUnitVariant.Status.REVIEW_NEEDED);
        return repository;
    }

    @Test
    public void allWithSevereError() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        Repository repository = tmTestData.repository;

        List<String> bcp47Tags = new ArrayList<>();
        bcp47Tags.add("fr-FR");

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setBcp47Tags(bcp47Tags);

        logger.debug("Create an initial drop for the repository");
        PollableFuture<Drop> startExportProcess = dropService.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        PollableTask pollableTask = startExportProcess.getPollableTask();

        logger.debug("Wait for export to finish");
        pollableTaskService.waitForPollableTask(pollableTask.getId(), 600000L);

        logger.debug("Drop export finished, localize files in Box");
        Drop drop = startExportProcess.get();
        localizeDropFiles(drop, 1, "translated", true); // introduce syntax error!

        logger.debug("Import drop");
        PollableFuture<Void> startImportDrop = dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        logger.debug("Wait for import to finish");
        try {
            pollableTaskService.waitForPollableTask(startImportDrop.getPollableTask().getId(), 60000L);
            fail();
        } catch (PollableTaskException pte) {
            PollableTask importPollableTask = pollableTaskService.getPollableTask(startImportDrop.getPollableTask().getId());

            PollableTask next = importPollableTask.getSubTasks().iterator().next();
            assertTrue(next.getErrorMessage().contains("Unexpected close tag"));
        }

    }

    @Test
    public void forNoEmptyXliffs() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        Repository repository = tmTestData.repository;

        // make French be fully translated and Japanese not
        tmTestData.addCurrentTMTextUnitVariant1FrFR.setStatus(Status.APPROVED);
        tmService.addCurrentTMTextUnitVariant(tmTestData.addTMTextUnit2.getId(), localeService.findByBcp47Tag("fr-FR").getId(), "French stuff here.");

        List<String> bcp47Tags = new ArrayList<>();
        bcp47Tags.add("fr-FR");
        bcp47Tags.add("ja-JP");

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setBcp47Tags(bcp47Tags);

        logger.debug("Check inital number of untranslated units");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 2);

        logger.debug("Create an initial drop for the repository");
        PollableFuture<Drop> startExportProcess = dropService.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        PollableTask pollableTask = startExportProcess.getPollableTask();

        logger.debug("Wait for export to finish");
        pollableTaskService.waitForPollableTask(pollableTask.getId(), 600000L);

        logger.debug("Drop export finished, localize files in Box without updating the state");
        Drop drop = startExportProcess.get();

        // Make sure no French xliff was generated
        FileSystemDropExporter fileSystemDropExporter = (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
        FileSystemDropExporterConfig fileSystemDropExporterConfig = fileSystemDropExporter.getFileSystemDropExporterConfig();

        File frFR = new File(Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_SOURCE_FILES_NAME, "fr-FR.xliff").toString());

        assertFalse(frFR.exists());
    }

    public void checkNumberOfUntranslatedTextUnit(Repository repository, List<String> bcp47Tags, int expectedNumberOfUnstranslated) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.UNTRANSLATED);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setLocaleTags(bcp47Tags);

        List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
        assertEquals(expectedNumberOfUnstranslated, search.size());
    }

    public void checkNumberOfNeedsReviewTextUnit(Repository repository, List<String> bcp47Tags, int expectedNumberOfUnstranslated) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.REVIEW_NEEDED);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setLocaleTags(bcp47Tags);

        List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
        assertEquals(expectedNumberOfUnstranslated, search.size());
    }

    public void localizeDropFiles(Drop drop, int round) throws BoxSDKServiceException, DropExporterException, IOException {
        localizeDropFiles(drop, round, "translated", false);
    }

    public void localizeDropFiles(Drop drop, int round, String xliffState, boolean introduceSyntaxError) throws BoxSDKServiceException, DropExporterException, IOException {

        logger.debug("Localize files in a drop for testing");

        FileSystemDropExporter fileSystemDropExporter = (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
        FileSystemDropExporterConfig fileSystemDropExporterConfig = fileSystemDropExporter.getFileSystemDropExporterConfig();

        File[] sourceFiles = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_SOURCE_FILES_NAME).toFile().listFiles();

        for (File sourceFile : sourceFiles) {

            String localizedContent = Files.toString(sourceFile, StandardCharsets.UTF_8);

            if (sourceFile.getName().startsWith("ko-KR")) {
                logger.debug("For the Korean file, don't translate but add a corrupted text unit (invalid id) at the end");
                localizedContent = localizedContent.replaceAll("</body>",
                        "<trans-unit id=\"badid\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                        + "<source xml:lang=\"en\">Content2</source>\n"
                        + "<target xml:lang=\"ko-KR\" state=\"new\">Import Drop" + round + " - Content2 ko-KR</target>\n"
                        + "</trans-unit>\n"
                        + "</body>");
            } else  {
                localizedContent = XliffUtils.localizeTarget(localizedContent, "Import Drop" + round);
            }

            if (introduceSyntaxError) {
                logger.debug("Creating a corrupted xml file to test import errors.");
                localizedContent = localizedContent.replaceAll("</body>", "</bod");
            }

            localizedContent = XliffUtils.replaceTargetState(localizedContent, xliffState);

            //TODO(P1) this logic is being duplicated everywhere maybe it should go back into the config or service.
            Path localizedFolderPath = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_LOCALIZED_FILES_NAME, sourceFile.getName());
            Files.write(localizedContent, localizedFolderPath.toFile(), StandardCharsets.UTF_8);
        }
    }

    public void reviewDropFiles(Drop drop) throws DropExporterException, BoxSDKServiceException, IOException {

        logger.debug("Review files in a drop for testing");

        FileSystemDropExporter fileSystemDropExporter = (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
        FileSystemDropExporterConfig fileSystemDropExporterConfig = fileSystemDropExporter.getFileSystemDropExporterConfig();

        File[] sourceFiles = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_SOURCE_FILES_NAME).toFile().listFiles();

        for (File sourceFile : sourceFiles) {

            String reviewedContent = Files.toString(sourceFile, StandardCharsets.UTF_8);
            reviewedContent = XliffUtils.replaceTargetState(reviewedContent, XliffState.SIGNED_OFF.toString());

            //TODO(P1) this logic is being duplicated everywhere maybe it should go back into the config or service.
            Path localizedFolderPath = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_LOCALIZED_FILES_NAME, sourceFile.getName());
            Files.write(reviewedContent, localizedFolderPath.toFile(), StandardCharsets.UTF_8);
        }
    }

    public void checkImportedFilesContent(Drop drop, int round) throws BoxSDKServiceException, DropExporterException, IOException {

        logger.debug("Check imported files contains text unit variant ids");

        FileSystemDropExporter fileSystemDropExporter = (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
        FileSystemDropExporterConfig fileSystemDropExporterConfig = fileSystemDropExporter.getFileSystemDropExporterConfig();

        File[] importedFiles = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_IMPORTED_FILES_NAME).toFile().listFiles();

        for (File importedFile : importedFiles) {

            if (!importedFile.getName().endsWith("xliff")) {
                continue;
            }

            String importedContent = Files.toString(importedFile, StandardCharsets.UTF_8);
            checkImportedFilesContent(importedFile.getName(), importedContent, round);
        }
    }

    public void checkImportedFilesContent(String filename, String importedContent, int round) {
        if (filename.startsWith("fr-FR")) {

            logger.debug(importedContent);

            String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
            logger.debug(xliffWithoutIds);

            assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                    + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                    + "<body>\n"
                    + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                    + "<source xml:lang=\"en\">Content2</source>\n"
                    + "<target xml:lang=\"fr-FR\" state=\"needs-review-translation\">Import Drop" + round + " - Content2 fr-FR</target>\n"
                    + "<note>Comment2</note>\n"
                    + "<note annotates=\"target\" from=\"automation\">OK\n"
                    + "[INFO] tuv id: replaced-id</note>\n"
                    + "</trans-unit>\n"
                    + "</body>\n"
                    + "</file>\n"
                    + "</xliff>\n", xliffWithoutIds);
            
        } else if (filename.startsWith("ko-KR")) {
            
            logger.debug(importedContent);
            
            String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
            logger.debug(xliffWithoutIds);
            
            if (round == 1) {
                assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                        + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"ko-KR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                        + "<body>\n"
                        + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                        + "<source xml:lang=\"en\">Content2</source>\n"
                        + "<target xml:lang=\"ko-KR\" state=\"needs-review-translation\">Content2</target>\n"
                        + "<note>Comment2</note>\n"
                        + "<note annotates=\"target\" from=\"automation\">NEEDS REVIEW\n"
                        + "[INFO] tuv id: replaced-id\n"
                        + "[WARNING] Translation is the same as the source.</note>\n"
                        + "</trans-unit>\n"
                        + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                        + "<source xml:lang=\"en\">Content2</source>\n"
                        + "<target xml:lang=\"ko-KR\" state=\"needs-translation\">Import Drop" + round + " - Content2 ko-KR</target>\n"
                        + "<note annotates=\"target\" from=\"automation\">MUST REVIEW\n"
                        + "[ERROR] Text unit for id: badid, Skipping it...</note>\n"
                        + "</trans-unit>\n"
                        + "</body>\n"
                        + "</file>\n"
                        + "</xliff>\n", xliffWithoutIds);
            } else {
                assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                        + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"ko-KR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                        + "<body>\n"
                        + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                        + "<source xml:lang=\"en\">Content2</source>\n"
                        + "<target xml:lang=\"ko-KR\" state=\"needs-review-translation\">Content2</target>\n"
                        + "<note>Comment2</note>\n"
                        + "<note annotates=\"target\" from=\"automation\">NEEDS REVIEW\n"
                        + "[INFO] tuv id: replaced-id\n"
                        + "[WARNING] Translation is the same as the source.\n"
                        + "[WARNING] Translation is the same as the source.</note>\n"
                        + "</trans-unit>\n"
                        + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                        + "<source xml:lang=\"en\">Content2</source>\n"
                        + "<target xml:lang=\"ko-KR\" state=\"needs-translation\">Import Drop2 - Content2 ko-KR</target>\n"
                        + "<note annotates=\"target\" from=\"automation\">MUST REVIEW\n"
                        + "[ERROR] Text unit for id: badid, Skipping it...</note>\n"
                        + "</trans-unit>\n"
                        + "</body>\n"
                        + "</file>\n"
                        + "</xliff>\n", xliffWithoutIds);
            }

        } else {
            
            logger.debug(importedContent);
            
            String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
            logger.debug(xliffWithoutIds);
            
            assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                    + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"ja-JP\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                    + "<body>\n"
                    + "<trans-unit id=\"replaced-id\" resname=\"zuora_error_message_verify_state_province\" xml:space=\"preserve\">\n"
                    + "<source xml:lang=\"en\">Please enter a valid state, region or province</source>\n"
                    + "<target xml:lang=\"ja-JP\" state=\"needs-review-translation\">Import Drop" + round + " - Please enter a valid state, region or province ja-JP</target>\n"
                    + "<note>Comment1</note>\n"
                    + "<note annotates=\"target\" from=\"automation\">OK\n"
                    + "[INFO] tuv id: replaced-id</note>\n"
                    + "</trans-unit>\n"
                    + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                    + "<source xml:lang=\"en\">Content2</source>\n"
                    + "<target xml:lang=\"ja-JP\" state=\"needs-review-translation\">Import Drop" + round + " - Content2 ja-JP</target>\n"
                    + "<note>Comment2</note>\n"
                    + "<note annotates=\"target\" from=\"automation\">OK\n"
                    + "[INFO] tuv id: replaced-id</note>\n"
                    + "</trans-unit>\n"
                    + "</body>\n"
                    + "</file>\n"
                    + "</xliff>\n", xliffWithoutIds);
        }
    }

    public void checkImportedFilesForReviewContent(Drop drop) throws DropExporterException, BoxSDKServiceException, IOException {
        logger.debug("Check imported files contains text unit variant ids");

        FileSystemDropExporter fileSystemDropExporter = (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
        FileSystemDropExporterConfig fileSystemDropExporterConfig = fileSystemDropExporter.getFileSystemDropExporterConfig();

        File[] importedFiles = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_IMPORTED_FILES_NAME).toFile().listFiles();

        for (File importedFile : importedFiles) {

            if (!importedFile.getName().endsWith("xliff")) {
                continue;
            }

            String importedContent = Files.toString(importedFile, StandardCharsets.UTF_8);

            if (importedFile.getName().startsWith("fr-FR")) {

                logger.debug(importedContent);

                String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
                logger.debug(xliffWithoutIds);

                assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                        + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                        + "<body>\n"
                        + "<trans-unit id=\"replaced-id\" resname=\"zuora_error_message_verify_state_province\" xml:space=\"preserve\">\n"
                        + "<source xml:lang=\"en\">Please enter a valid state, region or province</source>\n"
                        + "<target xml:lang=\"fr-FR\" state=\"final\">Veuillez indiquer un état, une région ou une province valide.</target>\n"
                        + "<note>Comment1</note>\n"
                        + "<note annotates=\"target\" from=\"automation\">OK\n"
                        + "[INFO] tuv id: replaced-id</note>\n"
                        + "</trans-unit>\n"
                        + "</body>\n"
                        + "</file>\n"
                        + "</xliff>\n"
                        + "", xliffWithoutIds);
            } else if (importedFile.getName().startsWith("ko-KR")) {

                logger.debug(importedContent);

                String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
                logger.debug(xliffWithoutIds);

                assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                        + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"ko-KR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                        + "<body>\n"
                        + "</body>\n"
                        + "</file>\n"
                        + "</xliff>\n"
                        + "", xliffWithoutIds);

            } else {

                logger.debug(importedContent);

                String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
                logger.debug(xliffWithoutIds);

                assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                        + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"ja-JP\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                        + "<body>\n"
                        + "</body>\n"
                        + "</file>\n"
                        + "</xliff>\n"
                        + "", xliffWithoutIds);
            }
        }
    }

    @Transactional
    public void checkTranslationKitStatistics(Drop drop) throws BoxSDKServiceException, DropExporterException {

        logger.debug("Check statistics");
        Drop d = dropRepository.findById(drop.getId()).orElse(null);

        for (TranslationKit tk : d.getTranslationKits()) {

            assertEquals("For locale: " + tk.getLocale().getBcp47Tag(), tk.getNumTranslationKitUnits(), tk.getNumTranslatedTranslationKitUnits());
            assertNotNull(tk.getWordCount());
            assertTrue(tk.getImported());

            if (tk.getLocale().getBcp47Tag().equals("ko-KR")) {
                assertEquals("For locale: " + tk.getLocale().getBcp47Tag(), 1, tk.getNotFoundTextUnitIds().size());
                assertEquals("For locale: " + tk.getLocale().getBcp47Tag(), 1, tk.getNumSourceEqualsTarget());
                assertEquals(1, tk.getWordCount().intValue());
            } else {
                if (tk.getLocale().getBcp47Tag().equals("ja-JP")) {
                    assertEquals(9, tk.getWordCount().intValue());
                } else {
                    assertEquals(1, tk.getWordCount().intValue());
                }
                assertEquals("For locale: " + tk.getLocale().getBcp47Tag(), 0, tk.getNotFoundTextUnitIds().size());
                assertEquals("For locale: " + tk.getLocale().getBcp47Tag(), 0, tk.getNumSourceEqualsTarget());
            }
        }
    }

    @Test
    public void testGetDropFolderName() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 0, 1, 0, 0, 0);

        assertEquals("Week 48 (Tuesday) - 01 January 2013 - 00.00.00", dropService.getDropName(cal.getTime()));

        cal.set(Calendar.WEEK_OF_YEAR, 6);
        assertEquals("Week 1 (Tuesday) - 05 February 2013 - 00.00.00", dropService.getDropName(cal.getTime()));
    }

    @Test
    public void testCancelDrop() throws DropExporterException, InterruptedException, ExecutionException, CancelDropException {
        TMTestData tmTestData = new TMTestData(testIdWatcher);

        Repository repository = tmTestData.repository;

        List<String> bcp47Tags = new ArrayList<>();
        bcp47Tags.add("fr-FR");

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setBcp47Tags(bcp47Tags);

        logger.debug("Check inital number of untranslated units");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 1);

        logger.debug("Create an initial drop for the repository");
        PollableFuture<Drop> startExportProcess = dropService.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        PollableTask pollableTask = startExportProcess.getPollableTask();

        logger.debug("Wait for export to finish");
        pollableTaskService.waitForPollableTask(pollableTask.getId(), 600000L);

        logger.debug("Drop export finished, localize files in Box");
        Drop drop = startExportProcess.get();

        PollableFuture<Drop> dropPollableFuture = dropService.cancelDrop(drop.getId(), PollableTask.INJECT_CURRENT_TASK);
        PollableTask cancelDropPollableTask = dropPollableFuture.getPollableTask();

        logger.debug("Wait for cancellation to finish");
        pollableTaskService.waitForPollableTask(cancelDropPollableTask.getId(), 600000L);

        Drop canceledDrop = dropPollableFuture.get();
        Assert.assertTrue("Drop should be canceled", canceledDrop.getCanceled());
    }

    @Test(expected = PollableTaskExecutionException.class)
    @Ignore("flaky test")
    public void testCancelDropException() throws DropExporterException, ExecutionException, InterruptedException, CancelDropException {

        DropService dropServiceSpy = spy(dropService);
        doReturn(true).when(dropServiceSpy).isDropBeingProcessed(any(Drop.class));

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        Repository repository = tmTestData.repository;

        List<String> bcp47Tags = new ArrayList<>();
        bcp47Tags.add("fr-FR");

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setBcp47Tags(bcp47Tags);

        logger.debug("Check initial number of untranslated units");
        checkNumberOfUntranslatedTextUnit(repository, bcp47Tags, 1);

        logger.debug("Create an initial drop for the repository");
        PollableFuture<Drop> startExportProcess = dropServiceSpy.startDropExportProcess(exportDropConfig, PollableTask.INJECT_CURRENT_TASK);

        Drop drop = startExportProcess.get();

        PollableFuture<Drop> dropPollableFuture = dropServiceSpy.cancelDrop(drop.getId(), PollableTask.INJECT_CURRENT_TASK);
        PollableTask cancelDropPollableTask = dropPollableFuture.getPollableTask();

        logger.debug("Wait for cancellation to finish");
        pollableTaskService.waitForPollableTask(cancelDropPollableTask.getId(), 600000L);
    }
}
