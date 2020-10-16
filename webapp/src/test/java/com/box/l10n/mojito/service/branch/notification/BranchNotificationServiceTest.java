package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchNotification;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.textunit.ImportTextUnitsBatch;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.branch.BranchTestData;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class BranchNotificationServiceTest extends ServiceTestBase {

    @Autowired
    BranchNotificationService branchNotificationService;

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;

    @Autowired
    BranchStatisticService branchStatisticService;

    @Autowired
    BranchNotificationRepository branchNotificationRepository;

    @Autowired
    AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    String senderType = BranchNotificationMessageSenderNoop.class.getSimpleName();

    @Test
    public void allNotfication() throws Exception {

        BranchTestData branchTestData = new BranchTestData(testIdWatcher);

        String branch2ContentUpdated = "# string1 description\n"
                + "string1=content1\n"
                + "string2=content2\n"
                + "string4=content4\n"
                + "string5=content5\n"
                + "string6=content6\n";

        AssetContent assetContentBranch2 = assetContentService.createAssetContent(branchTestData.getAsset(), branch2ContentUpdated, false, branchTestData.getBranch2());
        assetExtractionService.processAssetAsync(assetContentBranch2.getId(), null, null, null).get();

        waitForCondition("Branch1 new notification must be sent",
                () -> {
                    return branchNotificationRepository.findByBranchAndSenderType(branchTestData.getBranch1(), senderType).getNewMsgSentAt() != null;
                });

        waitForCondition("Branch2 translated notification must be sent",
                () -> {
                    return branchNotificationRepository.findByBranchAndSenderType(branchTestData.getBranch2(), senderType).getNewMsgSentAt() != null;
                });

        translateBranch(branchTestData.getBranch2());

        waitForCondition("Branch2 translated notification must be sent",
                () -> {
                    return branchNotificationRepository.findByBranchAndSenderType(branchTestData.getBranch2(), senderType).getTranslatedMsgSentAt() != null;
                });
    }

    @Test
    public void screenshotMissing() throws Exception {
        BranchTestData branchTestData = new BranchTestData(testIdWatcher);

        waitForCondition("Branch notification for branch1 is missing",
                () -> {
                    return branchNotificationRepository.findByBranchAndSenderType(branchTestData.getBranch1(), senderType) != null;
                });

        BranchNotification branchNotification = branchNotificationRepository.findByBranchAndSenderType(branchTestData.getBranch1(), senderType);
        branchNotification.setScreenshotMissingMsgSentAt(DateTime.now().minusMinutes(31));
        branchNotificationRepository.save(branchNotification);

        branchNotificationService.scheduleMissingScreenshotNotificationsForBranch(branchTestData.getBranch1(), senderType);

        waitForCondition("Branch1 screenshot missing notification must be sent",
                () -> {
                    return branchNotificationRepository.findByBranchAndSenderType(branchTestData.getBranch1(), senderType).getScreenshotMissingMsgSentAt() != null;
                });
    }

    void translateBranch(Branch branch) throws ExecutionException, InterruptedException {

        List<Long> tmTextUnitIdsByBranch = assetTextUnitToTMTextUnitRepository.findByBranch(branch);

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitIdsByBranch); //TODO(perf) does it mean you can see the branch in workbench as before? also need to review the branch page?
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        textUnitDTOs.stream().forEach(tu -> {
            tu.setTarget(tu.getSource() + " - " + tu.getTargetLocale());
        });

        ImportTextUnitsBatch importTextUnitsBatch = new ImportTextUnitsBatch();
        importTextUnitsBatch.setTextUnits(textUnitDTOs);

        textUnitBatchImporterService.asyncImportTextUnits(
                importTextUnitsBatch.getTextUnits(),
                importTextUnitsBatch.isIntegrityCheckSkipped(),
                importTextUnitsBatch.isIntegrityCheckKeepStatusIfFailedAndSameTarget()).get();

        // make sure the stats are ready for whatever is done next in notificaiton
        branchStatisticService.computeAndSaveBranchStatistics(branch);
    }
}
