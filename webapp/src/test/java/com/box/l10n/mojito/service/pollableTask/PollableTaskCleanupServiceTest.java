package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import java.util.List;
import org.joda.time.DateTime;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author aloison
 */
public class PollableTaskCleanupServiceTest extends ServiceTestBase {

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    PollableTaskCleanupService pollableTaskCleanupService;

    @Autowired
    PollableTaskRepository pollableTaskRepository;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Before
    public void finishAllPollableTasks() {
        List<PollableTask> pollableTasks = pollableTaskRepository.findAll();
        for (PollableTask pollableTask : pollableTasks) {
            pollableTask.setFinishedDate(new DateTime());
        }
        pollableTaskRepository.save(pollableTasks);
    }

    @Test
    public void testMarkZombieTasksAsFinishedWithErrorWithZombies() throws Exception {

        PollableTask pollableTask = pollableTaskService.createPollableTask(null, "test-pollable", null, 0, 1);
        PollableTask pollableTaskInPast = setPollableTaskCreatedDateInPast(pollableTask);
        assertFalse(isMarkedAsZombie(pollableTaskInPast));

        pollableTaskCleanupService.finishZombieTasksWithError();

        PollableTask pollableTaskAfterCleanup = pollableTaskService.getPollableTask(pollableTaskInPast.getId());
        assertTrue(isMarkedAsZombie(pollableTaskAfterCleanup));
    }

    @Transactional
    private PollableTask setPollableTaskCreatedDateInPast(PollableTask pollableTask) {
        DateTime pastCreatedDate = (pollableTask.getCreatedDate()).minusHours(1);
        pollableTask.setCreatedDate(pastCreatedDate);
        pollableTaskRepository.save(pollableTask);

        return pollableTaskRepository.findOne(pollableTask.getId());
    }

    private boolean isMarkedAsZombie(PollableTask pollableTask) {
        return pollableTask.getFinishedDate() != null &&
                pollableTask.getErrorMessage() != null &&
                pollableTask.getErrorStack().contains("Zombie task detected");
    }


    @Test
    public void testMarkZombieTasksAsFinishedWithErrorWithoutZombies() throws Exception {

        PollableTask pollableTask = pollableTaskService.createPollableTask(null, "test-pollable", null, 0);
        pollableTaskService.finishTask(pollableTask.getId(), null, null, null, null);
        assertFalse(isMarkedAsZombie(pollableTask));

        pollableTaskCleanupService.finishZombieTasksWithError();

        PollableTask pollableTaskAfterCleanup = pollableTaskService.getPollableTask(pollableTask.getId());
        assertFalse(isMarkedAsZombie(pollableTaskAfterCleanup));
    }
}
