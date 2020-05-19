package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.client.exception.PollableTaskException;
import com.box.l10n.mojito.rest.client.exception.PollableTaskExecutionException;
import com.box.l10n.mojito.rest.client.exception.PollableTaskTimeoutException;
import com.box.l10n.mojito.rest.entity.PollableTask;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author aloison
 *
 * // TODO(P1) Move into its own module
 */
@Component
public class PollableTaskClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = getLogger(PollableTaskClient.class);

    public final static Long NO_TIMEOUT = -1L;

    @Override
    public String getEntityName() {
        return "pollableTasks";
    }

    /**
     * @param pollableTaskId {@link PollableTask#id}
     * @return The {@link PollableTask} associated to the given id
     */
    public PollableTask getPollableTask(Long pollableTaskId) {
        return authenticatedRestTemplate.getForObject(getBasePathForResource(pollableTaskId), PollableTask.class);
    }

    public String getPollableTaskOutput(Long pollableTaskId) {
        String output = authenticatedRestTemplate.getForObject(getBasePathForResource(pollableTaskId, "output"), String.class);
        return output;
    }

    /**
     * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished()
     * }). Infinite timeout.
     *
     * @param pollableId the {@link PollableTask#id}
     * @throws PollableTaskException
     */
    public void waitForPollableTask(Long pollableId) throws PollableTaskException {
        waitForPollableTask(pollableId, NO_TIMEOUT);
    }

    /**
     * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished()
     * }).
     *
     * @param pollableTaskId the {@link PollableTask#id}
     * @param timeout timeout in milliseconds.
     * @throws PollableTaskException
     */
    public void waitForPollableTask(Long pollableTaskId, long timeout) throws PollableTaskException {
        waitForPollableTask(pollableTaskId, timeout, null);
    }

    /**
     * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished()
     * }).
     *
     * @param pollableId the {@link PollableTask#id}
     * @param timeout timeout in milliseconds.
     * @param waitForPollableTaskListener listener to be called during polling
     * @throws PollableTaskException
     */
    public void waitForPollableTask(Long pollableId, long timeout, WaitForPollableTaskListener waitForPollableTaskListener) throws PollableTaskException {

        long timeoutTime = System.currentTimeMillis() + timeout;
        long waitTime = 0;

        PollableTask pollableTask = null;

        while (pollableTask == null || !pollableTask.isAllFinished()) {

            logger.debug("Waiting for PollableTask: {} to finish", pollableId);

            pollableTask = getPollableTask(pollableId);

            if (waitForPollableTaskListener != null) {
                waitForPollableTaskListener.afterPoll(pollableTask);
            }

            List<PollableTask> pollableTaskWithErrors = getAllPollableTasksWithError(pollableTask);

            if (!pollableTaskWithErrors.isEmpty()) {

                for (PollableTask pollableTaskWithError : pollableTaskWithErrors) {
                    logger.debug("Error happened in PollableTask {}: {}", pollableTaskWithError.getId(), pollableTaskWithError.getErrorMessage().getMessage());
                }

                // Last task is the root task if it has an error or any of the sub task
                // TODO(P1) we might want to show all errors
                PollableTask lastTaskInError = pollableTaskWithErrors.get(pollableTaskWithErrors.size() - 1);

                throw new PollableTaskExecutionException(lastTaskInError.getErrorMessage().getMessage());
            }

            if (!pollableTask.isAllFinished()) {

                if (timeout != NO_TIMEOUT && System.currentTimeMillis() > timeoutTime) {
                    logger.debug("Timed out waiting for PollableTask: {} to finish. \n{}", pollableId, ReflectionToStringBuilder.toString(pollableTask));
                    throw new PollableTaskTimeoutException("Timed out waiting for PollableTask: " + pollableId);
                }

                try {
                    Thread.sleep(waitTime);
                    waitTime = getNextWaitTime(waitTime);
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            } else {
                logger.debug("PollableTask: {} finished", pollableId);
            }
        }
    }

    long getNextWaitTime(long lastWaitTime) {
        int maxTime = 500;
        long nextWaitTime = lastWaitTime + 25;
        nextWaitTime = Math.max(maxTime, nextWaitTime);
        return nextWaitTime;
    }

    /**
     * Get all the PollableTasks with error (traverses all the PollableTask's
     * subtasks)
     *
     * @param pollableTask
     * @return
     */
    public List<PollableTask> getAllPollableTasksWithError(PollableTask pollableTask) {
        List<PollableTask> result = new ArrayList<>();
        recursivelyGetAllPollableTaskWithError(pollableTask, result);
        return result;
    }

    /**
     * Recursively traverses all subtasks of {@code pollableTask} and return all
     * the {@link PollableTask} which had an error message
     *
     * @param pollableTask
     * @param pollableTasksWithError
     */
    private void recursivelyGetAllPollableTaskWithError(PollableTask pollableTask, List<PollableTask> pollableTasksWithError) {

        for (PollableTask subTask : pollableTask.getSubTasks()) {
            recursivelyGetAllPollableTaskWithError(subTask, pollableTasksWithError);
        }

        if (pollableTask.getErrorMessage() != null) {
            pollableTasksWithError.add(pollableTask);
        }
    }

}
