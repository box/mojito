package com.box.l10n.mojito.service.pollableTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author aloison
 */
@Profile("!disablescheduling")
@Component
public class PollableTaskCleanupScheduler {

    @Autowired
    PollableTaskCleanupService pollableTaskCleanupService;

    /**
     * @see PollableTaskCleanupService#finishZombieTasksWithError()
     * It is triggered every 30 seconds (= 30,000 milliseconds).
     */
    @Scheduled(fixedDelay = 30000)
    private void scheduleFinishZombieTasksWithError() {
        pollableTaskCleanupService.finishZombieTasksWithError();
    }
}
