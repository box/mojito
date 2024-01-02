package com.box.l10n.mojito.service.pollableTask;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** @author aloison */
public class PollableTaskCleanupServiceTest extends ServiceTestBase {

  static Logger logger = getLogger(PollableTaskCleanupServiceTest.class);

  @Autowired PollableTaskService pollableTaskService;

  @Autowired PollableTaskCleanupService pollableTaskCleanupService;

  @Autowired PollableTaskRepository pollableTaskRepository;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Before
  public void finishAllPollableTasks() {
    List<PollableTask> pollableTasks = pollableTaskRepository.findAll();
    for (PollableTask pollableTask : pollableTasks) {
      pollableTask.setFinishedDate(JSR310Migration.newDateTimeEmptyCtor());
    }
    pollableTaskRepository.saveAll(pollableTasks);
  }

  @Test
  public void testMarkZombieTasksAsFinishedWithErrorWithZombies() throws Exception {

    PollableTask pollableTask =
        pollableTaskService.createPollableTask(null, "test-pollable", null, 0, 1);
    PollableTask pollableTaskInPast = setPollableTaskCreatedDateInPast(pollableTask);
    assertFalse(isMarkedAsZombie(pollableTaskInPast));

    pollableTaskCleanupService.finishZombieTasksWithError();

    PollableTask pollableTaskAfterCleanup =
        pollableTaskService.getPollableTask(pollableTaskInPast.getId());
    try {
      assertTrue(isMarkedAsZombie(pollableTaskAfterCleanup));
    } catch (AssertionError ae) {
      logger.error("Make sure the server is configure in UTC");
      throw ae;
    }
  }

  @Transactional
  private PollableTask setPollableTaskCreatedDateInPast(PollableTask pollableTask) {
    ZonedDateTime pastCreatedDate = (pollableTask.getCreatedDate()).minusHours(2);
    pollableTask.setCreatedDate(pastCreatedDate);
    pollableTaskRepository.save(pollableTask);

    return pollableTaskRepository.findById(pollableTask.getId()).orElse(null);
  }

  private boolean isMarkedAsZombie(PollableTask pollableTask) {
    return pollableTask.getFinishedDate() != null
        && pollableTask.getErrorMessage() != null
        && pollableTask.getErrorStack().contains("Zombie task detected");
  }

  @Test
  public void testMarkZombieTasksAsFinishedWithErrorWithoutZombies() throws Exception {

    PollableTask pollableTask =
        pollableTaskService.createPollableTask(null, "test-pollable", null, 0);
    pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    assertFalse(isMarkedAsZombie(pollableTask));

    pollableTaskCleanupService.finishZombieTasksWithError();

    PollableTask pollableTaskAfterCleanup =
        pollableTaskService.getPollableTask(pollableTask.getId());
    assertFalse(isMarkedAsZombie(pollableTaskAfterCleanup));
  }
}
