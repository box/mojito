package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.apiclient.AssetClient;
import com.box.l10n.mojito.apiclient.DropWsApi;
import com.box.l10n.mojito.apiclient.RepositoryClient;
import com.box.l10n.mojito.apiclient.model.AssetAssetSummary;
import com.box.l10n.mojito.apiclient.model.PageDropDropSummary;
import com.box.l10n.mojito.apiclient.model.Pageable;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMImportService;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class DropExportCommandTest extends CLITestBase {

  @Autowired TMImportService tmImport;

  @Autowired AssetClient assetClient;

  @Autowired DropWsApi dropClient;
  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryClient repositoryClient;

  @Test
  public void export() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    RepositoryStatusChecker repositoryStatusChecker = new RepositoryStatusChecker();
    waitForCondition(
        "wait for repository stats to show forTranslationCount > 0 before exporting a drop",
        () ->
            repositoryStatusChecker.hasStringsForTranslationsForExportableLocales(
                repositoryClient.getRepositoryById(repository.getId())));

    PageDropDropSummary findAllBefore =
        dropClient.getDrops(new Pageable(), repository.getId(), null, null);

    getL10nJCommander().run("drop-export", "-r", repository.getName());

    PageDropDropSummary findAllAfter =
        dropClient.getDrops(new Pageable(), repository.getId(), null, null);

    assertEquals(
        "A Drop must have been added",
        findAllBefore.getTotalElements() + 1,
        (long) findAllAfter.getTotalElements());
  }

  @Test
  public void exportFullyTranslated() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    AssetAssetSummary asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    waitForCondition(
        "Must have text units that are fully translated",
        () -> {
          return repositoryRepository
              .findById(repository.getId())
              .map(
                  r ->
                      r.getRepositoryStatistic().getUsedTextUnitCount() > 0
                          && r.getRepositoryStatistic().getRepositoryLocaleStatistics().stream()
                              .filter(
                                  rls ->
                                      Sets.newHashSet("fr-FR", "ja-JP")
                                          .contains(rls.getLocale().getBcp47Tag()))
                              .allMatch(rls -> rls.getForTranslationCount() == 0))
              .orElse(false);
        });

    PageDropDropSummary findAllBefore =
        dropClient.getDrops(new Pageable(), repository.getId(), null, null);

    getL10nJCommander().run("drop-export", "-r", repository.getName());

    PageDropDropSummary findAllAfter =
        dropClient.getDrops(new Pageable(), repository.getId(), null, null);

    assertEquals(
        "A Drop should not have been added",
        findAllBefore.getTotalElements(),
        findAllAfter.getTotalElements());
  }

  @Test
  public void exportReviewWithInheritance() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    AssetAssetSummary asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    AssetAssetSummary asset2 =
        assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
    importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
    importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

    PageDropDropSummary findAllBefore =
        dropClient.getDrops(new Pageable(), repository.getId(), null, null);

    getL10nJCommander()
        .run(
            new String[] {
              "drop-export",
              "-r",
              repository.getName(),
              "-l",
              "fr-CA",
              "-t",
              "REVIEW",
              "--use-inheritance"
            });

    PageDropDropSummary findAllAfter =
        dropClient.getDrops(new Pageable(), repository.getId(), null, null);

    assertEquals(
        "A Drop must have been added",
        findAllBefore.getTotalElements() + 1,
        (long) findAllAfter.getTotalElements());
  }
}
