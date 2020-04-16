package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchNotification;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.branch.notification.job.BranchNotificationMissingScreenshotsJob;
import com.box.l10n.mojito.service.branch.notification.job.BranchNotificationMissingScreenshotsJobInput;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.utils.DateTimeUtils;
import com.google.common.base.Joiner;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BranchNotificationService {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchNotificationService.class);

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    BranchNotificationRepository branchNotificationRepository;

    @Autowired
    BranchStatisticService branchStatisticService;

    @Autowired
    BranchNotificationMissingScreenshotsJob branchNotificationMissingScreenshotsJob;

    @Autowired
    BranchStatisticRepository branchStatisticRepository;

    @Autowired(required = false)
    List<BranchNotificationMessageSender> branchNotificationMessageSenders = new ArrayList<>();

    @Autowired
    DateTimeUtils dateTimeUtils;

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    /**
     * When the state of branch changes, notifications must be send (new, updated, translated). This method sends
     * the required notification based on the current state of the branch.
     * <p>
     * It also schedules a job to check if screenshot are missing and send notification.
     *
     * @param branch
     */
    public void sendNotificationsForBranch(Long branchId) {
        logger.debug("sendNotificationsForBranch, id: {}", branchId);
        Branch branch = branchRepository.findOne(branchId);
        sendNotificationsForBranch(branch);
    }

    /**
     * If the branch was created more that X ({@link #scheduleMissingScreenshotNotificationsForBranch})
     * minutes ago and screenshot are still missing, send a notification.
     *
     * @param branchId
     */
    public void sendMissingScreenshotNotificationForBranch(Long branchId, String senderType) {
        logger.debug("sendMissingScreenshotNotificationForBranch, id: {}", branchId);
        BranchNotificationMessageSender branchNotificationMessageSender = findBySenderType(senderType);
        sendMissingScreenshotNotificationForBranchWithSender(branchNotificationMessageSender, branchId);
    }

    BranchNotificationMessageSender findBySenderType(String senderType) {
        return branchNotificationMessageSenders.stream().
                filter(s -> getSenderType(s).equals(senderType)).
                findFirst().
                orElseThrow(() -> new RuntimeException("Can't find sender for type: " + senderType));
    }

    void sendMissingScreenshotNotificationForBranchWithSender(BranchNotificationMessageSender branchNotificationMessageSender, Long branchId) {
        logger.debug("sendMissingScreenshotNotificationForBranch, id: {}", branchId);
        Branch branch = branchRepository.findOne(branchId);
        sendMissingScreenshotNotificationsForBranch(branchNotificationMessageSender, branch);
    }

    void sendNotificationsForBranch(Branch branch) {
        logger.debug("sendNotificationsForBranch: {} ({})", branch.getId(), branch.getName());
        for (BranchNotificationMessageSender branchNotificationMessageSender : branchNotificationMessageSenders) {
            try {
                sendNotificationsForBranchWithSender(branchNotificationMessageSender, branch);
            } catch (Exception e) {
                logger.error("Fail safe, error tracking is up to each sender", e);
            }
        }
    }

    void sendNotificationsForBranchWithSender(BranchNotificationMessageSender branchNotificationMessageSender, Branch branch) {
        String senderType = getSenderType(branchNotificationMessageSender);

        logger.debug("sendNotificationsForBranch: {} ({} with sender: {})", branch.getId(), branch.getName(), senderType);
        BranchNotificationInfo branchNotificationInfo = getBranchNotificationInfo(branch);
        BranchNotification branchNotification = getOrCreateBranchNotification(branch, senderType);

        if (shouldSendNewMessage(branchNotification, branchNotificationInfo)) {
            sendNewMessage(branchNotificationMessageSender, branch, branchNotification, branchNotificationInfo);
            scheduleMissingScreenshotNotificationsForBranch(branch, senderType);
        } else if (shouldSendUpdatedMessage(branchNotification, branchNotificationInfo)) {
            sendUpdatedMessage(branchNotificationMessageSender, branch, branchNotification, branchNotificationInfo);
            scheduleMissingScreenshotNotificationsForBranch(branch, senderType);
        }

        if (shouldSendTranslatedMessage(branch, branchNotification)) {
            sendTranslatedMessage(branchNotificationMessageSender, branch, branchNotification);
        }
    }

    /**
     * Schedule to check/send notification for missing screenshot in 30 minutes from now.
     *
     * @param branch
     * @param senderType
     */
    void scheduleMissingScreenshotNotificationsForBranch(Branch branch, String senderType) {
        Date date = DateTime.now().plusMinutes(30).toDate();

        BranchNotificationMissingScreenshotsJobInput branchNotificationMissingScreenshotsJobInput = new BranchNotificationMissingScreenshotsJobInput();
        branchNotificationMissingScreenshotsJobInput.setBranchId(branch.getId());
        branchNotificationMissingScreenshotsJobInput.setSenderType(senderType);
        QuartzJobInfo quartzJobInfo = QuartzJobInfo.newBuilder(BranchNotificationMissingScreenshotsJob.class).withInput(branchNotificationMissingScreenshotsJobInput).withTriggerStartDate(date).build();
        quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
    }

    void sendMissingScreenshotNotificationsForBranch(BranchNotificationMessageSender branchNotificationMessageSender, Branch branch) {
        logger.debug("sendMissingScreenshotNotificationForBranch: {} ({})", branch.getId(), branch.getName());

        BranchNotificationInfo branchNotificationInfo = getBranchNotificationInfo(branch);
        BranchNotification branchNotification = getOrCreateBranchNotification(branch, getSenderType(branchNotificationMessageSender));

        if (shouldSendScreenshotMissingMessage(branch, branchNotification)) {
            sendScreenshotMissingMessage(branchNotificationMessageSender, branch, branchNotification);
        }
    }

    void sendNewMessage(BranchNotificationMessageSender branchNotificationMessageSender,
                        Branch branch,
                        BranchNotification branchNotification,
                        BranchNotificationInfo branchNotificationInfo) {

        logger.debug("sendNewMessage for: {}", branch.getName());
        try {
            String messageId = branchNotificationMessageSender.sendNewMessage(
                    branch.getName(),
                    getUsername(branch),
                    branchNotificationInfo.getSourceStrings());

            branchNotification.setNewMsgSentAt(dateTimeUtils.now());
            branchNotification.setMessageId(messageId);
            branchNotification.setContentMD5(branchNotificationInfo.getContentMd5());

        } catch (BranchNotificationMessageSenderException mse) {
            logger.debug("Can't send new message", mse);
        } finally {
            branchNotificationRepository.save(branchNotification);
        }
    }

    void sendUpdatedMessage(BranchNotificationMessageSender branchNotificationMessageSender,
                            Branch branch,
                            BranchNotification branchNotification,
                            BranchNotificationInfo branchNotificationInfo) {

        logger.debug("sendUpdatedMessage for: {}", branch.getName());
        try {
            String messageId = branchNotificationMessageSender.sendUpdatedMessage(
                    branch.getName(),
                    getUsername(branch),
                    branchNotification.getMessageId(),
                    branchNotificationInfo.getSourceStrings());

            branchNotification.setUpdatedMsgSentAt(dateTimeUtils.now());
            branchNotification.setTranslatedMsgSentAt(null);
            branchNotification.setMessageId(messageId);
            branchNotification.setContentMD5(branchNotificationInfo.getContentMd5());

        } catch (BranchNotificationMessageSenderException mse) {
            logger.debug("Can't send updated message", mse);
        } finally {
            branchNotificationRepository.save(branchNotification);
        }
    }

    void sendTranslatedMessage(BranchNotificationMessageSender branchNotificationMessageSender,
                               Branch branch,
                               BranchNotification branchNotification) {
        logger.debug("sendTranslatedMessage for: {}", branch.getName());

        try {
            branchNotificationMessageSender.sendTranslatedMessage(
                    branch.getName(),
                    getUsername(branch),
                    branchNotification.getMessageId());

            branchNotification.setTranslatedMsgSentAt(dateTimeUtils.now());
        } catch (BranchNotificationMessageSenderException mse) {
            logger.debug("Can't send translated message", mse);
        } finally {
            branchNotificationRepository.save(branchNotification);
        }
    }

    void sendScreenshotMissingMessage(BranchNotificationMessageSender branchNotificationMessageSender,
                                      Branch branch,
                                      BranchNotification branchNotification) {
        logger.debug("sendScreenshotMissingMessage for: {}", branch.getName());

        try {
            branchNotificationMessageSender.sendScreenshotMissingMessage(branch.getName(), branchNotification.getMessageId(), getUsername(branch));
            branchNotification.setScreenshotMissingMsgSentAt(dateTimeUtils.now());
        } catch (BranchNotificationMessageSenderException mse) {
            logger.debug("Can't send screenshot missing message", mse);
        } finally {
            branchNotificationRepository.save(branchNotification);
        }
    }

    boolean shouldSendNewMessage(BranchNotification branchNotification, BranchNotificationInfo branchNotificationInfo) {
        return branchNotification.getNewMsgSentAt() == null
                && !branchNotificationInfo.getSourceStrings().isEmpty();
    }

    boolean shouldSendUpdatedMessage(BranchNotification branchNotification, BranchNotificationInfo branchNotificationInfo) {
        return branchNotification.getNewMsgSentAt() != null
                && !branchNotificationInfo.getSourceStrings().isEmpty()
                && !branchNotificationInfo.getContentMd5().equals(branchNotification.getContentMD5());
    }

    boolean shouldSendScreenshotMissingMessage(Branch branch, BranchNotification branchNotification) {
        return branchNotification.getScreenshotMissingMsgSentAt() == null
                && isScreenshotMissing(branch);
    }

    boolean isScreenshotMissing(Branch branch) {
        BranchStatistic branchStatistic = branchStatisticRepository.findByBranch(branch);

        List<Long> tmTextUnitWithScreenshotIds = branch.getScreenshots().stream().
                map(Screenshot::getScreenshotTextUnits).
                flatMap(Collection::stream).
                map(screenshotTextUnit -> screenshotTextUnit.getTmTextUnit().getId()).
                distinct().collect(Collectors.toList());

        return tmTextUnitWithScreenshotIds.size() < branchStatistic.getTotalCount();
    }

    boolean shouldSendTranslatedMessage(Branch branch, BranchNotification branchNotification) {
        return branchNotification.getNewMsgSentAt() != null
                && branchNotification.getTranslatedMsgSentAt() == null
                && isBranchTranslated(branch);
    }

    boolean isBranchTranslated(Branch branch) {
        BranchStatistic branchStatistic = branchStatisticRepository.findByBranch(branch);
        return branchStatistic.getForTranslationCount() == 0;
    }

    String getUsername(Branch branch) {
        return branch.getCreatedByUser() == null ? "" : branch.getCreatedByUser().getUsername();
    }

    String computeMD5(List<TextUnitDTO> textUnitDTOS) {
        String joined = textUnitDTOS.stream().
                map(t -> Joiner.on("#").join(t.getName(), t.getSource(), t.getTarget())).
                collect(Collectors.joining());

        return DigestUtils.md5Hex(joined);
    }

    List<String> getSourceStrings(Branch branch, List<TextUnitDTO> textUnitDTOsForBranch) {

        return textUnitDTOsForBranch.stream().filter(textUnitDTO -> {
            return textUnitDTO.getPluralForm() == null || "other".equals(textUnitDTO.getPluralForm());
        }).map(TextUnitDTO::getSource).collect(Collectors.toList());
    }

    BranchNotificationInfo getBranchNotificationInfo(Branch branch) {

        BranchNotificationInfo branchNotificationInfo = new BranchNotificationInfo();

        List<TextUnitDTO> textUnitDTOsForBranch = branchStatisticService.getTextUnitDTOsForBranch(branch);
        List<String> sourceStrings = getSourceStrings(branch, textUnitDTOsForBranch);

        branchNotificationInfo.setSourceStrings(sourceStrings);
        branchNotificationInfo.setContentMd5(computeMD5(textUnitDTOsForBranch));

        return branchNotificationInfo;
    }

    BranchNotification getOrCreateBranchNotification(Branch branch, String senderType) {
        logger.debug("getOrCreateBranchNotification for branch: {}", branch.getId());

        BranchNotification branchNotification = branchNotificationRepository.findByBranchAndSenderType(branch, senderType);

        if (branchNotification == null) {
            logger.debug("No branchNotification, create one for branch: {}", branch.getId());
            branchNotification = new BranchNotification();
            branchNotification.setBranch(branch);
            branchNotification.setSenderType(senderType);
            branchNotification = branchNotificationRepository.save(branchNotification);
        }
        return branchNotification;
    }

    String getSenderType(BranchNotificationMessageSender branchNotificationMessageSender) {
        return branchNotificationMessageSender.getClass().getSimpleName();
    }
}
