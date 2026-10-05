package com.box.l10n.mojito.service.assetExtraction;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig.PreserveStatusMode;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@code --preserve-status} on the mojito push / asset-processing path.
 *
 * <p>Both tests share the same setup: push a properties string, translate it as {@code
 * REVIEW_NEEDED}, then push again with the same key but changed source content. That triggers a
 * <em>unique name-only</em> source leverage (same name, different content). The tests differ only
 * in the preserve-status mode on the second push, and assert whether the copied translation keeps
 * {@code REVIEW_NEEDED} or is downgraded to {@code TRANSLATION_NEEDED}.
 *
 * <p>Name-only is the interesting case: under PRECISION it is always treated as needing
 * re-translation, while UNIQUE should keep the original status when the match is unambiguous.
 */
public class AssetExtractionPreserveStatusTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired AssetService assetService;

  @Autowired RepositoryService repositoryService;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired TMService tmService;

  @Autowired LocaleService localeService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  /**
   * UNIQUE + unique name-only match → keep source status.
   *
   * <p>After the content change, source leveraging finds exactly one prior text unit with the same
   * name ({@code hello}) and copies its fr-FR translation. With {@link PreserveStatusMode#UNIQUE},
   * a unique match preserves status even when the match is not high-precision (name+content). So
   * the leveraged string must stay {@code REVIEW_NEEDED} (not be re-queued as for-translation /
   * AI work). Also asserts the French target text itself was copied.
   */
  @Test
  public void uniquePreserveStatusKeepsReviewNeededOnNameOnlyLeverage() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
    repositoryService.addRepositoryLocale(repository, "fr-FR");

    Asset asset =
        processAsset(repository, "demo.properties", "hello=Hello\n", PreserveStatusMode.PRECISION);

    TMTextUnit original = tmTextUnitRepository.findFirstByAssetIdAndName(asset.getId(), "hello");
    translate(original, "fr-FR", "Bonjour", TMTextUnitVariant.Status.REVIEW_NEEDED);

    processAsset(
        repository, "demo.properties", "hello=Hello updated\n", PreserveStatusMode.UNIQUE);

    TextUnitDTO leveraged = getUsedTranslation(repository, "hello", "fr-FR");
    assertEquals("Bonjour", leveraged.getTarget());
    assertEquals(
        "UNIQUE should preserve REVIEW_NEEDED for a unique name-only match",
        TMTextUnitVariant.Status.REVIEW_NEEDED,
        leveraged.getStatus());
  }

  /**
   * PRECISION + unique name-only match → downgrade to TRANSLATION_NEEDED (default / legacy).
   *
   * <p>Same setup as {@link #uniquePreserveStatusKeepsReviewNeededOnNameOnlyLeverage()}, but the
   * second push uses {@link PreserveStatusMode#PRECISION} (also the default when the flag is
   * omitted). PRECISION only preserves status for unique <em>high-precision</em> matches
   * (name+content). A name-only match always sets {@code translationNeededIfUniqueMatch}, so even
   * though the match is unique the status must become {@code TRANSLATION_NEEDED}. Confirms we did
   * not change today's push behavior when UNIQUE is not requested.
   */
  @Test
  public void precisionPreserveStatusDowngradesReviewNeededOnNameOnlyLeverage() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
    repositoryService.addRepositoryLocale(repository, "fr-FR");

    Asset asset =
        processAsset(repository, "demo.properties", "hello=Hello\n", PreserveStatusMode.PRECISION);

    TMTextUnit original = tmTextUnitRepository.findFirstByAssetIdAndName(asset.getId(), "hello");
    translate(original, "fr-FR", "Bonjour", TMTextUnitVariant.Status.REVIEW_NEEDED);

    processAsset(
        repository, "demo.properties", "hello=Hello updated\n", PreserveStatusMode.PRECISION);

    TextUnitDTO leveraged = getUsedTranslation(repository, "hello", "fr-FR");
    assertEquals("Bonjour", leveraged.getTarget());
    assertEquals(
        "PRECISION should downgrade name-only matches to TRANSLATION_NEEDED",
        TMTextUnitVariant.Status.TRANSLATION_NEEDED,
        leveraged.getStatus());
  }

  private Asset processAsset(
      Repository repository, String path, String content, PreserveStatusMode preserveStatusMode)
      throws Exception {
    PollableFuture<Asset> future =
        assetService.addOrUpdateAssetAndProcessIfNeeded(
            repository.getId(),
            path,
            content,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            preserveStatusMode);
    pollableTaskService.waitForPollableTask(future.getPollableTask().getId());
    return future.get();
  }

  private void translate(
      TMTextUnit tu, String localeTag, String translation, TMTextUnitVariant.Status status) {
    Locale locale = localeService.findByBcp47Tag(localeTag);
    tmService.addCurrentTMTextUnitVariant(tu.getId(), locale.getId(), translation, status, true);
  }

  private TextUnitDTO getUsedTranslation(Repository repository, String name, String localeTag) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setRepositoryIds(repository.getId());
    params.setName(name);
    params.setLocaleTags(List.of(localeTag));
    params.setStatusFilter(StatusFilter.TRANSLATED);
    params.setUsedFilter(UsedFilter.USED);
    List<TextUnitDTO> results = textUnitSearcher.search(params);
    assertEquals(1, results.size());
    return results.get(0);
  }
}
