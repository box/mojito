package com.box.l10n.mojito.service.tm.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.asset.VirtualAsset;
import com.box.l10n.mojito.service.asset.VirtualAssetBadRequestException;
import com.box.l10n.mojito.service.asset.VirtualAssetService;
import com.box.l10n.mojito.service.asset.VirtualAssetTextUnit;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetMappingService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParametersForTesting;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class TextUnitBatchImporterServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TextUnitBatchImporterServiceTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired TextUnitBatchImporterService textUnitBatchImporterService;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired RepositoryService repositoryService;

  @Autowired VirtualAssetService virtualAssetService;

  @Autowired TMService tmService;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetMappingService assetMappingService;

  @Test
  public void testAsyncImportTextUnitsNameOnly() throws InterruptedException {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setRepositoryName(tmTestData.repository.getName());
    textUnitDTO.setTargetLocale(tmTestData.frFR.getBcp47Tag());
    textUnitDTO.setAssetPath(tmTestData.asset.getPath());
    textUnitDTO.setName("TEST2");
    textUnitDTO.setTarget("TEST2 translation for fr");
    textUnitDTO.setComment("Comment2");

    TextUnitDTO textUnitDTO2 = new TextUnitDTO();
    textUnitDTO2.setRepositoryName(tmTestData.repository.getName());
    textUnitDTO2.setTargetLocale(tmTestData.frFR.getBcp47Tag());
    textUnitDTO2.setAssetPath(tmTestData.asset.getPath());
    textUnitDTO2.setName("TEST3");
    textUnitDTO2.setTarget("TEST3 translation for fr");
    textUnitDTO2.setComment("Comment3");

    TextUnitDTO textUnitDTO3 = new TextUnitDTO();
    textUnitDTO3.setRepositoryName(tmTestData.repository.getName());
    textUnitDTO3.setTargetLocale(tmTestData.frFR.getBcp47Tag());
    textUnitDTO3.setAssetPath(tmTestData.asset.getPath());
    textUnitDTO3.setName("zuora_error_message_verify_state_province");
    textUnitDTO3.setTarget("zuora_error_message_verify_state_province translation for fr");
    textUnitDTO3.setComment("Comment1");

    List<TextUnitDTO> textUnitDTOsForImport =
        Arrays.asList(textUnitDTO, textUnitDTO2, textUnitDTO3);

    PollableFuture<Void> asyncImportTextUnits =
        textUnitBatchImporterService.asyncImportTextUnits(textUnitDTOsForImport, false, false);

    pollableTaskService.waitForPollableTask(asyncImportTextUnits.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryNames(Arrays.asList(tmTestData.repository.getName()));
    textUnitSearcherParameters.setAssetPath(tmTestData.asset.getPath());
    textUnitSearcherParameters.setLocaleTags(Arrays.asList("fr-FR"));

    List<TextUnitDTO> textUnitDTOsFromSearch = textUnitSearcher.search(textUnitSearcherParameters);

    int i = 0;

    assertEquals(
        "zuora_error_message_verify_state_province", textUnitDTOsFromSearch.get(i).getName());
    assertEquals(
        "zuora_error_message_verify_state_province translation for fr",
        textUnitDTOsFromSearch.get(i).getTarget());
    i++;
    assertEquals("TEST2", textUnitDTOsFromSearch.get(i).getName());
    assertEquals("TEST2 translation for fr", textUnitDTOsFromSearch.get(i).getTarget());
    i++;
    assertEquals("TEST3", textUnitDTOsFromSearch.get(i).getName());
    assertEquals("TEST3 translation for fr", textUnitDTOsFromSearch.get(i).getTarget());
    i++;
  }

  @Test
  public void testAsyncImportTextUnitsFromSearch() throws InterruptedException {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryNames(Arrays.asList(tmTestData.repository.getName()));
    textUnitSearcherParameters.setAssetPath(tmTestData.asset.getPath());
    textUnitSearcherParameters.setLocaleTags(Arrays.asList("fr-FR"));

    List<TextUnitDTO> textUnitDTOsForImport = textUnitSearcher.search(textUnitSearcherParameters);
    for (TextUnitDTO textUnitDTO : textUnitDTOsForImport) {
      textUnitDTO.setTarget(textUnitDTO.getName() + " from import");
      textUnitDTO.setName(null); // make sure we import by id
    }

    PollableFuture<Void> asyncImportTextUnits =
        textUnitBatchImporterService.asyncImportTextUnits(textUnitDTOsForImport, false, false);
    pollableTaskService.waitForPollableTask(asyncImportTextUnits.getPollableTask().getId());

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertFalse(textUnitDTOs.isEmpty());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      assertEquals(textUnitDTO.getName() + " from import", textUnitDTO.getTarget());
    }
  }

  @Test
  public void testAsyncImportTextUnitsDuplicatedNames() throws InterruptedException {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(tmTestData.asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    AssetTextUnit createAssetTextUnit1 =
        assetExtractionService.createAssetTextUnit(
            assetExtraction, "TEST4", "Content4", "comment4");
    AssetTextUnit createAssetTextUnit2 =
        assetExtractionService.createAssetTextUnit(
            assetExtraction, "TEST4", "Content4b", "comment4");

    assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(
        assetExtraction.getId(),
        tmTestData.tm.getId(),
        tmTestData.asset.getId(),
        null,
        PollableTask.INJECT_CURRENT_TASK);
    assetExtractionService.markAssetExtractionAsLastSuccessful(tmTestData.asset, assetExtraction);

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryNames(Arrays.asList(tmTestData.repository.getName()));
    textUnitSearcherParameters.setAssetPath(tmTestData.asset.getPath());
    textUnitSearcherParameters.setLocaleTags(Arrays.asList("fr-FR"));

    List<TextUnitDTO> textUnitDTOsForImport = textUnitSearcher.search(textUnitSearcherParameters);
    for (TextUnitDTO textUnitDTO : textUnitDTOsForImport) {
      if ("Content4".equals(textUnitDTO.getSource())) {
        textUnitDTO.setTarget(textUnitDTO.getName() + " from import");
      } else {
        textUnitDTO.setTarget(textUnitDTO.getName() + " from import b");
      }
      textUnitDTO.setTmTextUnitId(null); // we're testing import by name
    }

    PollableFuture<Void> asyncImportTextUnits =
        textUnitBatchImporterService.asyncImportTextUnits(textUnitDTOsForImport, false, false);
    pollableTaskService.waitForPollableTask(asyncImportTextUnits.getPollableTask().getId());

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertFalse(textUnitDTOs.isEmpty());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      if ("Content4".equals(textUnitDTO.getSource())) {
        assertEquals(textUnitDTO.getName() + " from import", textUnitDTO.getTarget());
      } else {
        assertEquals(textUnitDTO.getName() + " from import b", textUnitDTO.getTarget());
      }
    }
  }

  @Test
  public void testAsyncImportTextUnitsDuplicatedEntries() throws InterruptedException {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(tmTestData.asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    AssetTextUnit createAssetTextUnit1 =
        assetExtractionService.createAssetTextUnit(
            assetExtraction, "TEST4", "Content4", "comment4");

    assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(
        assetExtraction.getId(),
        tmTestData.tm.getId(),
        tmTestData.asset.getId(),
        null,
        PollableTask.INJECT_CURRENT_TASK);
    assetExtractionService.markAssetExtractionAsLastSuccessful(tmTestData.asset, assetExtraction);

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryNames(Arrays.asList(tmTestData.repository.getName()));
    textUnitSearcherParameters.setAssetPath(tmTestData.asset.getPath());
    textUnitSearcherParameters.setLocaleTags(Arrays.asList("fr-FR"));

    List<TextUnitDTO> textUnitDTOsForImport = textUnitSearcher.search(textUnitSearcherParameters);

    TextUnitDTO duplicatedEntry = null;

    for (TextUnitDTO textUnitDTO : textUnitDTOsForImport) {
      textUnitDTO.setTmTextUnitId(null); // we're testing import by n ame
      if ("Content4".equals(textUnitDTO.getSource())) {
        duplicatedEntry = textUnitDTO;
        duplicatedEntry.setTarget(duplicatedEntry.getSource() + "-duplicated");
      }
    }
    textUnitDTOsForImport.add(duplicatedEntry);

    PollableFuture<Void> asyncImportTextUnits =
        textUnitBatchImporterService.asyncImportTextUnits(textUnitDTOsForImport, false, false);
    pollableTaskService.waitForPollableTask(asyncImportTextUnits.getPollableTask().getId());

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertFalse(textUnitDTOs.isEmpty());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      if ("Content4".equals(textUnitDTO.getSource())) {
        assertEquals(textUnitDTO.getSource() + "-duplicated", textUnitDTO.getTarget());
      }
    }
  }

  @Test
  public void testImportMulipleRepositoryAssetAndLocale() throws Exception {
    Repository repository1 =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("testImportMulipleRepositoryAssetAndLocale1"));
    Repository repository2 =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("testImportMulipleRepositoryAssetAndLocale2"));

    for (Repository repository : Arrays.asList(repository1, repository2)) {
      RepositoryLocale repositoryLocaleFrFR =
          repositoryService.addRepositoryLocale(repository, "fr-FR");
      RepositoryLocale repositoryLocaleKoKR =
          repositoryService.addRepositoryLocale(repository, "ko-KR");

      VirtualAsset virtualAsset1 = new VirtualAsset();
      virtualAsset1.setRepositoryId(repository.getId());
      virtualAsset1.setPath("default");
      virtualAsset1 = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset1);

      VirtualAsset virtualAsset2 = new VirtualAsset();
      virtualAsset2.setRepositoryId(repository.getId());
      virtualAsset2.setPath("default2");
      virtualAsset2 = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset2);

      List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

      VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
      virtualAssetTextUnit.setName("name1");
      virtualAssetTextUnit.setContent("content1");
      virtualAssetTextUnit.setComment("comment1");
      virtualAssetTextUnits.add(virtualAssetTextUnit);

      virtualAssetTextUnit = new VirtualAssetTextUnit();
      virtualAssetTextUnit.setName("name2");
      virtualAssetTextUnit.setContent("content2");
      virtualAssetTextUnit.setComment("comment2");
      virtualAssetTextUnits.add(virtualAssetTextUnit);

      virtualAssetService.addTextUnits(virtualAsset1.getId(), virtualAssetTextUnits).get();
      virtualAssetService.addTextUnits(virtualAsset2.getId(), virtualAssetTextUnits).get();
    }

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryNames(
        Arrays.asList(repository1.getName(), repository2.getName()));

    List<TextUnitDTO> textUnitDTOsForImport = textUnitSearcher.search(textUnitSearcherParameters);
    for (TextUnitDTO textUnitDTO : textUnitDTOsForImport) {
      textUnitDTO.setTarget(
          textUnitDTO.getRepositoryName()
              + ":"
              + textUnitDTO.getAssetPath()
              + ":"
              + textUnitDTO.getTargetLocale()
              + ":"
              + textUnitDTO.getName());
    }

    textUnitBatchImporterService.asyncImportTextUnits(textUnitDTOsForImport, false, false).get();

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertFalse(textUnitDTOs.isEmpty());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      assertEquals(
          textUnitDTO.getRepositoryName()
              + ":"
              + textUnitDTO.getAssetPath()
              + ":"
              + textUnitDTO.getTargetLocale()
              + ":"
              + textUnitDTO.getName(),
          textUnitDTO.getTarget());
    }
  }

  @Test
  public void testUnused()
      throws InterruptedException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          VirtualAssetBadRequestException,
          ExecutionException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("testUnused"));
    RepositoryLocale repositoryLocaleFrFR =
        repositoryService.addRepositoryLocale(repository, "fr-FR");

    VirtualAsset virtualAsset1 = new VirtualAsset();
    virtualAsset1.setRepositoryId(repository.getId());
    virtualAsset1.setPath("default");
    virtualAsset1 = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset1);

    VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
    virtualAssetTextUnit.setName("name1");
    virtualAssetTextUnit.setContent("content1");
    virtualAssetTextUnit.setComment("comment1");

    logger.debug("Create a first unused text unit for name1");
    virtualAssetService
        .addTextUnits(virtualAsset1.getId(), Arrays.asList(virtualAssetTextUnit))
        .get();
    virtualAssetService
        .replaceTextUnits(virtualAsset1.getId(), new ArrayList<VirtualAssetTextUnit>())
        .get();

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryNames(Arrays.asList(repository.getName()));

    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setRepositoryName(repository.getName());
    textUnitDTO.setAssetPath("default");
    textUnitDTO.setName("name1");
    textUnitDTO.setComment("comment1");
    textUnitDTO.setTargetLocale("fr-FR");
    textUnitDTO.setTarget("v1");

    textUnitBatchImporterService
        .asyncImportTextUnits(Arrays.asList(textUnitDTO), false, false)
        .get();

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(1, textUnitDTOs.size());
    assertEquals("content1", textUnitDTOs.get(0).getSource());
    assertEquals(
        "Should import since there is a single unused text unit for name1",
        "v1",
        textUnitDTOs.get(0).getTarget());

    virtualAssetTextUnit = new VirtualAssetTextUnit();
    virtualAssetTextUnit.setName("name1");
    virtualAssetTextUnit.setContent("content1 - v2");
    virtualAssetTextUnit.setComment("comment1");

    logger.debug("Create a second unused for text unit for name1");
    virtualAssetService
        .addTextUnits(virtualAsset1.getId(), Arrays.asList(virtualAssetTextUnit))
        .get();
    virtualAssetService
        .replaceTextUnits(virtualAsset1.getId(), new ArrayList<VirtualAssetTextUnit>())
        .get();

    textUnitDTO = new TextUnitDTO();
    textUnitDTO.setRepositoryName(repository.getName());
    textUnitDTO.setAssetPath("default");
    textUnitDTO.setName("name1");
    textUnitDTO.setComment("comment1");
    textUnitDTO.setTargetLocale("fr-FR");
    textUnitDTO.setTarget("v2");

    textUnitBatchImporterService
        .asyncImportTextUnits(Arrays.asList(textUnitDTO), false, false)
        .get();

    textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(2, textUnitDTOs.size());
    assertEquals("content1", textUnitDTOs.get(0).getSource());
    assertEquals("v1", textUnitDTOs.get(0).getTarget());
    assertEquals("content1 - v2", textUnitDTOs.get(1).getSource());
    assertNull(
        "Should not import since there is 2 text unit for name1", textUnitDTOs.get(1).getTarget());

    virtualAssetService
        .addTextUnits(virtualAsset1.getId(), Arrays.asList(virtualAssetTextUnit))
        .get();
    textUnitBatchImporterService
        .asyncImportTextUnits(Arrays.asList(textUnitDTO), false, false)
        .get();

    textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(2, textUnitDTOs.size());
    assertEquals("content1", textUnitDTOs.get(0).getSource());
    assertEquals("v1", textUnitDTOs.get(0).getTarget());
    assertEquals("content1 - v2", textUnitDTOs.get(1).getSource());
    assertEquals("v2", textUnitDTOs.get(1).getTarget());
  }

  @Test
  public void testIntegirtyChecker() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("testIntegirtyChecker"));
    RepositoryLocale repositoryLocaleFrFR =
        repositoryService.addRepositoryLocale(repository, "fr-FR");
    Locale frFR = repositoryLocaleFrFR.getLocale();

    VirtualAsset virtualAsset = new VirtualAsset();
    virtualAsset.setRepositoryId(repository.getId());
    virtualAsset.setPath("default");
    virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

    VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
    virtualAssetTextUnit.setName("name1");
    virtualAssetTextUnit.setContent("with {placeholder}");
    virtualAssetService
        .addTextUnits(virtualAsset.getId(), Arrays.asList(virtualAssetTextUnit))
        .get();

    AssetIntegrityChecker assetIntegrityChecker = new AssetIntegrityChecker();
    assetIntegrityChecker.setAssetExtension("");
    assetIntegrityChecker.setIntegrityCheckerType(IntegrityCheckerType.MESSAGE_FORMAT);
    repositoryService.updateAssetIntegrityCheckers(
        repository, Sets.newHashSet(assetIntegrityChecker));

    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setRepositoryName(repository.getName());
    textUnitDTO.setTargetLocale(frFR.getBcp47Tag());
    textUnitDTO.setAssetPath(virtualAsset.getPath());
    textUnitDTO.setName("name1");
    textUnitDTO.setTarget("with some broken {placeholder");

    PollableFuture<Void> asyncImportTextUnits =
        textUnitBatchImporterService.asyncImportTextUnits(Arrays.asList(textUnitDTO), false, false);
    pollableTaskService.waitForPollableTask(asyncImportTextUnits.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryNames(Arrays.asList(repository.getName()));
    textUnitSearcherParameters.setName("name1");

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(1, textUnitDTOs.size());
    assertEquals("name1", textUnitDTOs.get(0).getName());
    assertEquals("with some broken {placeholder", textUnitDTOs.get(0).getTarget());
    assertFalse(
        "Should be excluded with broken placeholder",
        textUnitDTOs.get(0).isIncludedInLocalizedFile());

    textUnitDTO.setTarget("with fixed {placeholder}");
    asyncImportTextUnits =
        textUnitBatchImporterService.asyncImportTextUnits(Arrays.asList(textUnitDTO), false, false);
    pollableTaskService.waitForPollableTask(asyncImportTextUnits.getPollableTask().getId());

    textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(1, textUnitDTOs.size());
    assertEquals("name1", textUnitDTOs.get(0).getName());
    assertEquals("with fixed {placeholder}", textUnitDTOs.get(0).getTarget());
    assertTrue(
        "should be included with proper placeholder",
        textUnitDTOs.get(0).isIncludedInLocalizedFile());
  }

  @Test
  public void testImportTextUnits() throws InterruptedException {
    TMTestData tmTestData = new TMTestData(this.testIdWatcher);

    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setRepositoryName(tmTestData.repository.getName());
    textUnitDTO.setTargetLocale(tmTestData.frFR.getBcp47Tag());
    textUnitDTO.setAssetPath(tmTestData.asset.getPath());
    textUnitDTO.setName("TEST2");
    textUnitDTO.setTarget("New TEST2 translation for fr");
    textUnitDTO.setComment("Comment2");
    textUnitDTO.setStatus(TMTextUnitVariant.Status.OVERRIDDEN);

    TextUnitDTO textUnitDTO2 = new TextUnitDTO();
    textUnitDTO2.setRepositoryName(tmTestData.repository.getName());
    textUnitDTO2.setTargetLocale(tmTestData.frFR.getBcp47Tag());
    textUnitDTO2.setAssetPath(tmTestData.asset.getPath());
    textUnitDTO2.setName("TEST3");
    textUnitDTO2.setComment("Comment3");
    textUnitDTO2.setTarget("New TEST3 translation for fr");

    List<TextUnitDTO> textUnitDTOsForImport = Arrays.asList(textUnitDTO, textUnitDTO2);

    this.textUnitBatchImporterService.importTextUnits(textUnitDTOsForImport, true, false);

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryNames(
        Collections.singletonList(tmTestData.repository.getName()));
    textUnitSearcherParameters.setAssetPath(tmTestData.asset.getPath());
    textUnitSearcherParameters.setLocaleTags(List.of("fr-FR"));

    List<TextUnitDTO> textUnitDTOsFromSearch =
        this.textUnitSearcher.search(textUnitSearcherParameters);

    int i = 1;
    assertEquals("TEST2", textUnitDTOsFromSearch.get(i).getName());
    assertEquals("New TEST2 translation for fr", textUnitDTOsFromSearch.get(i).getTarget());
    assertEquals(TMTextUnitVariant.Status.OVERRIDDEN, textUnitDTOsFromSearch.get(i).getStatus());
    i++;
    assertEquals("TEST3", textUnitDTOsFromSearch.get(i).getName());
    assertEquals("New TEST3 translation for fr", textUnitDTOsFromSearch.get(i).getTarget());
    assertEquals(TMTextUnitVariant.Status.APPROVED, textUnitDTOsFromSearch.get(i).getStatus());

    textUnitDTO = new TextUnitDTO();
    textUnitDTO.setRepositoryName(tmTestData.repository.getName());
    textUnitDTO.setTargetLocale(tmTestData.frFR.getBcp47Tag());
    textUnitDTO.setAssetPath(tmTestData.asset.getPath());
    textUnitDTO.setName("TEST2");
    textUnitDTO.setTarget("The newest TEST2 translation for fr");
    textUnitDTO.setComment("Comment2");

    textUnitDTO2 = new TextUnitDTO();
    textUnitDTO2.setRepositoryName(tmTestData.repository.getName());
    textUnitDTO2.setTargetLocale(tmTestData.frFR.getBcp47Tag());
    textUnitDTO2.setAssetPath(tmTestData.asset.getPath());
    textUnitDTO2.setName("TEST3");
    textUnitDTO2.setTarget("The newest TEST3 translation for fr");
    textUnitDTO2.setComment("Comment3");

    textUnitDTOsForImport = Arrays.asList(textUnitDTO, textUnitDTO2);

    this.textUnitBatchImporterService.importTextUnits(textUnitDTOsForImport, false, false);

    textUnitDTOsFromSearch = this.textUnitSearcher.search(textUnitSearcherParameters);

    i = 1;
    assertEquals("TEST2", textUnitDTOsFromSearch.get(i).getName());
    assertEquals("New TEST2 translation for fr", textUnitDTOsFromSearch.get(i).getTarget());
    assertEquals(TMTextUnitVariant.Status.OVERRIDDEN, textUnitDTOsFromSearch.get(i).getStatus());
    i++;
    assertEquals("TEST3", textUnitDTOsFromSearch.get(i).getName());
    assertEquals("The newest TEST3 translation for fr", textUnitDTOsFromSearch.get(i).getTarget());
    assertEquals(TMTextUnitVariant.Status.APPROVED, textUnitDTOsFromSearch.get(i).getStatus());

    // Set status to APPROVED
    this.tmService.addTMTextUnitCurrentVariant(
        textUnitDTOsFromSearch.get(1).getTmTextUnitId(),
        textUnitDTOsFromSearch.get(1).getLocaleId(),
        "Reverted TEST2 translation for fr",
        textUnitDTOsFromSearch.get(1).getComment(),
        TMTextUnitVariant.Status.APPROVED,
        true);

    textUnitDTOsFromSearch = this.textUnitSearcher.search(textUnitSearcherParameters);

    i = 1;
    assertEquals("TEST2", textUnitDTOsFromSearch.get(i).getName());
    assertEquals("Reverted TEST2 translation for fr", textUnitDTOsFromSearch.get(i).getTarget());
    assertEquals(TMTextUnitVariant.Status.APPROVED, textUnitDTOsFromSearch.get(i).getStatus());
    i++;
    assertEquals("TEST3", textUnitDTOsFromSearch.get(i).getName());
    assertEquals("The newest TEST3 translation for fr", textUnitDTOsFromSearch.get(i).getTarget());
    assertEquals(TMTextUnitVariant.Status.APPROVED, textUnitDTOsFromSearch.get(i).getStatus());
  }
}
