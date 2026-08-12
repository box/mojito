package com.box.l10n.mojito.service.tm.localizeasset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.Status;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class LocalizeAssetTestBase extends ServiceTestBase {

  @Autowired protected TMService tmService;

  @Autowired protected RepositoryService repositoryService;

  @Autowired protected AssetRepository assetRepository;

  @Autowired protected AssetService assetService;

  @Autowired protected PollableTaskService pollableTaskService;

  @Autowired protected TextUnitSearcher textUnitSearcher;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  protected Repository repository;
  protected Asset asset;
  protected Long tmId;
  protected Long assetId;

  protected Repository createRepository() throws Exception {
    return repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
  }

  protected RepositoryLocale addLocale(Repository repo, String bcp47Tag) {
    try {
      return repositoryService.addRepositoryLocale(repo, bcp47Tag);
    } catch (RepositoryLocaleCreationException e) {
      throw new RuntimeException(e);
    }
  }

  protected void createAsset(Repository repo, String assetPath, String assetContent) {
    asset = assetService.createAssetWithContent(repo.getId(), assetPath, assetContent);
    asset = assetRepository.findById(asset.getId()).orElse(null);
    assetId = asset.getId();
    tmId = repo.getTm().getId();
  }

  protected void processAsset(Repository repo, String assetContent) throws Exception {
    processAsset(repo, assetContent, null, null);
  }

  protected void processAsset(
      Repository repo,
      String assetContent,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions)
      throws Exception {
    PollableFuture<Asset> assetResult =
        assetService.addOrUpdateAssetAndProcessIfNeeded(
            repo.getId(),
            asset.getPath(),
            assetContent,
            false,
            null,
            null,
            null,
            null,
            filterConfigIdOverride,
            filterOptions);
    try {
      pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
    } catch (PollableTaskException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    assetResult.get();
  }

  protected List<TextUnitDTO> searchTextUnits(Repository repo) {
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repo.getId());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
    return textUnitSearcher.search(textUnitSearcherParameters);
  }

  protected String generateLocalized(
      String assetContent, RepositoryLocale repoLocale, String bcp47Tag) throws Exception {
    return generateLocalized(assetContent, repoLocale, bcp47Tag, null, null);
  }

  protected String generateLocalized(
      String assetContent,
      RepositoryLocale repoLocale,
      String bcp47Tag,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions)
      throws Exception {
    return tmService.generateLocalized(
        asset,
        assetContent,
        repoLocale,
        bcp47Tag,
        filterConfigIdOverride,
        filterOptions,
        Status.ALL,
        InheritanceMode.USE_PARENT,
        null);
  }

  protected String generateLocalizedRemoveUntranslated(
      String assetContent, RepositoryLocale repoLocale, String bcp47Tag) throws Exception {
    return generateLocalizedRemoveUntranslated(assetContent, repoLocale, bcp47Tag, null);
  }

  protected String generateLocalizedRemoveUntranslated(
      String assetContent, RepositoryLocale repoLocale, String bcp47Tag, List<String> filterOptions)
      throws Exception {
    return tmService.generateLocalized(
        asset,
        assetContent,
        repoLocale,
        bcp47Tag,
        null,
        filterOptions,
        Status.ALL,
        InheritanceMode.REMOVE_UNTRANSLATED,
        null);
  }

  protected void importTranslations(
      RepositoryLocale repoLocale, String localizedContent, StatusForEqualTarget status)
      throws ExecutionException, InterruptedException {
    tmService
        .importLocalizedAssetAsync(
            assetId, localizedContent, repoLocale.getLocale().getId(), status, null, null)
        .get();
  }
}
