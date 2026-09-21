package com.box.l10n.mojito.service.assetintegritychecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.entity.RepoTypeIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.XliffState;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.asset.AssetUpdateException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.MessageFormatIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TrailingWhitespaceIntegrityChecker;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import com.box.l10n.mojito.service.tm.TMService;
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

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();
  protected static final String ASSET_PATH = "source-asset-path.xliff";

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
  public void testFactoryUnionsCheckersUsingDetachedAssetAndFiltersByExtension() throws Exception {
    Set<RepoTypeIntegrityChecker> typeCheckers = new HashSet<>();
    typeCheckers.add(checker(ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT));
    typeCheckers.add(checker("other.properties", IntegrityCheckerType.HTML_TAG));
    Repository repository = createRepository(typeCheckers);
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT);
    assetIntegrityCheckerService.addToRepository(
        repository, ASSET_PATH, IntegrityCheckerType.TRAILING_WHITESPACE);

    addSourceAsset(repository);
    entityManager.clear();
    Asset asset = assetRepository.findByPathAndRepositoryId(ASSET_PATH, repository.getId());
    assertFalse(Hibernate.isInitialized(asset.getRepository().getRepoType()));
    entityManager.clear();

    Set<TextUnitIntegrityChecker> checkers = integrityCheckerFactory.getTextUnitCheckers(asset);

    assertEquals(2, checkers.size());
    assertTrue(
        checkers.stream().anyMatch(checker -> checker instanceof MessageFormatIntegrityChecker));
    assertTrue(
        checkers.stream()
            .anyMatch(checker -> checker instanceof TrailingWhitespaceIntegrityChecker));
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

  private void addSourceAsset(Repository repository)
      throws AssetUpdateException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException {
    String sourceTextUnit = "{numFiles, plural, one{# There is one file} other{There are # files}}";
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

    String frFR = "fr-FR";
    repositoryService.addRepositoryLocale(repository, "fr-FR");

    String sourceTextUnit = "{numFiles, plural, one{# There is one file} other{There are # files}}";
    addSourceAsset(repository);

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
                    "{numFiles, plural, one{Il y a un fichier} other{Il y a # fichiers}",
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
}
