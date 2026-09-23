package com.box.l10n.mojito.service.assetintegritychecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.entity.RepoTypeIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.okapi.XliffState;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.asset.AssetUpdateException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.EllipsisIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.HtmlTagIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.MessageFormatIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TrailingWhitespaceIntegrityChecker;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitIntegrityCheckService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.hibernate.Hibernate;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wyau
 */
public class AssetIntegrityCheckerServiceTest extends ServiceTestBase {

  @Autowired RepositoryService repositoryService;

  @Autowired AssetService assetService;

  @Autowired AssetRepository assetRepository;

  @Autowired AssetIntegrityCheckerService assetIntegrityCheckerService;

  @Autowired IntegrityCheckerFactory integrityCheckerFactory;

  @Autowired RepoTypeService repoTypeService;

  @Autowired TMService tmService;

  @Autowired LocaleService localeService;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired private TMTextUnitRepository tmTextUnitRepository;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired EntityManager entityManager;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired TMTextUnitIntegrityCheckService tmTextUnitIntegrityCheckService;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();
  protected static final String ASSET_PATH = "source-asset-path.xliff";
  private static final String PROPERTIES_ASSET_PATH = "messages.properties";
  private static final String MESSAGE_FORMAT_SOURCE =
      "{numFiles, plural, one{# There is one file} other{There are # files}}";
  private static final String BROKEN_MESSAGE_FORMAT_TARGET =
      "{numFiles, plural, one{Il y a un fichier} other{Il y a # fichiers}";
  private static final String VALID_MESSAGE_FORMAT_TARGET =
      "{numFiles, plural, one{Il y a un fichier} other{Il y a # fichiers}}";
  private static final String MESSAGE_FORMAT_SOURCE_WITH_ELLIPSIS =
      "{numFiles, plural, one{# There is one file…} other{There are # files…}}";
  private static final String VALID_MESSAGE_FORMAT_TARGET_WITH_THREE_DOTS =
      "{numFiles, plural, one{Il y a un fichier...} other{Il y a # fichiers...}}";
  private static final String BROKEN_MESSAGE_FORMAT_TARGET_ALTERNATE =
      "{numFiles, plural, one{Il y a deux fichiers} other{Il y a # fichiers}";
  private static final String PROPERTIES_SOURCE = "greeting=Hello {name}\n";
  private static final String PROPERTIES_BROKEN_TARGET = "greeting=Bonjour {name\n";

  @Test
  public void testTmUpdateWithoutRepoTypeOrCheckersKeepsTranslationIncluded() throws Exception {
    Repository repository = createRepository(null);

    assertBrokenMessageFormatTranslationIncluded(repository, true);
  }

  @Test
  public void testTypeOnlyIntegrityCheckerIsUsedInTmServiceUpdate() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    Repository repository = createRepository(typeCheckers);

    assertBrokenMessageFormatTranslationIncluded(repository, false);
  }

  @Test
  public void testRepoOnlyIntegrityCheckerIsUsedInTmServiceUpdate() throws Exception {
    Repository repository = createRepository(null);
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT);

    assertBrokenMessageFormatTranslationIncluded(repository, false);
  }

  @Test
  public void testOverlappingTypeAndRepoIntegrityCheckerIsUsedInTmServiceUpdate() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    Repository repository = createRepository(typeCheckers);
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT);

    assertBrokenMessageFormatTranslationIncluded(repository, false);
  }

  @Test
  public void testTypeMessageFormatAllowsValidIcuWithEllipsisMismatch() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    Repository repository = createRepository(typeCheckers);

    assertTranslationIncluded(
        repository,
        MESSAGE_FORMAT_SOURCE_WITH_ELLIPSIS,
        VALID_MESSAGE_FORMAT_TARGET_WITH_THREE_DOTS,
        true);
  }

  @Test
  public void testDisjointTypeAndRepoIntegrityCheckersAreUsedInTmServiceUpdate() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    Repository repository = createRepository(typeCheckers);
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.ELLIPSIS);

    assertTranslationIncluded(
        repository,
        MESSAGE_FORMAT_SOURCE_WITH_ELLIPSIS,
        VALID_MESSAGE_FORMAT_TARGET_WITH_THREE_DOTS,
        false);
  }

  @Test
  public void testDisjointTypeCheckerRejectsWhenRepoCheckerWouldAllow() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.ELLIPSIS));
    Repository repository = createRepository(typeCheckers);
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT);

    assertTranslationIncluded(
        repository,
        MESSAGE_FORMAT_SOURCE_WITH_ELLIPSIS,
        VALID_MESSAGE_FORMAT_TARGET_WITH_THREE_DOTS,
        false);
  }

  @Test
  public void testFactoryUnionsCheckersUsingDetachedAssetAndFiltersByExtension() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.ELLIPSIS));
    typeCheckers.add(checker("other.properties", IntegrityCheckerType.HTML_TAG));
    Repository repository = createRepository(typeCheckers);
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT);
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.TRAILING_WHITESPACE);

    addSourceAsset(repository, MESSAGE_FORMAT_SOURCE);
    entityManager.clear();
    Asset asset = assetRepository.findByPathAndRepositoryId(ASSET_PATH, repository.getId());
    assertFalse(Hibernate.isInitialized(asset.getRepository().getRepoType()));
    entityManager.clear();

    Set<TextUnitIntegrityChecker> checkers = integrityCheckerFactory.getTextUnitCheckers(asset);

    assertEquals(3, checkers.size());
    assertTrue(
        checkers.stream().anyMatch(checker -> checker instanceof MessageFormatIntegrityChecker));
    assertTrue(checkers.stream().anyMatch(checker -> checker instanceof EllipsisIntegrityChecker));
    assertTrue(
        checkers.stream()
            .anyMatch(checker -> checker instanceof TrailingWhitespaceIntegrityChecker));
    assertFalse(checkers.stream().anyMatch(checker -> checker instanceof HtmlTagIntegrityChecker));
  }

  @Test
  public void testTypedRepoWithEmptyTypeCheckersStillUsesRepoCheckers() throws Exception {
    Repository repository = createRepository(Collections.emptySet());
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT);

    assertBrokenMessageFormatTranslationIncluded(repository, false);
  }

  @Test
  public void testAssigningTypeAfterFirstImportStartsRejecting() throws Exception {
    Repository repository = createRepository(null);
    PreparedImport preparedImport = prepareXliffImport(repository, MESSAGE_FORMAT_SOURCE);

    importXliffTarget(preparedImport, MESSAGE_FORMAT_SOURCE, BROKEN_MESSAGE_FORMAT_TARGET, true);

    RepoType repoType =
        repoTypeService.createRepoType(
            testIdWatcher.getEntityName("assignedType"),
            null,
            "",
            Set.of(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT)));
    repository.setRepoType(repoType);
    repositoryRepository.save(repository);

    importXliffTarget(
        preparedImport, MESSAGE_FORMAT_SOURCE, BROKEN_MESSAGE_FORMAT_TARGET_ALTERNATE, false);
  }

  @Test
  public void testClearingTypeAfterFirstImportStopsRejecting() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    Repository repository = createRepository(typeCheckers);
    PreparedImport preparedImport = prepareXliffImport(repository, MESSAGE_FORMAT_SOURCE);

    importXliffTarget(preparedImport, MESSAGE_FORMAT_SOURCE, BROKEN_MESSAGE_FORMAT_TARGET, false);

    repository.setRepoType(null);
    repositoryRepository.save(repository);

    importXliffTarget(
        preparedImport, MESSAGE_FORMAT_SOURCE, BROKEN_MESSAGE_FORMAT_TARGET_ALTERNATE, true);
  }

  @Test
  public void testUpdatingTypeCheckersAfterFirstImportStartsRejecting() throws Exception {
    Repository repository = createRepository(Collections.emptySet());
    RepoType repoType = repository.getRepoType();
    PreparedImport preparedImport = prepareXliffImport(repository, MESSAGE_FORMAT_SOURCE);

    importXliffTarget(preparedImport, MESSAGE_FORMAT_SOURCE, BROKEN_MESSAGE_FORMAT_TARGET, true);

    repoTypeService.updateIntegrityCheckers(
        repoType, Set.of(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT)));

    importXliffTarget(
        preparedImport, MESSAGE_FORMAT_SOURCE, BROKEN_MESSAGE_FORMAT_TARGET_ALTERNATE, false);
  }

  @Test
  public void testTypeOnlyIntegrityCheckerIsUsedOnWorkbenchCheck() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    Repository repository = createRepository(typeCheckers);
    PreparedImport preparedImport = prepareXliffImport(repository, MESSAGE_FORMAT_SOURCE);

    tmTextUnitIntegrityCheckService.checkTMTextUnitIntegrity(
        preparedImport.tmTextUnitId, VALID_MESSAGE_FORMAT_TARGET);

    try {
      tmTextUnitIntegrityCheckService.checkTMTextUnitIntegrity(
          preparedImport.tmTextUnitId, BROKEN_MESSAGE_FORMAT_TARGET);
      fail("Broken ICU should fail the workbench integrity check");
    } catch (IntegrityCheckException expected) {
    }
  }

  @Test
  public void testTypeOnlyIntegrityCheckerIsUsedOnLocalizedAssetImport() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(PROPERTIES_ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    Repository repository = createRepository(typeCheckers);
    repositoryService.addRepositoryLocale(repository, "fr-FR");

    PollableFuture<Asset> assetPollableFuture =
        assetService.addOrUpdateAssetAndProcessIfNeeded(
            repository.getId(),
            PROPERTIES_ASSET_PATH,
            PROPERTIES_SOURCE,
            false,
            null,
            null,
            null,
            null,
            null,
            null);
    pollableTaskService.waitForPollableTask(assetPollableFuture.getPollableTask().getId());
    Asset asset =
        assetRepository.findByPathAndRepositoryId(PROPERTIES_ASSET_PATH, repository.getId());
    Locale frFR = localeService.findByBcp47Tag("fr-FR");

    tmService.importLocalizedAsset(
        asset.getId(),
        PROPERTIES_BROKEN_TARGET,
        frFR.getId(),
        StatusForEqualTarget.APPROVED,
        null,
        null);

    List<TMTextUnitVariant> textUnitVariants =
        tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(
            frFR.getId(), repository.getTm().getId());
    assertEquals(1, textUnitVariants.size());
    assertFalse(textUnitVariants.get(0).isIncludedInLocalizedFile());
    assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, textUnitVariants.get(0).getStatus());
  }

  @Test
  public void testTypeOnlyIntegrityCheckerProtectsPlaceholdersInPseudolocalization()
      throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(PROPERTIES_ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    Repository repository = createRepository(typeCheckers);

    PollableFuture<Asset> assetPollableFuture =
        assetService.addOrUpdateAssetAndProcessIfNeeded(
            repository.getId(),
            PROPERTIES_ASSET_PATH,
            PROPERTIES_SOURCE,
            false,
            null,
            null,
            null,
            null,
            null,
            null);
    pollableTaskService.waitForPollableTask(assetPollableFuture.getPollableTask().getId());
    Asset asset =
        assetRepository.findByPathAndRepositoryId(PROPERTIES_ASSET_PATH, repository.getId());

    String pseudoLocalized = tmService.generatePseudoLocalized(asset, PROPERTIES_SOURCE, null);

    assertTrue(pseudoLocalized.contains("{name}"));
  }

  private Repository createRepository(Set<RepoTypeIntegrityChecker> typeCheckers) throws Exception {
    RepoType repoType = null;
    if (typeCheckers != null) {
      repoType =
          repoTypeService.createRepoType(
              testIdWatcher.getEntityName("repoType"), null, "", typeCheckers);
    }
    return repositoryService.createRepository(
        testIdWatcher.getEntityName("repository"),
        null,
        null,
        false,
        Collections.emptySet(),
        Collections.emptySet(),
        repoType);
  }

  private RepoTypeIntegrityChecker checker(
      String assetPath, IntegrityCheckerType integrityCheckerType) {
    RepoTypeIntegrityChecker checker = new RepoTypeIntegrityChecker();
    checker.setAssetExtension(org.apache.commons.io.FilenameUtils.getExtension(assetPath));
    checker.setIntegrityCheckerType(integrityCheckerType);
    return checker;
  }

  private void addSourceAsset(Repository repository, String sourceTextUnit)
      throws AssetUpdateException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException {
    String sourceXliff =
        xliffDataFactory.generateSourceXliff(
            Arrays.asList(xliffDataFactory.createTextUnit(1L, "tu1", sourceTextUnit, null)));
    PollableFuture<Asset> assetPollableFuture =
        assetService.addOrUpdateAssetAndProcessIfNeeded(
            repository.getId(), ASSET_PATH, sourceXliff, false, null, null, null, null, null, null);
    pollableTaskService.waitForPollableTask(assetPollableFuture.getPollableTask().getId());
  }

  private void assertBrokenMessageFormatTranslationIncluded(
      Repository repository, boolean expectedIncluded) throws Exception {
    assertTranslationIncluded(
        repository, MESSAGE_FORMAT_SOURCE, BROKEN_MESSAGE_FORMAT_TARGET, expectedIncluded);
  }

  private void assertTranslationIncluded(
      Repository repository, String sourceTextUnit, String targetTextUnit, boolean expectedIncluded)
      throws Exception {

    String frFR = "fr-FR";
    repositoryService.addRepositoryLocale(repository, "fr-FR");

    addSourceAsset(repository, sourceTextUnit);

    Long tmId = repository.getTm().getId();
    List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(tmId);
    assertEquals(1, tmTextUnits.size());
    Long tmTextUnitId = tmTextUnits.get(0).getId();

    String targetXliff =
        xliffDataFactory.generateTargetXliff(
            Arrays.asList(
                xliffDataFactory.createTextUnit(
                    tmTextUnitId,
                    "tu1",
                    sourceTextUnit,
                    null,
                    targetTextUnit,
                    frFR,
                    XliffState.TRANSLATED)),
            frFR);

    Locale frFRLocale = localeService.findByBcp47Tag(frFR);
    tmService.updateTMWithXLIFFById(targetXliff, null);

    List<TMTextUnitVariant> textUnitVariants =
        tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(frFRLocale.getId(), tmId);

    assertEquals(1, textUnitVariants.size());
    // TODO(P2) check message from message table
    assertEquals(expectedIncluded, textUnitVariants.get(0).isIncludedInLocalizedFile());
    if (!expectedIncluded) {
      assertEquals(
          TMTextUnitVariant.Status.TRANSLATION_NEEDED, textUnitVariants.get(0).getStatus());
    }
  }

  private PreparedImport prepareXliffImport(Repository repository, String sourceTextUnit)
      throws Exception {
    repositoryService.addRepositoryLocale(repository, "fr-FR");
    addSourceAsset(repository, sourceTextUnit);
    List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(repository.getTm().getId());
    assertEquals(1, tmTextUnits.size());
    return new PreparedImport(
        repository, tmTextUnits.get(0).getId(), localeService.findByBcp47Tag("fr-FR"));
  }

  private void importXliffTarget(
      PreparedImport preparedImport,
      String sourceTextUnit,
      String targetTextUnit,
      boolean expectedIncluded)
      throws Exception {
    String targetXliff =
        xliffDataFactory.generateTargetXliff(
            Arrays.asList(
                xliffDataFactory.createTextUnit(
                    preparedImport.tmTextUnitId,
                    "tu1",
                    sourceTextUnit,
                    null,
                    targetTextUnit,
                    "fr-FR",
                    XliffState.TRANSLATED)),
            "fr-FR");
    tmService.updateTMWithXLIFFById(targetXliff, null);

    TMTextUnitCurrentVariant current =
        tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
            preparedImport.frFR.getId(), preparedImport.tmTextUnitId);
    TMTextUnitVariant variant = current.getTmTextUnitVariant();
    assertEquals(expectedIncluded, variant.isIncludedInLocalizedFile());
    if (!expectedIncluded) {
      assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, variant.getStatus());
    }
  }

  private static final class PreparedImport {
    final Repository repository;
    final Long tmTextUnitId;
    final Locale frFR;

    PreparedImport(Repository repository, Long tmTextUnitId, Locale frFR) {
      this.repository = repository;
      this.tmTextUnitId = tmTextUnitId;
      this.frFR = frFR;
    }
  }
}
