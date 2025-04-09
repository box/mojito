package com.box.l10n.mojito.service.appender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.Status;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.security.user.UserService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class POTAssetAppenderTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired BranchRepository branchRepository;

  @Autowired BranchService branchService;

  @Autowired RepositoryService repositoryService;

  @Autowired AssetService assetService;

  @Autowired UserService userService;

  @Autowired AssetContentService assetContentService;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired BranchStatisticService branchStatisticService;

  @Autowired BranchStatisticRepository branchStatisticRepository;

  @Autowired TMService tmService;

  @Test
  public void testPOTAppender()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          InterruptedException,
          ExecutionException,
          IOException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("append-repo"));
    RepositoryLocale rl = repositoryService.addRepositoryLocale(repository, "fr-FR");

    String assetPath = "path/to/messages.pot";
    Asset asset = assetService.createAsset(repository.getId(), assetPath, false);

    String basePath =
        Resources.getResource("com/box/l10n/mojito/appender/pot/input/base.pot").getPath();
    String masterContent = Files.readString(Path.of(basePath));

    Branch master =
        branchService.createBranch(
            asset.getRepository(), "master", null, (Sets.newHashSet("noop-1")));
    AssetContent assetContentMaster =
        assetContentService.createAssetContent(asset, masterContent, false, master);

    assetExtractionService
        .processAssetAsync(assetContentMaster.getId(), null, null, null, null)
        .get();

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder()
            .repositoryId(repository.getId())
            .forRootLocale(true)
            .build();

    List<TextUnitDTO> textUnits = textUnitSearcher.search(textUnitSearcherParameters);

    assertEquals(1, textUnits.size());

    textUnits.forEach(
        tu -> {
          tmService.addTMTextUnitCurrentVariant(
              tu.getTmTextUnitId(),
              rl.getLocale().getId(),
              "New translation!",
              tu.getTargetComment(),
              tu.getStatus(),
              tu.isIncludedInLocalizedFile());
        });

    String branchContent =
        Files.readString(
            Path.of(
                Resources.getResource("com/box/l10n/mojito/appender/pot/input/branch.pot")
                    .getPath()));

    // Process strings on dev branch, simulating string differential
    Branch devBranch =
        branchService.createBranch(
            asset.getRepository(), "new_append_branch", null, (Sets.newHashSet("noop-1")));
    AssetContent branchAssetContent =
        assetContentService.createAssetContent(asset, branchContent, false, devBranch);
    assetExtractionService
        .processAssetAsync(branchAssetContent.getId(), null, null, null, null)
        .get();

    waitForCondition(
        "Branch statistics must be set",
        () -> branchStatisticRepository.findByBranch(devBranch) != null);

    waitForCondition(
        "Text unit count must be 4 for the branch",
        () -> branchStatisticService.getTextUnitDTOsForBranch(devBranch).size() == 4);

    textUnits = branchStatisticService.getTextUnitDTOsForBranch(devBranch);

    AtomicInteger counter = new AtomicInteger(1);
    textUnits.forEach(
        tu -> {
          String content = "New translation! - " + counter;
          if (counter.get() == 4) content = "Final \"translation\"";
          tmService.addTMTextUnitCurrentVariant(
              tu.getTmTextUnitId(),
              rl.getLocale().getId(),
              content,
              tu.getTargetComment(),
              tu.getStatus(),
              tu.isIncludedInLocalizedFile());
          counter.getAndIncrement();
        });

    POTAssetAppender potAssetAppender = new POTAssetAppender(masterContent);
    potAssetAppender.appendTextUnits(textUnits);

    String appendedContent = potAssetAppender.getAssetContent();

    String expectedAppendedContent =
        Files.readString(
            Path.of(
                Resources.getResource("com/box/l10n/mojito/appender/pot/expected/appended.pot")
                    .getPath()));

    assertEquals(expectedAppendedContent, appendedContent);

    String localizedAsset =
        tmService.generateLocalized(
            asset,
            appendedContent,
            rl,
            null,
            null,
            null,
            Status.ACCEPTED,
            null,
            UUID.randomUUID().toString());

    String expectedLocalizedAsset =
        Files.readString(
            Path.of(
                Resources.getResource("com/box/l10n/mojito/appender/pot/expected/localized.pot")
                    .getPath()));
    assertEquals(expectedLocalizedAsset, localizedAsset);
  }
}
