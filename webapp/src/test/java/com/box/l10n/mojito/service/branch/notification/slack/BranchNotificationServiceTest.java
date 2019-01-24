package com.box.l10n.mojito.service.branch.notification.slack;

import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchNotification;
import com.box.l10n.mojito.rest.textunit.ImportTextUnitsBatch;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.branch.BranchTestData;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationRepository;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationService;
import com.box.l10n.mojito.service.branch.notification.job.BranchNotificationMissingScreenshotsJob;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Channel;
import com.box.l10n.mojito.slack.response.ChatPostMessageResponse;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Date;
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
    BranchNotificationMissingScreenshotsJob branchNotificationMissingScreenshotsJob;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void allNotfication() throws Exception {

        BranchTestData branchTestData = new BranchTestData(testIdWatcher);

        String branch2ContentUpdated = "# string1 description\n"
                + "string1=content1\n"
                + "string2=content2\n"
                + "string4=content4\n"
                + "string5=content5\n"
                + "string6=content6\n";

        AssetContent assetContentBranch2 = assetContentService.createAssetContent(branchTestData.getAsset(), branch2ContentUpdated, branchTestData.getBranch2());
        assetExtractionService.processAssetAsync(assetContentBranch2.getId(), null, null).get();

        translateBranch(branchTestData.getBranch2());

        waitForCondition("Branch2 translated notification must be sent",
                () -> {
                    return branchNotificationRepository.findByBranch(branchTestData.getBranch2()).getTranslatedMsgSentAt() != null;
                });


        waitForCondition("Branch1 new notification must be sent",
                () -> {
                    return branchNotificationRepository.findByBranch(branchTestData.getBranch1()).getNewMsgSentAt() != null;
                });
    }

    @Ignore("need to finish screenshot implementation")
    @Test
    public void screenshotMissing() throws Exception {
        BranchTestData branchTestData = new BranchTestData(testIdWatcher);

        BranchNotification branchNotification = branchNotificationRepository.findByBranch(branchTestData.getBranch1());
        branchNotification.setScreenshotMissingMsgSentAt(DateTime.now().minusMinutes(31));

        branchNotificationMissingScreenshotsJob.schedule(branchTestData.getBranch1().getId(), new Date());

        waitForCondition("Branch1 new notification must be sent",
                () -> {
                    return branchNotificationRepository.findByBranch(branchTestData.getBranch1()).getScreenshotMissingMsgSentAt() != null;
                });
    }

    void translateBranch(Branch branch) throws ExecutionException, InterruptedException {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        textUnitSearcherParameters.setBranchId(branch.getId());
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