package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.google.common.base.Throwables;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Services to manage pollable tasks.
 *
 * <p>A {@link PollableTask} is used to keep track of long running process. As the process evolves
 * it will update tasks and create new ones.
 *
 * <p>This information can be accessed by a FE to display progress message to the user.
 *
 * <p>All the operation are done in separate transaction and PollableTask should never be modified
 * in other transaction to avoid locks and preventing proper process reporting.
 *
 * @author jaurambault
 */
@Service
public class PollableTaskService {

  public static final Long NO_TIMEOUT = -1L;
  /** logger */
  static Logger logger = LoggerFactory.getLogger(PollableTaskService.class);

  @Autowired ObjectMapper objectMapper;

  @Autowired PollableTaskRepository pollableTaskRepository;

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public PollableTask getPollableTask(long id) {
    return pollableTaskRepository.findById(id).orElse(null);
  }

  public PollableTask createPollableTask(
      Long parentId, String name, String message, int expectedSubTaskNumber) {
    return createPollableTask(parentId, name, message, expectedSubTaskNumber, NO_TIMEOUT);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PollableTask createPollableTask(
      Long parentId, String name, String message, int expectedSubTaskNumber, long timeout) {

    PollableTask pollableTask = new PollableTask();

    if (parentId != null) {
      pollableTask.setParentTask(pollableTaskRepository.getOne(parentId));
    }

    pollableTask.setExpectedSubTaskNumber(expectedSubTaskNumber);
    pollableTask.setName(name);
    pollableTask.setMessage(message);

    if (timeout > 0) {
      pollableTask.setTimeout(timeout);
    }

    return pollableTaskRepository.save(pollableTask);
  }

  /**
   * Updates a task.
   *
   * @param id the task id
   * @param messageOverride the new task message if not {@code null}
   * @param exceptionHolder exception holder
   * @param expectedSubTaskNumberOverride the new expected sub task number if not {@code null}
   * @return the updated task
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PollableTask finishTask(
      long id,
      String messageOverride,
      ExceptionHolder exceptionHolder,
      Integer expectedSubTaskNumberOverride) {

    PollableTask pollableTask = getPollableTask(id);
    pollableTask.setFinishedDate(ZonedDateTime.now());

    if (exceptionHolder != null && exceptionHolder.getException() != null) {
      pollableTask.setErrorStack(Throwables.getStackTraceAsString(exceptionHolder.getException()));
      pollableTask.setErrorMessage(objectMapper.writeValueAsStringUnchecked(exceptionHolder));
    }

    if (messageOverride != null) {
      pollableTask.setMessage(messageOverride);
    }

    if (expectedSubTaskNumberOverride != null) {
      pollableTask.setExpectedSubTaskNumber(expectedSubTaskNumberOverride);
    }

    return pollableTaskRepository.save(pollableTask);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PollableTask updateExpectedSubTaskNumber(long id, int expectedSubTaskNumber) {

    PollableTask pollableTask = getPollableTask(id);
    pollableTask.setExpectedSubTaskNumber(expectedSubTaskNumber);

    return pollableTaskRepository.save(pollableTask);
  }

  /**
   * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished() }).
   * Infinite timeout.
   *
   * @param pollableId the {@link PollableTask#id}
   * @throws InterruptedException
   * @throws PollableTaskException
   */
  public PollableTask waitForPollableTask(Long pollableId)
      throws InterruptedException, PollableTaskException {
    return waitForPollableTask(pollableId, NO_TIMEOUT);
  }

  /**
   * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished() }).
   *
   * @param pollableId the {@link PollableTask#id}
   * @param timeout timeout in milliseconds.
   * @throws InterruptedException
   * @throws PollableTaskException
   */
  public PollableTask waitForPollableTask(Long pollableId, long timeout)
      throws InterruptedException, PollableTaskException {
    return waitForPollableTask(pollableId, timeout, 500);
  }

  /**
   * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished() }).
   *
   * @param pollableId the {@link PollableTask#id}
   * @param timeout timeout in milliseconds.
   * @param sleepTime time to sleep before checking the status again
   * @throws InterruptedException
   * @throws PollableTaskException
   */
  public PollableTask waitForPollableTask(Long pollableId, long timeout, long sleepTime)
      throws InterruptedException, PollableTaskException {
    return waitForPollableTasks(Arrays.asList(pollableId), timeout, sleepTime).get(0);
  }

  /**
   * Waits for a list of {@link PollableTask}s to be all finished (see {@link
   * PollableTask#isAllFinished() }).
   *
   * @param pollableIds a list of {@link PollableTask#id}
   * @param timeout timeout in milliseconds.
   * @param sleepTime time to sleep before checking the status again
   * @throws InterruptedException
   * @throws PollableTaskException
   */
  public List<PollableTask> waitForPollableTasks(
      List<Long> pollableIds, long timeout, long sleepTime)
      throws InterruptedException, PollableTaskException {

    long currentTime = System.currentTimeMillis();
    long timeoutTime = currentTime + timeout;

    List<PollableTask> pollableTasks = new ArrayList<>();

    for (Long pollableId : pollableIds) {

      boolean isAllFinished = false;

      while (!isAllFinished && (timeout == NO_TIMEOUT || currentTime <= timeoutTime)) {

        logger.debug("Waiting for PollableTask id: {} to finish", pollableId);

        PollableTask pollableTask = getPollableTask(pollableId);
        isAllFinished = pollableTask.isAllFinished();

        List<PollableTask> pollableTaskWithErrors = getAllPollableTasksWithError(pollableTask);
        if (!pollableTaskWithErrors.isEmpty()) {
          for (PollableTask pollableTaskWithError : pollableTaskWithErrors) {
            logger.error(
                "Error happened in PollableTask: {}\n{}",
                pollableTaskWithError.getId(),
                pollableTaskWithError.getErrorStack());
          }
          throw new PollableTaskExecutionException(
              "Error happened in PollableTask or sub tasks: " + pollableTask.getId());
        }

        if (!isAllFinished) {
          Thread.sleep(sleepTime);
          currentTime = System.currentTimeMillis();
        }
      }

      if (isAllFinished) {
        logger.debug("PollableTask: {} finished", pollableId);
      } else {
        logger.debug("Timed out waiting for PollableTask: {} to finished", pollableId);
        throw new PollableTaskTimeoutException("Timed out waiting for PollableTask: " + pollableId);
      }

      pollableTasks.add(getPollableTask(pollableId));
    }

    return pollableTasks;
  }

  /**
   * Get all the PollableTasks with error (traverses all the PollableTask's subtasks)
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
   * Recursively traverses all subtasks of {@code pollableTask} and return all the {@link
   * PollableTask} which had an error message
   *
   * @param pollableTask
   * @param pollableTasksWithError
   */
  private void recursivelyGetAllPollableTaskWithError(
      PollableTask pollableTask, List<PollableTask> pollableTasksWithError) {

    for (PollableTask subTask : pollableTask.getSubTasks()) {
      recursivelyGetAllPollableTaskWithError(subTask, pollableTasksWithError);
    }

    if (pollableTask.getErrorMessage() != null) {
      pollableTasksWithError.add(pollableTask);
    }
  }
}
