package com.box.l10n.mojito.service.branch.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchMergeTarget;
import com.box.l10n.mojito.entity.BranchNotification;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.rest.textunit.ImportTextUnitsBatch;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.branch.BranchMergeTargetRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.branch.BranchTestData;
import com.box.l10n.mojito.service.branch.notification.noop.BranchNotificationMessageSenderNoop;
import com.box.l10n.mojito.service.commit.CommitRepository;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BranchNotificationServiceTest extends ServiceTestBase {

  @Autowired BranchNotificationService branchNotificationService;

  @Autowired AssetContentService assetContentService;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired TextUnitBatchImporterService textUnitBatchImporterService;

  @Autowired BranchStatisticService branchStatisticService;

  @Autowired BranchNotificationRepository branchNotificationRepository;

  @Autowired AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

  @Autowired BranchMergeTargetRepository branchMergeTargetRepository;

  @Autowired CommitRepository commitRepository;

  @Autowired BranchStatisticRepository branchStatisticRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  String branchNotifierId = "noop-1";

  @Test
  public void allNotification() throws Exception {

    BranchTestData branchTestData = new BranchTestData(testIdWatcher);

    String branch2ContentUpdated =
        "# string1 description\n"
            + "string1=content1\n"
            + "string2=content2\n"
            + "string4=content4\n"
            + "string5=content5\n"
            + "string6=content6\n";

    AssetContent assetContentBranch2 =
        assetContentService.createAssetContent(
            branchTestData.getAsset(), branch2ContentUpdated, false, branchTestData.getBranch2());
    assetExtractionService
        .processAssetAsync(assetContentBranch2.getId(), null, null, null, null)
        .get();

    waitForCondition(
        "Branch1 new notification must be sent",
        () ->
            branchNotificationRepository
                    .findByBranchAndNotifierId(branchTestData.getBranch1(), branchNotifierId)
                    .getNewMsgSentAt()
                != null);

    waitForCondition(
        "Branch2 translated notification must be sent",
        () ->
            branchNotificationRepository
                    .findByBranchAndNotifierId(branchTestData.getBranch2(), branchNotifierId)
                    .getNewMsgSentAt()
                != null);

    translateBranch(branchTestData.getBranch2());

    waitForCondition(
        "Branch2 translated notification must be sent",
        () ->
            branchNotificationRepository
                    .findByBranchAndNotifierId(branchTestData.getBranch2(), branchNotifierId)
                    .getTranslatedMsgSentAt()
                != null);
  }

  @Test
  public void allNotificationFail() throws InterruptedException {
    String exceptionMessage = "allNotificationFail - should not fail silently";

    BranchNotificationMessageSenderNoop branchNotificationMessageSenderNoopException =
        new BranchNotificationMessageSenderNoop(branchNotifierId) {
          @Override
          public String sendNewMessage(
              String branchName, String username, List<String> sourceStrings)
              throws BranchNotificationMessageSenderException {
            throw new BranchNotificationMessageSenderException(exceptionMessage);
          }

          @Override
          public String sendUpdatedMessage(
              String branchName, String username, String messageId, List<String> sourceStrings)
              throws BranchNotificationMessageSenderException {
            throw new BranchNotificationMessageSenderException(exceptionMessage);
          }

          @Override
          public void sendTranslatedMessage(
              String branchName, String username, String messageId, String safeI18NCommit)
              throws BranchNotificationMessageSenderException {
            throw new BranchNotificationMessageSenderException(exceptionMessage);
          }

          @Override
          public void sendScreenshotMissingMessage(
              String branchName, String username, String messageId)
              throws BranchNotificationMessageSenderException {
            throw new BranchNotificationMessageSenderException(exceptionMessage);
          }
        };

    String exceptionNotifierId = "noop-exception";
    branchNotificationService.branchNotificationMessageSenders
        .mapIdToBranchNotificationMessageSender.put(
        exceptionNotifierId, branchNotificationMessageSenderNoopException);

    BranchTestData branchTestData = new BranchTestData(testIdWatcher, Set.of(exceptionNotifierId));

    waitForCondition(
        "Branch1 must have associated text units",
        () ->
            !this.branchStatisticService
                .getTextUnitDTOsForBranch(branchTestData.getBranch1())
                .isEmpty());

    assertThatThrownBy(
            () ->
                branchNotificationService.sendNotificationsForBranch(
                    branchTestData.getBranch1().getId()))
        .satisfies(
            throwable -> {
              Exception ex = (Exception) throwable;
              assertThat(
                      Arrays.stream(ex.getSuppressed())
                          .anyMatch(
                              suppressed -> suppressed.getMessage().contains(exceptionMessage)))
                  .isTrue();
            });

    BranchNotification branchNotification =
        branchNotificationRepository.findByBranchAndNotifierId(
            branchTestData.getBranch1(), exceptionNotifierId);

    assertThat(branchNotification == null || branchNotification.getNewMsgSentAt() == null).isTrue();
    assertThat(branchNotification == null || branchNotification.getTranslatedMsgSentAt() == null)
        .isTrue();
    assertThat(branchNotification == null || branchNotification.getTranslatedMsgSentAt() == null)
        .isTrue();
    assertThat(
            branchNotification == null
                || branchNotification.getScreenshotMissingMsgSentAt() == null)
        .isTrue();
  }

  @Test
  public void screenshotMissing() throws Exception {
    BranchTestData branchTestData = new BranchTestData(testIdWatcher);

    waitForCondition(
        "Branch notification for branch1 is missing",
        () ->
            branchNotificationRepository.findByBranchAndNotifierId(
                    branchTestData.getBranch1(), branchNotifierId)
                != null);

    BranchNotification branchNotification =
        branchNotificationRepository.findByBranchAndNotifierId(
            branchTestData.getBranch1(), branchNotifierId);
    branchNotification.setScreenshotMissingMsgSentAt(ZonedDateTime.now().minusMinutes(31));
    branchNotificationRepository.save(branchNotification);

    branchNotificationService.scheduleMissingScreenshotNotificationsForBranch(
        branchTestData.getBranch1(), branchNotifierId);

    waitForCondition(
        "Branch1 screenshot missing notification must be sent",
        () ->
            branchNotificationRepository
                    .findByBranchAndNotifierId(branchTestData.getBranch1(), branchNotifierId)
                    .getScreenshotMissingMsgSentAt()
                != null);
  }

  void translateBranch(Branch branch) throws ExecutionException, InterruptedException {

    List<Long> tmTextUnitIdsByBranch = assetTextUnitToTMTextUnitRepository.findByBranch(branch);

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
    textUnitSearcherParameters.setTmTextUnitIds(
        tmTextUnitIdsByBranch); // TODO(perf) does it mean you can see the branch in workbench as
    // before? also need to review the branch page?
    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    textUnitDTOs.forEach(tu -> tu.setTarget(tu.getSource() + " - " + tu.getTargetLocale()));

    ImportTextUnitsBatch importTextUnitsBatch = new ImportTextUnitsBatch();
    importTextUnitsBatch.setTextUnits(textUnitDTOs);

    textUnitBatchImporterService
        .asyncImportTextUnits(
            importTextUnitsBatch.getTextUnits(),
            importTextUnitsBatch.isIntegrityCheckSkipped(),
            importTextUnitsBatch.isIntegrityCheckKeepStatusIfFailedAndSameTarget())
        .get();

    // make sure the stats are ready for whatever is done next in notification
    branchStatisticService.computeAndSaveBranchStatistics(branch);
  }

  @Test
  public void safeTranslationNotificationSendsWithCommit() throws Exception {
    BranchTestData branchTestData = new BranchTestData(testIdWatcher);
    String branch2ContentUpdated = "# string1 description\nstring1=content1\n";

    AssetContent assetContentBranch2 =
        assetContentService.createAssetContent(
            branchTestData.getAsset(), branch2ContentUpdated, false, branchTestData.getBranch2());
    assetExtractionService
        .processAssetAsync(assetContentBranch2.getId(), null, null, null, null)
        .get();

    // New message must be sent first for any translated message to go out
    waitForCondition(
        "Branch2 new notification must be sent",
        () ->
            branchNotificationRepository
                    .findByBranchAndNotifierId(branchTestData.getBranch2(), branchNotifierId)
                    .getNewMsgSentAt()
                != null);

    // Branch is tracked for safe i18n if it has a merge target. It shouldn't send the translation
    // until the commit exists or the translated time is over the default 8 hour mark
    BranchMergeTarget branchMergeTarget = new BranchMergeTarget();
    branchMergeTarget.setBranch(branchTestData.getBranch2());
    branchMergeTarget.setTargetsMain(true);
    branchMergeTarget = branchMergeTargetRepository.save(branchMergeTarget);

    translateBranch(branchTestData.getBranch2());

    // Wait for branch stats to run
    branchNotificationService.sendNotificationsForBranch(branchTestData.getBranch2());

    // Make sure no translation message was sent out as the commit doesn't exist
    assertNull(
        branchNotificationRepository
            .findByBranchAndNotifierId(branchTestData.getBranch2(), branchNotifierId)
            .getTranslatedMsgSentAt());

    // Create a commit, link it up and run the branch stats again
    Commit commit = new Commit();
    commit.setName("ffffffffffffffffffffffffffffffffffffffff");
    commit.setAuthorEmail("mojito@example.com");
    commit.setAuthorName("Mojito");
    commit.setRepository(branchTestData.getBranch2().getRepository());
    commit.setSourceCreationDate(ZonedDateTime.now());
    commit = commitRepository.save(commit);
    branchMergeTarget.setCommit(commit);
    branchMergeTargetRepository.save(branchMergeTarget);

    branchNotificationService.sendNotificationsForBranch(branchTestData.getBranch2());

    // Translated message should send as the commit exists - it is checked in
    waitForCondition(
        "Branch2 translated notification must be sent",
        () ->
            branchNotificationRepository
                    .findByBranchAndNotifierId(branchTestData.getBranch2(), branchNotifierId)
                    .getTranslatedMsgSentAt()
                != null);
  }

  @Test
  public void safeTranslationNotificationSendsAfterExceedsTranslatedWindow() throws Exception {
    BranchTestData branchTestData = new BranchTestData(testIdWatcher);
    String branch2ContentUpdated = "# string1 description\nstring1=content1\n";

    AssetContent assetContentBranch2 =
        assetContentService.createAssetContent(
            branchTestData.getAsset(), branch2ContentUpdated, false, branchTestData.getBranch2());
    assetExtractionService
        .processAssetAsync(assetContentBranch2.getId(), null, null, null, null)
        .get();

    waitForCondition(
        "Branch2 new notification must be sent",
        () ->
            branchNotificationRepository
                    .findByBranchAndNotifierId(branchTestData.getBranch2(), branchNotifierId)
                    .getNewMsgSentAt()
                != null);

    // Track branch for safe i18n
    BranchMergeTarget branchMergeTarget = new BranchMergeTarget();
    branchMergeTarget.setBranch(branchTestData.getBranch2());
    branchMergeTarget.setTargetsMain(true);
    branchMergeTargetRepository.save(branchMergeTarget);

    translateBranch(branchTestData.getBranch2());

    branchNotificationService.sendNotificationsForBranch(branchTestData.getBranch2());

    // Make sure no translation message was sent out as the commit doesn't exist
    assertNull(
        branchNotificationRepository
            .findByBranchAndNotifierId(branchTestData.getBranch2(), branchNotifierId)
            .getTranslatedMsgSentAt());

    // Set the translated date to an older date that is outside the translated window
    BranchStatistic branchStatistic =
        branchStatisticRepository.findByBranch(branchTestData.getBranch2());
    branchStatistic.setTranslatedDate(
        ZonedDateTime.now().minus(branchNotificationService.notificationFallbackTimeout));
    branchStatisticRepository.save(branchStatistic);
    branchStatisticRepository.flush();

    // Branch should use old notification flow as it has been translated for longer than x amount of
    // hours
    branchNotificationService.sendNotificationsForBranch(branchTestData.getBranch2());

    waitForCondition(
        "Branch2 translated notification must be sent",
        () ->
            branchNotificationRepository
                    .findByBranchAndNotifierId(branchTestData.getBranch2(), branchNotifierId)
                    .getTranslatedMsgSentAt()
                != null);
  }
}
