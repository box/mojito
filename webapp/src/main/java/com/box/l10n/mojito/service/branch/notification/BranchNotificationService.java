package com.box.l10n.mojito.service.branch.notification;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchMergeTarget;
import com.box.l10n.mojito.entity.BranchNotification;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.branch.BranchMergeTargetRepository;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.branch.BranchTextUnitStatisticRepository;
import com.box.l10n.mojito.service.branch.notification.job.BranchNotificationMissingScreenshotsJob;
import com.box.l10n.mojito.service.branch.notification.job.BranchNotificationMissingScreenshotsJobInput;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.utils.DateTimeUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BranchNotificationService {

  /** logger */
  static Logger logger = getLogger(BranchNotificationService.class);

  @Autowired BranchRepository branchRepository;

  @Autowired BranchNotificationRepository branchNotificationRepository;

  @Autowired BranchStatisticService branchStatisticService;

  @Autowired BranchStatisticRepository branchStatisticRepository;

  @Autowired BranchTextUnitStatisticRepository branchTextUnitStatisticRepository;

  @Autowired BranchNotificationMessageSenders branchNotificationMessageSenders;

  @Autowired DateTimeUtils dateTimeUtils;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired BranchMergeTargetRepository branchMergeTargetRepository;

  @Autowired MeterRegistry meterRegistry;

  // If a branch that is tracked for safe i18n has been translated for this duration then it
  // will fall back to using the old notification flow & message to unblock the branch.
  @Value("${l10n.branchNotification.notificationFallbackTimeout:8h}")
  protected Duration notificationFallbackTimeout;

  @Value("${l10n.branchNotification.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String schedulerName;

  @Autowired private RepositoryRepository repositoryRepository;

  /**
   * When the state of branch changes, notifications must be sent (new, updated, translated). This
   * method sends the required notification based on the current state of the branch.
   *
   * <p>It also schedules a job to check if screenshot are missing and send notification.
   *
   * @param branchId
   */
  public void sendNotificationsForBranch(Long branchId) {
    logger.debug("sendNotificationsForBranch, id: {}", branchId);
    Branch branch = branchRepository.findById(branchId).orElse(null);
    sendNotificationsForBranch(branch);
  }

  /**
   * If the branch was created more that X ({@link
   * #scheduleMissingScreenshotNotificationsForBranch}) minutes ago and screenshot are still
   * missing, send a notification.
   *
   * @param branchId
   */
  public void sendMissingScreenshotNotificationForBranch(Long branchId, String notifierId) {
    logger.debug("sendMissingScreenshotNotificationForBranch, id: {}", branchId);
    BranchNotificationMessageSender branchNotificationMessageSender = findByNotifierId(notifierId);
    sendMissingScreenshotNotificationForBranchWithSender(branchNotificationMessageSender, branchId);
  }

  BranchNotificationMessageSender findByNotifierId(String notifierId) {
    final BranchNotificationMessageSender branchNotificationMessageSender =
        branchNotificationMessageSenders.getById(notifierId);

    if (branchNotificationMessageSender == null) {
      throw new RuntimeException("Can't find notifier for id: " + notifierId);
    }

    return branchNotificationMessageSender;
  }

  void sendMissingScreenshotNotificationForBranchWithSender(
      BranchNotificationMessageSender branchNotificationMessageSender, Long branchId) {
    logger.debug("sendMissingScreenshotNotificationForBranch, id: {}", branchId);
    Branch branch = branchRepository.findById(branchId).orElse(null);
    sendMissingScreenshotNotificationsForBranch(branchNotificationMessageSender, branch);
  }

  void sendNotificationsForBranch(Branch branch) {
    Preconditions.checkNotNull(branch);
    Preconditions.checkNotNull(branch.getNotifiers());

    logger.debug("sendNotificationsForBranch: {} ({})", branch.getId(), branch.getName());

    BranchNotificationInfo branchNotificationInfo = getBranchNotificationInfo(branch);

    List<Exception> exceptions = new ArrayList<>();
    branch.getNotifiers().stream()
        .map(
            notifierId -> {
              BranchNotificationMessageSender messageSender =
                  branchNotificationMessageSenders.getById(notifierId);
              if (messageSender == null) {
                throw new RuntimeException(
                    "Can't get BranchNotificationMessageSender for id: " + notifierId);
              }
              return messageSender;
            })
        .forEach(
            branchNotificationMessageSender -> {
              try {
                sendNotificationsForBranchWithSender(
                    branchNotificationMessageSender, branch, branchNotificationInfo);
              } catch (Exception e) {
                logger.error("Fail safe, error tracking is up to each sender", e);
                exceptions.add(e);
              }
            });

    if (!exceptions.isEmpty()) {
      RuntimeException aggregatedException =
          new RuntimeException("One or more exceptions occurred while sending notifications.");
      exceptions.forEach(aggregatedException::addSuppressed);
      throw aggregatedException;
    }
  }

  void sendNotificationsForBranchWithSender(
      BranchNotificationMessageSender branchNotificationMessageSender,
      Branch branch,
      BranchNotificationInfo branchNotificationInfo) {
    // Check if the username for the Slack notification is in the block list

    String username = getUsername(branch);
    if (Strings.isNullOrEmpty(username)
        || !branchNotificationMessageSender.isUserAllowed(username)) {
      return;
    }

    String notifierId = branchNotificationMessageSender.getId();

    logger.debug(
        "sendNotificationsForBranch: {} ({} with sender: {})",
        branch.getId(),
        branch.getName(),
        notifierId);
    BranchNotification branchNotification = getOrCreateBranchNotification(branch, notifierId);

    if (shouldSendNewMessage(branchNotification, branchNotificationInfo)) {
      sendNewMessage(
          branchNotificationMessageSender, branch, branchNotification, branchNotificationInfo);
      scheduleMissingScreenshotNotificationsForBranch(branch, notifierId);
    } else if (shouldSendUpdatedMessage(branchNotification, branchNotificationInfo)) {
      sendUpdatedMessage(
          branchNotificationMessageSender, branch, branchNotification, branchNotificationInfo);
      scheduleMissingScreenshotNotificationsForBranch(branch, notifierId);
    }

    handleSendBranchTranslatedMessage(branchNotificationMessageSender, branch, branchNotification);
  }

  private void handleSendBranchTranslatedMessage(
      BranchNotificationMessageSender branchNotificationMessageSender,
      Branch branch,
      BranchNotification branchNotification) {

    if (!shouldSendTranslatedMessage(branch, branchNotification)) {
      // Already sent a notification or the branch is not translated yet, exit early
      return;
    }

    Optional<BranchMergeTarget> branchMergeTargetOptional =
        branchMergeTargetRepository.findByBranch(branch);
    BranchStatistic branchStatistic = branchStatisticRepository.findByBranch(branch);

    if (isNotTargetingMainBranch(branchMergeTargetOptional)) {
      // Not tracked for safe i18n, send old notification
      sendTranslatedMessage(branchNotificationMessageSender, branch, branchNotification, null);
      return;
    }

    // Safe to use orElseThrow here as negating isNotTargetingMainBranch ensures it exists
    BranchMergeTarget branchMergeTarget = branchMergeTargetOptional.orElseThrow();

    if (hasExceededSafeTranslationNotificationTimeout(branchMergeTarget, branchStatistic)) {
      // Branch has been translated for the configured amount of time and has not been checked into
      // the repo, use the old flow to unblock
      Repository repository = repositoryRepository.findById(branch.getRepository().getId()).get();

      meterRegistry
          .counter(
              "BranchNotificationService.handleSendBranchTranslatedMessage.branchExceededSafeTranslationNotificationWindow",
              Tags.of("repository", repository.getName()))
          .increment();

      logger.warn(
          "Branch '{}' has exceeded the safe I18N check in window. Falling back to the old notification flow to unblock.",
          branch.getName());

      sendTranslatedMessage(branchNotificationMessageSender, branch, branchNotification, null);
      return;
    }

    if (isCheckedInToTargetBranch(branchMergeTarget)) {
      // Branch is targeting main and is checked in, safe to notify the user / PR the
      // translations have arrived safely
      sendTranslatedMessage(
          branchNotificationMessageSender,
          branch,
          branchNotification,
          branchMergeTarget.getCommit().getName());
    }
  }

  /**
   * Schedule to check/send notification for missing screenshot in 30 minutes from now.
   *
   * @param branch
   * @param notifierId
   */
  void scheduleMissingScreenshotNotificationsForBranch(Branch branch, String notifierId) {
    Date date = JSR310Migration.dateTimeToDate(ZonedDateTime.now().plusMinutes(30));

    BranchNotificationMissingScreenshotsJobInput branchNotificationMissingScreenshotsJobInput =
        new BranchNotificationMissingScreenshotsJobInput();
    branchNotificationMissingScreenshotsJobInput.setBranchId(branch.getId());
    branchNotificationMissingScreenshotsJobInput.setNotifierId(notifierId);

    QuartzJobInfo<BranchNotificationMissingScreenshotsJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(BranchNotificationMissingScreenshotsJob.class)
            .withInput(branchNotificationMissingScreenshotsJobInput)
            .withTriggerStartDate(date)
            .withUniqueId(notifierId + "_" + branch.getId())
            .withScheduler(schedulerName)
            .build();
    quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  void sendMissingScreenshotNotificationsForBranch(
      BranchNotificationMessageSender branchNotificationMessageSender, Branch branch) {
    logger.debug(
        "sendMissingScreenshotNotificationForBranch: {} ({})", branch.getId(), branch.getName());
    BranchNotification branchNotification =
        getOrCreateBranchNotification(branch, branchNotificationMessageSender.getId());

    if (shouldSendScreenshotMissingMessage(branch, branchNotification)) {
      sendScreenshotMissingMessage(branchNotificationMessageSender, branch, branchNotification);
    }
  }

  void sendNewMessage(
      BranchNotificationMessageSender branchNotificationMessageSender,
      Branch branch,
      BranchNotification branchNotification,
      BranchNotificationInfo branchNotificationInfo) {

    logger.debug("sendNewMessage for: {}", branch.getName());
    try {
      String messageId =
          branchNotificationMessageSender.sendNewMessage(
              branch.getName(), getUsername(branch), branchNotificationInfo.getSourceStrings());

      branchNotification.setNewMsgSentAt(dateTimeUtils.now());
      branchNotification.setMessageId(messageId);
      branchNotification.setContentMD5(branchNotificationInfo.getContentMd5());
      branchNotificationRepository.save(branchNotification);
    } catch (BranchNotificationMessageSenderException mse) {
      logger.error(
          "Can't send new message with notifier: {} for branch: {}",
          branch.getName(),
          branchNotificationMessageSender.getClass().getName(),
          mse);
      throw new RuntimeException(mse);
    }
  }

  void sendUpdatedMessage(
      BranchNotificationMessageSender branchNotificationMessageSender,
      Branch branch,
      BranchNotification branchNotification,
      BranchNotificationInfo branchNotificationInfo) {

    logger.debug("sendUpdatedMessage for: {}", branch.getName());
    try {
      String messageId =
          branchNotificationMessageSender.sendUpdatedMessage(
              branch.getName(),
              getUsername(branch),
              branchNotification.getMessageId(),
              branchNotificationInfo.getSourceStrings());

      branchNotification.setUpdatedMsgSentAt(dateTimeUtils.now());
      branchNotification.setTranslatedMsgSentAt(null);
      branchNotification.setMessageId(messageId);
      branchNotification.setContentMD5(branchNotificationInfo.getContentMd5());
      branchNotificationRepository.save(branchNotification);
    } catch (BranchNotificationMessageSenderException mse) {
      logger.error("Can't send updated message", mse);
      throw new RuntimeException(mse);
    }
  }

  void sendTranslatedMessage(
      BranchNotificationMessageSender branchNotificationMessageSender,
      Branch branch,
      BranchNotification branchNotification,
      String safeI18NCommit) {
    logger.debug("sendTranslatedMessage for: {}", branch.getName());

    try {
      branchNotificationMessageSender.sendTranslatedMessage(
          branch.getName(), getUsername(branch), branchNotification.getMessageId(), safeI18NCommit);

      branchNotification.setTranslatedMsgSentAt(dateTimeUtils.now());
      branchNotificationRepository.save(branchNotification);
    } catch (BranchNotificationMessageSenderException mse) {
      logger.error("Can't send translated message", mse);
      throw new RuntimeException(mse);
    }
  }

  void sendScreenshotMissingMessage(
      BranchNotificationMessageSender branchNotificationMessageSender,
      Branch branch,
      BranchNotification branchNotification) {
    logger.debug("sendScreenshotMissingMessage for: {}", branch.getName());

    try {
      branchNotificationMessageSender.sendScreenshotMissingMessage(
          branch.getName(), branchNotification.getMessageId(), getUsername(branch));
      branchNotification.setScreenshotMissingMsgSentAt(dateTimeUtils.now());
      branchNotificationRepository.save(branchNotification);
    } catch (BranchNotificationMessageSenderException mse) {
      logger.error("Can't send screenshot missing message", mse);
      throw new RuntimeException(mse);
    }
  }

  boolean shouldSendNewMessage(
      BranchNotification branchNotification, BranchNotificationInfo branchNotificationInfo) {
    return branchNotification.getNewMsgSentAt() == null
        && !branchNotificationInfo.getSourceStrings().isEmpty();
  }

  boolean shouldSendUpdatedMessage(
      BranchNotification branchNotification, BranchNotificationInfo branchNotificationInfo) {
    return branchNotification.getNewMsgSentAt() != null
        && !branchNotificationInfo.getSourceStrings().isEmpty()
        && !branchNotificationInfo.getContentMd5().equals(branchNotification.getContentMD5());
  }

  boolean shouldSendScreenshotMissingMessage(Branch branch, BranchNotification branchNotification) {
    return branchNotification.getScreenshotMissingMsgSentAt() == null
        && isScreenshotMissing(branch);
  }

  boolean isScreenshotMissing(Branch branch) {
    long numberOfTmTextUnitInBranch =
        branchTextUnitStatisticRepository.countTmTextUnitIds(branch.getId());

    long numberOfTmTextUnitWithScreenshot =
        branch.getScreenshots().stream()
            .map(Screenshot::getScreenshotTextUnits)
            .flatMap(Collection::stream)
            .map(screenshotTextUnit -> screenshotTextUnit.getTmTextUnit().getId())
            .distinct()
            .count();

    return numberOfTmTextUnitWithScreenshot < numberOfTmTextUnitInBranch;
  }

  boolean shouldSendTranslatedMessage(Branch branch, BranchNotification branchNotification) {
    return branchNotification.getNewMsgSentAt() != null
        && branchNotification.getTranslatedMsgSentAt() == null
        && isBranchTranslated(branch);
  }

  boolean isBranchTranslated(Branch branch) {
    BranchStatistic branchStatistic = branchStatisticRepository.findByBranch(branch);
    return branchStatistic != null
        && branchStatistic.getTotalCount() > 0
        && branchStatistic.getForTranslationCount() == 0;
  }

  String getUsername(Branch branch) {
    return branch.getCreatedByUser() == null ? "" : branch.getCreatedByUser().getUsername();
  }

  String computeMD5(List<TextUnitDTO> textUnitDTOS) {
    String joined =
        textUnitDTOS.stream()
            .map(t -> Joiner.on("#").join(t.getName(), t.getSource(), t.getTarget()))
            .collect(Collectors.joining());

    return DigestUtils.md5Hex(joined);
  }

  List<String> getSourceStrings(Branch branch, List<TextUnitDTO> textUnitDTOsForBranch) {

    return textUnitDTOsForBranch.stream()
        .filter(
            textUnitDTO -> {
              return textUnitDTO.getPluralForm() == null
                  || "other".equals(textUnitDTO.getPluralForm());
            })
        .map(TextUnitDTO::getSource)
        .collect(Collectors.toList());
  }

  BranchNotificationInfo getBranchNotificationInfo(Branch branch) {

    BranchNotificationInfo branchNotificationInfo = new BranchNotificationInfo();

    List<TextUnitDTO> textUnitDTOsForBranch =
        branchStatisticService.getTextUnitDTOsForBranch(branch);
    List<String> sourceStrings = getSourceStrings(branch, textUnitDTOsForBranch);

    branchNotificationInfo.setSourceStrings(sourceStrings);
    branchNotificationInfo.setContentMd5(computeMD5(textUnitDTOsForBranch));

    return branchNotificationInfo;
  }

  BranchNotification getOrCreateBranchNotification(Branch branch, String notifierId) {
    logger.debug("getOrCreateBranchNotification for branch: {}", branch.getId());

    BranchNotification branchNotification =
        branchNotificationRepository.findByBranchAndNotifierId(branch, notifierId);

    if (branchNotification == null) {
      logger.debug("No branchNotification, create one for branch: {}", branch.getId());
      branchNotification = new BranchNotification();
      branchNotification.setBranch(branch);
      branchNotification.setNotifierId(notifierId);
      branchNotification = branchNotificationRepository.save(branchNotification);
    }
    return branchNotification;
  }

  private boolean isNotTargetingMainBranch(Optional<BranchMergeTarget> branchMergeTargetOptional) {
    return branchMergeTargetOptional.isEmpty() || !branchMergeTargetOptional.get().isTargetsMain();
  }

  private boolean hasExceededSafeTranslationNotificationTimeout(
      BranchMergeTarget branchMergeTarget, BranchStatistic branchStatistic) {
    Duration translatedDuration =
        Duration.between(branchStatistic.getTranslatedDate(), ZonedDateTime.now());
    return branchMergeTarget.getCommit() == null
        && branchStatistic.getTranslatedDate() != null
        && translatedDuration.compareTo(notificationFallbackTimeout) > 0;
  }

  private boolean isCheckedInToTargetBranch(BranchMergeTarget branchMergeTarget) {
    return branchMergeTarget.getCommit() != null;
  }
}
