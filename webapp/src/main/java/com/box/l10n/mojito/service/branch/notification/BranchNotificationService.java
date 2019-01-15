package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchNotification;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.branch.notification.job.BranchNotificationMissingScreenshotsJob;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.utils.DateTimeUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Autowired
    BranchNotificationMessageSender branchNotificationMessageSender;

    @Autowired
    DateTimeUtils dateTimeUtils;

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
    public void sendMissingScreenshotNotificationForBranch(Long branchId) {
        logger.debug("sendMissingScreenshotNotificationForBranch, id: {}", branchId);
        Branch branch = branchRepository.findOne(branchId);
        sendMissingScreenshotNotificationsForBranch(branch);
    }

    void sendNotificationsForBranch(Branch branch) {
        logger.debug("sendNotificationsForBranch: {} ({})", branch.getId(), branch.getName());

        BranchNotificationInfo branchNotificationInfo = getBranchNotificationInfo(branch);
        BranchNotification branchNotification = getOrCreateBranchNotification(branch);

        if (shouldSendNewMessage(branchNotification, branchNotificationInfo)) {
            sendNewMessage(branch, branchNotification, branchNotificationInfo);
            scheduleMissingScreenshotNotificationsForBranch(branch);
        } else if (shouldSendUpdatedMessage(branchNotification, branchNotificationInfo)) {
            sendUpdatedMessage(branch, branchNotification, branchNotificationInfo);
            scheduleMissingScreenshotNotificationsForBranch(branch);
        }

        if (shouldSendTranslatedMessage(branch, branchNotification)) {
            sendTranslatedMessage(branch, branchNotification);
        }
    }

    /**
     * Schedule to check/send notification for missing screenshot in 30 minutes from now.
     *
     * @param branch
     */
    void scheduleMissingScreenshotNotificationsForBranch(Branch branch) {
        Date date = DateTime.now().minusMinutes(30).toDate();
        branchNotificationMissingScreenshotsJob.schedule(branch.getId(), date);
    }

    void sendMissingScreenshotNotificationsForBranch(Branch branch) {
        logger.debug("sendMissingScreenshotNotificationForBranch: {} ({})", branch.getId(), branch.getName());

        BranchNotificationInfo branchNotificationInfo = getBranchNotificationInfo(branch);
        BranchNotification branchNotification = getOrCreateBranchNotification(branch);

        if (shouldSendScreenshotMissingMessage(branch, branchNotification)) {
            sendScreenshotMissingMessage(branch, branchNotification);
        }
    }

    void sendNewMessage(Branch branch, BranchNotification branchNotification, BranchNotificationInfo branchNotificationInfo) {

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

    void sendUpdatedMessage(Branch branch, BranchNotification branchNotification, BranchNotificationInfo branchNotificationInfo) {

        logger.debug("sendUpdatedMessage for: {}", branch.getName());
        try {
            String messageId = branchNotificationMessageSender.sendUpdatedMessage(
                    branch.getName(),
                    getUsername(branch),
                    branchNotification.getMessageId(),
                    branchNotificationInfo.getSourceStrings());

            branchNotification.setUpdatedMsgSentAt(dateTimeUtils.now());
            branchNotification.setMessageId(messageId);
            branchNotification.setContentMD5(branchNotificationInfo.getContentMd5());

        } catch (BranchNotificationMessageSenderException mse) {
            logger.debug("Can't send updated message", mse);
        } finally {
            branchNotificationRepository.save(branchNotification);
        }
    }

    void sendTranslatedMessage(Branch branch, BranchNotification branchNotification) {
        logger.debug("sendTranslatedMessage for: {}", branch.getName());

        try {
            branchNotificationMessageSender.sendTranslatedMessage(getUsername(branch), branchNotification.getMessageId());
            branchNotification.setTranslatedMsgSentAt(dateTimeUtils.now());
        } catch (BranchNotificationMessageSenderException mse) {
            logger.debug("Can't send translated message", mse);
        } finally {
            branchNotificationRepository.save(branchNotification);
        }
    }

    void sendScreenshotMissingMessage(Branch branch, BranchNotification branchNotification) {
        logger.debug("sendScreenshotMissingMessage for: {}", branch.getName());

        try {
            branchNotificationMessageSender.sendScreenshotMissingMessage(getUsername(branch), branchNotification.getMessageId());
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
        return branchNotification.getUpdatedMsgSentAt() == null
                && !branchNotificationInfo.getContentMd5().equals(branchNotification.getContentMD5());
    }

    boolean shouldSendScreenshotMissingMessage(Branch branch, BranchNotification branchNotification) {
        return false;
    }

    boolean shouldSendTranslatedMessage(Branch branch, BranchNotification branchNotification) {
        return branchNotification.getTranslatedMsgSentAt() == null
                && isBranchTranslated(branch);
    }

    boolean isBranchTranslated(Branch branch) {
        BranchStatistic branchStatistic = branchStatisticRepository.findByBranch(branch);
        return branchStatistic.getForTranslationCount() == 0;
    }

    String getUsername(Branch branch) {
        return branch.getCreatedByUser() == null ? "" : branch.getCreatedByUser().getUsername();
    }

    String computeMD5(List<String> sourceStrings) {
        return DigestUtils.md5Hex(String.join("", sourceStrings));
    }

    List<String> getSourceStrings(Branch branch) {
        List<TextUnitDTO> textUnitDTOsForBranch = branchStatisticService.getTextUnitDTOsForBranch(branch);

        return textUnitDTOsForBranch.stream().filter(textUnitDTO -> {
            return textUnitDTO.getPluralForm() == null || "other".equals(textUnitDTO.getPluralForm());
        }).map(TextUnitDTO::getSource).collect(Collectors.toList());
    }

    BranchNotificationInfo getBranchNotificationInfo(Branch branch) {

        BranchNotificationInfo branchNotificationInfo = new BranchNotificationInfo();

        List<String> sourceStrings = getSourceStrings(branch);
        branchNotificationInfo.setSourceStrings(sourceStrings);
        branchNotificationInfo.setContentMd5(computeMD5(sourceStrings));

        return branchNotificationInfo;
    }

    BranchNotification getOrCreateBranchNotification(Branch branch) {
        logger.debug("getOrCreateBranchNotification for branch: {}", branch.getId());

        BranchNotification branchNotification = branchNotificationRepository.findByBranch(branch);

        if (branchNotification == null) {
            logger.debug("No branchNotification, create one for branch: {}", branch.getId());
            branchNotification = new BranchNotification();
            branchNotification.setBranch(branch);
            branchNotification.setSenderType(getSenderType());
            branchNotification = branchNotificationRepository.save(branchNotification);
        } else {
            branchNotification = resetMessageIdIfSenderChanged(branchNotification);
        }
        
        return branchNotification;
    }

    /**
     * The message id is linked to sender type, if changing the sender then the message id is not valid anymore and
     * is reset.
     */
    BranchNotification resetMessageIdIfSenderChanged(BranchNotification branchNotification) {
        logger.debug("resetMessageIdIfSenderChanged, id: {}", branchNotification.getId());
        String senderType = getSenderType();
        if (!senderType.equals(branchNotification.getSenderType())) {
            branchNotification.setSenderType(senderType);
            branchNotification.setMessageId(null);
            branchNotification = branchNotificationRepository.save(branchNotification);
        }

        return branchNotification;
    }

    String getSenderType() {
        return branchNotificationMessageSender.getClass().getSimpleName();
    }
}
