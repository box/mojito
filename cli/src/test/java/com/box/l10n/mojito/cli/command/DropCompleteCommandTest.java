package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_LOCALIZED_FILES_NAME;
import static com.box.l10n.mojito.service.drop.exporter.DropExporterDirectories.DROP_FOLDER_SOURCE_FILES_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.service.drop.DropRepository;
import com.box.l10n.mojito.service.drop.exporter.DropExporterException;
import com.box.l10n.mojito.service.drop.exporter.DropExporterService;
import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporter;
import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporterConfig;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.test.XliffUtils;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureRule;

/**
 * @author wadimw
 */
public class DropCompleteCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropCompleteCommandTest.class);

  @Autowired AssetClient assetClient;

  @Autowired DropRepository dropRepository;

  @Autowired RepositoryClient repositoryClient;

  @Autowired TMTextUnitCurrentVariantRepository textUnitCurrentVariantRepository;

  @Autowired LocaleService localeService;

  @Autowired DropExporterService dropExporterService;

  @Test
  public void dropComplete() throws Exception {

    // ----- setup -----

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");

    Asset asset2 =
        assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
    importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");

    RepositoryStatusChecker repositoryStatusChecker = new RepositoryStatusChecker();
    waitForCondition(
        "wait for repository stats to show forTranslationCount > 0 before exporting a drop",
        () ->
            repositoryStatusChecker.hasStringsForTranslationsForExportableLocales(
                repositoryClient.getRepositoryById(repository.getId())));

    getL10nJCommander().run("drop-export", "-r", repository.getName());

    final Long dropId = getLastDropIdFromOutput(outputCapture);

    logger.debug("Mocking the console input for drop id: {}", dropId);
    Console mockConsole = mock(Console.class);
    when(mockConsole.readLine(Long.class))
        .thenAnswer(
            new Answer<Long>() {
              @Override
              public Long answer(InvocationOnMock invocation) throws Throwable {
                return getAvailableDropNumberForDropIdFromOutput(dropId);
              }
            });

    L10nJCommander l10nJCommander = getL10nJCommander();

    DropImportCommand dropImportCommand = l10nJCommander.getCommand(DropImportCommand.class);

    dropImportCommand.console = mockConsole;

    int numberOfFrenchTranslationsBefore = getNumberOfFrenchTranslations(repository);

    // Don't localize ja-JP
    localizeDropFiles(dropRepository.findById(dropId).orElse(null), Set.of("fr-FR"));

    l10nJCommander.run(
        new String[] {"drop-import", "-r", repository.getName(), "--number-drop-fetched", "1000"});

    int numberOfFrenchTranslationsAfter = getNumberOfFrenchTranslations(repository);

    assertEquals(
        "2 new french translations must be added",
        numberOfFrenchTranslationsBefore + 2,
        numberOfFrenchTranslationsAfter);

    Drop dropBefore = dropRepository.findById(dropId).orElseThrow();

    assertEquals("Drop must be partially imported", true, dropBefore.getPartiallyImported());

    // ----- test -----

    logger.debug("Mocking the console input for drop id: {}", dropId);
    Console mockConsole2 = mock(Console.class);
    when(mockConsole2.readLine(Long.class))
        .thenAnswer(
            new Answer<Long>() {
              @Override
              public Long answer(InvocationOnMock invocation) throws Throwable {
                return getAvailableDropNumberForDropIdFromOutput(dropId);
              }
            });
    DropCompleteCommand dropCompleteCommand = l10nJCommander.getCommand(DropCompleteCommand.class);
    dropCompleteCommand.console = mockConsole2;

    l10nJCommander.run(
        new String[] {
          "drop-complete", "-r", repository.getName(), "--number-drop-fetched", "1000"
        });

    Drop dropAfter = dropRepository.findById(dropId).orElseThrow();

    assertEquals("Drop should not be partially imported", false, dropAfter.getPartiallyImported());
  }

  @Test
  public void dropCompleteSpecifiedId() throws Exception {

    // ----- setup -----

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");

    Asset asset2 =
        assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
    importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");

    RepositoryStatusChecker repositoryStatusChecker = new RepositoryStatusChecker();
    waitForCondition(
        "wait for repository stats to show forTranslationCount > 0 before exporting a drop",
        () ->
            repositoryStatusChecker.hasStringsForTranslationsForExportableLocales(
                repositoryClient.getRepositoryById(repository.getId())));

    getL10nJCommander().run("drop-export", "-r", repository.getName());

    final Long dropId = getLastDropIdFromOutput(outputCapture);

    L10nJCommander l10nJCommander = getL10nJCommander();

    DropImportCommand dropImportCommand = l10nJCommander.getCommand(DropImportCommand.class);

    int numberOfFrenchTranslationsBefore = getNumberOfFrenchTranslations(repository);

    // Don't localize ja-JP
    localizeDropFiles(dropRepository.findById(dropId).orElse(null), Set.of("fr-FR"));

    l10nJCommander.run(
        new String[] {"drop-import", "-r", repository.getName(), "-i", dropId.toString()});

    int numberOfFrenchTranslationsAfter = getNumberOfFrenchTranslations(repository);

    assertEquals(
        "2 new french translations must be added",
        numberOfFrenchTranslationsBefore + 2,
        numberOfFrenchTranslationsAfter);

    Drop dropBefore = dropRepository.findById(dropId).orElseThrow();

    assertEquals("Drop must be partially imported", true, dropBefore.getPartiallyImported());

    // ----- test -----

    DropCompleteCommand dropCompleteCommand = l10nJCommander.getCommand(DropCompleteCommand.class);

    l10nJCommander.run(
        new String[] {"drop-complete", "-r", repository.getName(), "-i", dropId.toString()});

    Drop dropAfter = dropRepository.findById(dropId).orElseThrow();

    assertEquals("Drop should not be partially imported", false, dropAfter.getPartiallyImported());
  }

  private int getNumberOfFrenchTranslations(Repository repository) {
    return textUnitCurrentVariantRepository
        .findByTmTextUnit_Tm_IdAndLocale_Id(
            repository.getTm().getId(), localeService.findByBcp47Tag("fr-FR").getId())
        .size();
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

  public void localizeDropFiles(Drop drop, Collection<String> allowedLocales)
      throws BoxSDKServiceException, DropExporterException, IOException {

    logger.debug("Localize files in a drop");

    FileSystemDropExporter fileSystemDropExporter =
        (FileSystemDropExporter) dropExporterService.recreateDropExporter(drop);
    FileSystemDropExporterConfig fileSystemDropExporterConfig =
        fileSystemDropExporter.getFileSystemDropExporterConfig();

    File[] sourceFiles =
        Paths.get(fileSystemDropExporterConfig.getDropFolderPath(), DROP_FOLDER_SOURCE_FILES_NAME)
            .toFile()
            .listFiles();

    for (File sourceFile : sourceFiles) {
      String sourceFileName = sourceFile.getName();
      if (!allowedLocales.stream().anyMatch(sourceFileName::startsWith)) {
        logger.debug("Skipping locallocalization for drop file: {}", sourceFileName);
        continue;
      }
      String localizedContent = Files.toString(sourceFile, StandardCharsets.UTF_8);
      localizedContent = XliffUtils.localizeTarget(localizedContent, "Import Drop");
      localizedContent = XliffUtils.replaceTargetState(localizedContent, "translated");

      Path localizedFolderPath =
          Paths.get(
              fileSystemDropExporterConfig.getDropFolderPath(),
              DROP_FOLDER_LOCALIZED_FILES_NAME,
              sourceFile.getName());
      Files.write(localizedContent, localizedFolderPath.toFile(), StandardCharsets.UTF_8);
    }
  }
}
