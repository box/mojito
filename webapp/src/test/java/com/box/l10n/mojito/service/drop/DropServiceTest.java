package com.box.l10n.mojito.service.drop;

import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.entity.TMTextUnitVariant.Status;
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
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.translationkit.TranslationKitRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.XliffUtils;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault
 */
public class DropServiceTest extends ServiceTestBase {

  static Logger logger = LoggerFactory.getLogger(DropServiceTest.class);

  @Autowired DropService dropService;

  @Autowired DropRepository dropRepository;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryService repositoryService;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired DropExporterService dropExporterService;

  @Autowired LocaleService localeService;

  @Autowired TranslationKitRepository translationKitRepository;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired TMService tmService;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testCreateDrop() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    Drop createDrop = dropService.createDrop(repository);
    assertEquals(createDrop.getRepository().getId(), repository.getId());
    assertNotNull(createDrop.getCreatedByUser());
  }

  public <T> PollableFuture<T> awaitPollableProcess(PollableFuture<T> process)
      throws InterruptedException, PollableTaskException {
    String processName = process.getClass().getName();
    logger.debug("Starting process [{}]", processName);
    PollableTask pollableTask = process.getPollableTask();
    Long taskId = pollableTask.getId();
    logger.debug("Wait for process [{}] task [{}] to finish", processName, taskId);
    pollableTaskService.waitForPollableTask(taskId, 600000L);
    logger.debug("Process [{}] finished", processName);
    return process;
  }

  @Test
  public void forNotTranslated() throws Exception {
    List<String> locales = List.of("fr-FR", "ko-KR", "ja-JP");

    DropTestData dropTestData = DropTestData.createWithDefaultData(testIdWatcher);

    ExportDropConfig exportDropConfig = dropTestData.getExportDropConfig(locales);

    logger.debug("Check inital number of untranslated units");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 4);

    logger.debug("Create an initial drop for the repository");
    Drop drop =
        awaitPollableProcess(
                dropService.startDropExportProcess(
                    exportDropConfig, PollableTask.INJECT_CURRENT_TASK))
            .get();

    logger.debug("Drop export finished, localize files in Box without updating the state");
    localizeDropFiles(drop, 1, "new", false);

    logger.debug("Import drop");
    awaitPollableProcess(
        dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK));

    logger.debug("Check everything is still untranslated");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 4);
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
    List<String> locales = List.of("fr-FR", "ko-KR", "ja-JP");

    DropTestData dropTestData = DropTestData.createWithDefaultData(testIdWatcher);

    ExportDropConfig exportDropConfig = dropTestData.getExportDropConfig(locales);

    logger.debug("Check inital number of untranslated units");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 4);

    logger.debug("Create an initial drop for the repository");
    Drop drop =
        awaitPollableProcess(
                dropService.startDropExportProcess(
                    exportDropConfig, PollableTask.INJECT_CURRENT_TASK))
            .get();

    logger.debug("Drop export finished, localize files in Box");
    localizeDropFiles(drop, 1);

    logger.debug("Import drop");
    awaitPollableProcess(
        dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK));

    logger.debug("Check everything is now translated");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 0);
    checkImportedFilesContent(drop, 1);
    checkTranslationKitStatistics(drop);

    logger.debug(
        "Perform a third import drop with changes (must be able to re-import as many time as wanted)");
    localizeDropFiles(drop, 2);
    awaitPollableProcess(
        dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK));

    logger.debug("Check everything is now translated");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 0);

    checkImportedFilesContent(drop, 2);
    checkTranslationKitStatistics(drop);
  }

  @Test
  public void forTranslationWithTranslationAddedAfterExport() throws Exception {
    List<String> locales = List.of("fr-FR", "ko-KR", "ja-JP");

    DropTestData dropTestData = DropTestData.createWithDefaultData(testIdWatcher);

    ExportDropConfig exportDropConfig = dropTestData.getExportDropConfig(locales);

    logger.debug("Check inital number of untranslated units");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 4);

    logger.debug("Create an initial drop for the repository");
    Drop drop =
        awaitPollableProcess(
                dropService.startDropExportProcess(
                    exportDropConfig, PollableTask.INJECT_CURRENT_TASK))
            .get();

    logger.debug("Drop export finished, localize files in Box");
    localizeDropFiles(drop, 1);

    logger.debug("Translate one of the entry, will check later that this string wasn't overriden");
    Long tmTextUnitId =
        dropTestData.tmTextUnits.get("zuora_error_message_verify_state_province").getId();
    Long localeId = dropTestData.findLocaleForTag("fr-FR").getId();

    TMTextUnitVariant translationAddedAfterTheImport =
        tmService.addCurrentTMTextUnitVariant(
            tmTextUnitId, localeId, "string added while the drop is translated");

    logger.debug("Import drop");
    awaitPollableProcess(
        dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK));

    logger.debug("Check everything is now translated");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 0);

    checkImportedFilesContent(drop, 1);

    checkTranslationKitStatistics(drop);

    logger.debug("Perform a second import drop (must be able to re-import as many time as wanted)");
    awaitPollableProcess(
        dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK));

    logger.debug(
        "Check that the current translation is the one that was added after the export and before the import and not coming from the TK");
    TMTextUnitCurrentVariant currentTranslation =
        tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(localeId, tmTextUnitId);

    assertEquals(
        "The translation that has been added between the export and import must be kept",
        translationAddedAfterTheImport.getId(),
        currentTranslation.getTmTextUnitVariant().getId());
  }

  @Test
  public void forReview() throws Exception {
    List<String> locales = List.of("fr-FR", "ko-KR", "ja-JP");

    DropTestData dropTestData = DropTestData.createWithDefaultData(testIdWatcher);

    logger.debug("Mark on translated string as need review");
    Long tmTextUnitId =
        dropTestData.tmTextUnits.get("zuora_error_message_verify_state_province").getId();
    Locale locale = dropTestData.findLocaleForTag("fr-FR");
    String currentTranslation =
        dropTestData
            .addCurrentTMTextUnitVariants
            .get(locale)
            .get("zuora_error_message_verify_state_province")
            .getContent();
    tmService.addTMTextUnitCurrentVariant(
        tmTextUnitId,
        locale.getId(),
        currentTranslation,
        null,
        TMTextUnitVariant.Status.REVIEW_NEEDED);

    ExportDropConfig exportDropConfig = dropTestData.getExportDropConfig(locales);
    exportDropConfig.setType(TranslationKit.Type.REVIEW);

    logger.debug("Check inital number of needs review");
    checkNumberOfNeedsReviewTextUnit(dropTestData, locales, 1);

    logger.debug("Create an initial drop for the repository");
    Drop drop =
        awaitPollableProcess(
                dropService.startDropExportProcess(
                    exportDropConfig, PollableTask.INJECT_CURRENT_TASK))
            .get();

    logger.debug("Drop export finished, localize files in Box");

    reviewDropFiles(drop);

    logger.debug("Import drop");
    awaitPollableProcess(
        dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK));

    logger.debug("Check everything is now translated");
    checkNumberOfNeedsReviewTextUnit(dropTestData, locales, 0);

    checkImportedFilesForReviewContent(drop);
  }

  @Test
  public void allWithSevereError() throws Exception {
    List<String> locales = List.of("fr-FR");

    DropTestData dropTestData = DropTestData.createWithDefaultData(testIdWatcher);

    ExportDropConfig exportDropConfig = dropTestData.getExportDropConfig(locales);

    logger.debug("Create an initial drop for the repository");
    Drop drop =
        awaitPollableProcess(
                dropService.startDropExportProcess(
                    exportDropConfig, PollableTask.INJECT_CURRENT_TASK))
            .get();

    logger.debug("Drop export finished, localize files in Box");
    localizeDropFiles(drop, 1, "translated", true); // introduce syntax error!

    logger.debug("Import drop");
    PollableFuture<Void> startImportDrop =
        dropService.importDrop(drop.getId(), null, PollableTask.INJECT_CURRENT_TASK);

    logger.debug("Wait for import to finish");
    try {
      awaitPollableProcess(startImportDrop);
      fail();
    } catch (PollableTaskException pte) {
      PollableTask importPollableTask =
          pollableTaskService.getPollableTask(startImportDrop.getPollableTask().getId());

      PollableTask next = importPollableTask.getSubTasks().iterator().next();
      assertTrue(next.getErrorMessage().contains("Unexpected close tag"));
    }
  }

  @Test
  public void importNonExistentId() throws Exception {

    // don't create any drops
    Long nonExistentDropId = 9999L;

    logger.debug("Import drop");
    PollableFuture<Void> startImportDrop =
        dropService.importDrop(nonExistentDropId, null, PollableTask.INJECT_CURRENT_TASK);

    logger.debug("Wait for import to finish");
    try {
      awaitPollableProcess(startImportDrop);
      fail();
    } catch (PollableTaskException pte) {
      PollableTask importPollableTask =
          pollableTaskService.getPollableTask(startImportDrop.getPollableTask().getId());
      assertTrue(
          importPollableTask
              .getErrorMessage()
              .contains("Drop with ID [" + nonExistentDropId + "] does not exist"));
    }
  }

  @Test
  public void forNoEmptyXliffs() throws Exception {
    List<String> locales = List.of("fr-FR", "ja-JP");

    DropTestData dropTestData = DropTestData.createWithDefaultData(testIdWatcher);

    // make French be fully translated and Japanese not
    Locale locale = dropTestData.findLocaleForTag("fr-FR");
    dropTestData
        .addCurrentTMTextUnitVariants
        .get(locale)
        .get("zuora_error_message_verify_state_province")
        .setStatus(Status.APPROVED);
    tmService.addCurrentTMTextUnitVariant(
        dropTestData.tmTextUnits.get("TEST2").getId(), locale.getId(), "French stuff here.");

    ExportDropConfig exportDropConfig = dropTestData.getExportDropConfig(locales);

    logger.debug("Check inital number of untranslated units");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 2);

    logger.debug("Create an initial drop for the repository");
    Drop drop =
        awaitPollableProcess(
                dropService.startDropExportProcess(
                    exportDropConfig, PollableTask.INJECT_CURRENT_TASK))
            .get();

    logger.debug("Drop export finished, localize files in Box without updating the state");

    // Make sure no French xliff was generated
    assertFalse(
        getDropFiles(drop, DROP_FOLDER_SOURCE_FILES_NAME).stream()
            .anyMatch(dropFile -> dropFile.getName().equals("fr-FR.xliff")));
  }

  public List<DropFile> getDropFiles(Drop drop, String dropFolder)
      throws DropExporterException, BoxSDKServiceException {
    FileSystemDropExporter fileSystemDropExporter =
        (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
    FileSystemDropExporterConfig fileSystemDropExporterConfig =
        fileSystemDropExporter.getFileSystemDropExporterConfig();

    File folder = Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), dropFolder).toFile();
    File[] files = folder.listFiles();
    assertNotNull(files);
    return Arrays.stream(files).map(FileSystemDropFile::new).collect(Collectors.toList());
  }

  public void checkNumberOfUntranslatedTextUnit(
      DropTestData dropTestData, List<String> locales, int expectedNumberOfUnstranslated) {
    List<TextUnitDTO> search =
        dropTestData.getTextUnitsForStatus(StatusFilter.UNTRANSLATED, locales);
    assertEquals(expectedNumberOfUnstranslated, search.size());
  }

  public void checkNumberOfNeedsReviewTextUnit(
      DropTestData dropTestData, List<String> locales, int expectedNumberOfUnstranslated) {
    List<TextUnitDTO> search =
        dropTestData.getTextUnitsForStatus(StatusFilter.REVIEW_NEEDED, locales);
    assertEquals(expectedNumberOfUnstranslated, search.size());
  }

  public void localizeDropFiles(Drop drop, int round)
      throws BoxSDKServiceException, DropExporterException, IOException {
    localizeDropFiles(drop, round, "translated", false);
  }

  public void localizeDropFiles(
      Drop drop, int round, String xliffState, boolean introduceSyntaxError)
      throws BoxSDKServiceException, DropExporterException, IOException {

    logger.debug("Localize files in a drop for testing");

    for (DropFile sourceFile : getDropFiles(drop, DROP_FOLDER_SOURCE_FILES_NAME)) {
      String localizedContent = sourceFile.getContent();

      if (sourceFile.getName().startsWith("ko-KR")) {
        logger.debug(
            "For the Korean file, don't translate but add a corrupted text unit (invalid id) at the end");
        localizedContent =
            localizedContent.replaceAll(
                "</body>",
                "<trans-unit id=\"badid\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                    + "<source xml:lang=\"en\">Content2</source>\n"
                    + "<target xml:lang=\"ko-KR\" state=\"new\">Import Drop"
                    + round
                    + " - Content2 ko-KR</target>\n"
                    + "</trans-unit>\n"
                    + "</body>");
      } else {
        localizedContent = XliffUtils.localizeTarget(localizedContent, "Import Drop" + round);
      }

      if (introduceSyntaxError) {
        logger.debug("Creating a corrupted xml file to test import errors.");
        localizedContent = localizedContent.replaceAll("</body>", "</bod");
      }

      localizedContent = XliffUtils.replaceTargetState(localizedContent, xliffState);

      writeDropFile(drop, DROP_FOLDER_LOCALIZED_FILES_NAME, sourceFile.getName(), localizedContent);
    }
  }

  public void writeDropFile(Drop drop, String dropFolder, String fileName, String content)
      throws BoxSDKServiceException, IOException, DropExporterException {
    FileSystemDropExporter fileSystemDropExporter =
        (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
    FileSystemDropExporterConfig fileSystemDropExporterConfig =
        fileSystemDropExporter.getFileSystemDropExporterConfig();
    File file =
        Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), dropFolder, fileName).toFile();
    Files.write(content, file, StandardCharsets.UTF_8);
  }

  public void reviewDropFiles(Drop drop)
      throws DropExporterException, BoxSDKServiceException, IOException {

    logger.debug("Review files in a drop for testing");

    for (DropFile sourceFile : getDropFiles(drop, DROP_FOLDER_SOURCE_FILES_NAME)) {
      String reviewedContent = sourceFile.getContent();
      reviewedContent =
          XliffUtils.replaceTargetState(reviewedContent, XliffState.SIGNED_OFF.toString());

      writeDropFile(drop, DROP_FOLDER_LOCALIZED_FILES_NAME, sourceFile.getName(), reviewedContent);
    }
  }

  public void checkImportedFilesContent(Drop drop, int round)
      throws BoxSDKServiceException, DropExporterException, IOException {

    logger.debug("Check imported files contains text unit variant ids");

    for (DropFile importedFile : getDropFiles(drop, DROP_FOLDER_IMPORTED_FILES_NAME)) {

      if (!importedFile.getName().endsWith("xliff")) {
        continue;
      }

      String importedContent = importedFile.getContent();
      checkImportedFilesContent(importedFile.getName(), importedContent, round);
    }
  }

  public void checkImportedFilesContent(String filename, String importedContent, int round) {
    if (filename.startsWith("fr-FR")) {

      logger.debug(importedContent);

      String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
      logger.debug(xliffWithoutIds);

      assertEquals(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
              + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
              + "<body>\n"
              + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
              + "<source xml:lang=\"en\">Content2</source>\n"
              + "<target xml:lang=\"fr-FR\" state=\"needs-review-translation\">Import Drop"
              + round
              + " - Content2 fr-FR</target>\n"
              + "<note>Comment2</note>\n"
              + "<note annotates=\"target\" from=\"automation\">OK\n"
              + "[INFO] tuv id: replaced-id</note>\n"
              + "</trans-unit>\n"
              + "</body>\n"
              + "</file>\n"
              + "</xliff>\n",
          xliffWithoutIds);

    } else if (filename.startsWith("ko-KR")) {

      logger.debug(importedContent);

      String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
      logger.debug(xliffWithoutIds);

      if (round == 1) {
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
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
                + "<target xml:lang=\"ko-KR\" state=\"needs-translation\">Import Drop"
                + round
                + " - Content2 ko-KR</target>\n"
                + "<note annotates=\"target\" from=\"automation\">MUST REVIEW\n"
                + "[ERROR] Text unit for id: badid, Skipping it...</note>\n"
                + "</trans-unit>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n",
            xliffWithoutIds);
      } else {
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
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
                + "</xliff>\n",
            xliffWithoutIds);
      }

    } else {

      logger.debug(importedContent);

      String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
      logger.debug(xliffWithoutIds);

      assertEquals(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
              + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"ja-JP\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
              + "<body>\n"
              + "<trans-unit id=\"replaced-id\" resname=\"zuora_error_message_verify_state_province\" xml:space=\"preserve\">\n"
              + "<source xml:lang=\"en\">Please enter a valid state, region or province</source>\n"
              + "<target xml:lang=\"ja-JP\" state=\"needs-review-translation\">Import Drop"
              + round
              + " - Please enter a valid state, region or province ja-JP</target>\n"
              + "<note>Comment1</note>\n"
              + "<note annotates=\"target\" from=\"automation\">OK\n"
              + "[INFO] tuv id: replaced-id</note>\n"
              + "</trans-unit>\n"
              + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
              + "<source xml:lang=\"en\">Content2</source>\n"
              + "<target xml:lang=\"ja-JP\" state=\"needs-review-translation\">Import Drop"
              + round
              + " - Content2 ja-JP</target>\n"
              + "<note>Comment2</note>\n"
              + "<note annotates=\"target\" from=\"automation\">OK\n"
              + "[INFO] tuv id: replaced-id</note>\n"
              + "</trans-unit>\n"
              + "</body>\n"
              + "</file>\n"
              + "</xliff>\n",
          xliffWithoutIds);
    }
  }

  public void checkImportedFilesForReviewContent(Drop drop)
      throws DropExporterException, BoxSDKServiceException, IOException {
    logger.debug("Check imported files contains text unit variant ids");

    for (DropFile importedFile : getDropFiles(drop, DROP_FOLDER_IMPORTED_FILES_NAME)) {

      if (!importedFile.getName().endsWith("xliff")) {
        continue;
      }

      String importedContent = importedFile.getContent();

      if (importedFile.getName().startsWith("fr-FR")) {

        logger.debug(importedContent);

        String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
        logger.debug(xliffWithoutIds);

        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
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
                + "",
            xliffWithoutIds);
      } else if (importedFile.getName().startsWith("ko-KR")) {

        logger.debug(importedContent);

        String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
        logger.debug(xliffWithoutIds);

        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"ko-KR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n"
                + "",
            xliffWithoutIds);

      } else {

        logger.debug(importedContent);

        String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(importedContent);
        logger.debug(xliffWithoutIds);

        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"ja-JP\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n"
                + "",
            xliffWithoutIds);
      }
    }
  }

  @Transactional
  public void checkTranslationKitStatistics(Drop drop)
      throws BoxSDKServiceException, DropExporterException {

    logger.debug("Check statistics");
    Drop d = dropRepository.findById(drop.getId()).orElse(null);

    for (TranslationKit tk : d.getTranslationKits()) {

      assertEquals(
          "For locale: " + tk.getLocale().getBcp47Tag(),
          tk.getNumTranslationKitUnits(),
          tk.getNumTranslatedTranslationKitUnits());
      assertNotNull(tk.getWordCount());
      assertTrue(tk.getImported());

      if (tk.getLocale().getBcp47Tag().equals("ko-KR")) {
        assertEquals(
            "For locale: " + tk.getLocale().getBcp47Tag(), 1, tk.getNotFoundTextUnitIds().size());
        assertEquals(
            "For locale: " + tk.getLocale().getBcp47Tag(), 1, tk.getNumSourceEqualsTarget());
        assertEquals(1, tk.getWordCount().intValue());
      } else {
        if (tk.getLocale().getBcp47Tag().equals("ja-JP")) {
          assertEquals(9, tk.getWordCount().intValue());
        } else {
          assertEquals(1, tk.getWordCount().intValue());
        }
        assertEquals(
            "For locale: " + tk.getLocale().getBcp47Tag(), 0, tk.getNotFoundTextUnitIds().size());
        assertEquals(
            "For locale: " + tk.getLocale().getBcp47Tag(), 0, tk.getNumSourceEqualsTarget());
      }
    }
  }

  @Test
  public void testGetDropFolderName() {
    Calendar cal = Calendar.getInstance();
    cal.set(2013, 0, 1, 0, 0, 0);

    assertEquals(
        "Week 48 (Tuesday) - 01 January 2013 - 00.00.00", dropService.getDropName(cal.getTime()));

    cal.set(Calendar.WEEK_OF_YEAR, 6);
    assertEquals(
        "Week 1 (Tuesday) - 05 February 2013 - 00.00.00", dropService.getDropName(cal.getTime()));
  }

  @Test
  public void testCancelDrop() throws Exception {
    List<String> locales = List.of("fr-FR");
    DropTestData dropTestData = DropTestData.createWithDefaultData(testIdWatcher);

    ExportDropConfig exportDropConfig = dropTestData.getExportDropConfig(locales);

    logger.debug("Check inital number of untranslated units");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 1);

    logger.debug("Create an initial drop for the repository");
    Drop drop =
        awaitPollableProcess(
                dropService.startDropExportProcess(
                    exportDropConfig, PollableTask.INJECT_CURRENT_TASK))
            .get();

    logger.debug("Drop export finished, localize files in Box");

    Drop canceledDrop =
        awaitPollableProcess(dropService.cancelDrop(drop.getId(), PollableTask.INJECT_CURRENT_TASK))
            .get();

    Assert.assertTrue("Drop should be canceled", canceledDrop.getCanceled());
  }

  @Test(expected = PollableTaskExecutionException.class)
  @Ignore("flaky test")
  public void testCancelDropException() throws Exception {

    DropService dropServiceSpy = spy(dropService);
    doReturn(true).when(dropServiceSpy).isDropBeingProcessed(any(Drop.class));

    List<String> locales = List.of("fr-FR");

    DropTestData dropTestData = DropTestData.createWithDefaultData(testIdWatcher);

    ExportDropConfig exportDropConfig = dropTestData.getExportDropConfig(locales);

    logger.debug("Check initial number of untranslated units");
    checkNumberOfUntranslatedTextUnit(dropTestData, locales, 1);

    logger.debug("Create an initial drop for the repository");
    Drop drop =
        awaitPollableProcess(
                dropServiceSpy.startDropExportProcess(
                    exportDropConfig, PollableTask.INJECT_CURRENT_TASK))
            .get();

    awaitPollableProcess(dropServiceSpy.cancelDrop(drop.getId(), PollableTask.INJECT_CURRENT_TASK));
  }

  @Test
  public void testCancelDropNonExistentId()
      throws DropExporterException, InterruptedException, CancelDropException {

    // don't create any drops
    Long nonExistentDropId = 9999L;

    PollableFuture<Drop> startCancelDrop =
        dropService.cancelDrop(nonExistentDropId, PollableTask.INJECT_CURRENT_TASK);

    try {
      awaitPollableProcess(startCancelDrop);
      fail();
    } catch (PollableTaskException pte) {
      PollableTask cancelPollableTask =
          pollableTaskService.getPollableTask(startCancelDrop.getPollableTask().getId());
      assertTrue(
          cancelPollableTask
              .getErrorMessage()
              .contains("Drop with ID [" + nonExistentDropId + "] does not exist"));
    }
  }
}

class FileSystemDropFile implements DropFile {
  private final File file;

  FileSystemDropFile(File file) {
    this.file = file;
  }

  public String getName() {
    return file.getName();
  }

  public String getContent() throws IOException {
    return Files.toString(file, StandardCharsets.UTF_8);
  }
}
