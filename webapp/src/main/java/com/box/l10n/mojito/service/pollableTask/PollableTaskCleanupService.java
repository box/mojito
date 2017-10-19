package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author aloison
 */
@Service
public class PollableTaskCleanupService {

    /**
     * logger
     */
    static Logger logger = getLogger(PollableTaskCleanupService.class);

    @Autowired
    PollableTaskRepository pollableTaskRepository;

    @Autowired
    PollableTaskService pollableTaskService;

    /**
     * Marks zombie tasks as finished with error.
     * A zombie task can be defined as a task that did not complete before its given timeout period.
     */
    public void finishZombieTasksWithError() {

        List<PollableTask> zombiePollableTasks;

        do {
            logger.debug("Fetching 5 zombie pollable tasks to clean up");

            // Fetching 5 by 5 to avoid locking too many rows.
            // It is also useful to distribute the load across multiple instances.
            PageRequest pageable = new PageRequest(0, 5);
            zombiePollableTasks = pollableTaskRepository.findZombiePollableTasks(pageable);

            for (PollableTask zombiePollableTask : zombiePollableTasks) {
                markAsFinishedWithError(zombiePollableTask);
            }
        } while (!zombiePollableTasks.isEmpty());
    }

    /**
     * Sets the pollable task's state to "Finished" and adds an error message.
     *
     * @param pollableTask
     */
    private void markAsFinishedWithError(PollableTask pollableTask) {

        logger.debug("Zombie task detected: {}, mark as finished with error", pollableTask.getId());
        ExceptionHolder exceptionHolder = new ExceptionHolder(pollableTask);
        exceptionHolder.setExpected(true);
        exceptionHolder.setException(new PollableTaskTimeoutException("Zombie task detected: Maximum execution time exceeded."));

        pollableTaskService.finishTask(pollableTask.getId(), null, exceptionHolder, null);
    }
}
