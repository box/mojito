package com.box.l10n.mojito.service.pollableTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Iterator;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class PollableTaskServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PollableTaskServiceTest.class);

  @Autowired PollableTaskService pollableTaskService;

  @Autowired ObjectMapper objectMapper;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testGetPollableTask() {

    PollableTask createPollableTask =
        pollableTaskService.createPollableTask(
            null, testIdWatcher.getEntityName("testGetPollableTask"), "a message", 0);

    PollableTask pollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertEquals(pollableTask.getId(), createPollableTask.getId());
    assertEquals(pollableTask.getName(), createPollableTask.getName());
    assertEquals("a message", createPollableTask.getMessage());
    assertEquals("\"a message\"", createPollableTask.getMessageAsJson());
    assertEquals(0, createPollableTask.getExpectedSubTaskNumber());
    assertNull(createPollableTask.getFinishedDate());
    assertFalse(createPollableTask.isAllFinished());
  }

  @Test
  public void testCreateParentTask() {
    PollableTask createPollableTask =
        pollableTaskService.createPollableTask(
            null, testIdWatcher.getEntityName("testCreateParentTask"), "a message", 15);

    PollableTask pollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertEquals(pollableTask.getId(), createPollableTask.getId());
    assertEquals(pollableTask.getName(), createPollableTask.getName());
    assertEquals("a message", createPollableTask.getMessage());
    assertEquals("\"a message\"", createPollableTask.getMessageAsJson());
    assertEquals(15, createPollableTask.getExpectedSubTaskNumber());
    assertNull(createPollableTask.getFinishedDate());
    assertFalse(createPollableTask.isAllFinished());
  }

  @Test
  public void testCreateSubTask() {

    PollableTask parent =
        pollableTaskService.createPollableTask(
            null, testIdWatcher.getEntityName("testCreateSubTask"), "a message", 2);
    PollableTask createSubTask1 =
        pollableTaskService.createPollableTask(
            parent.getId(),
            testIdWatcher.getEntityName("testCreateSubTask-Sub1"),
            "sub1 message",
            0);
    PollableTask createSubTask2 =
        pollableTaskService.createPollableTask(
            parent.getId(),
            testIdWatcher.getEntityName("testCreateSubTask-Sub2"),
            "sub2 message",
            0);

    PollableTask pollableTask = pollableTaskService.getPollableTask(parent.getId());

    Iterator<PollableTask> iterator = pollableTask.getSubTasks().iterator();
    assertEquals(createSubTask1.getId(), iterator.next().getId());
    assertEquals(createSubTask2.getId(), iterator.next().getId());

    logger.debug(objectMapper.writeValueAsStringUnchecked(pollableTask));
  }

  @Test
  public void testFinishTask() {
    PollableTask createPollableTask =
        pollableTaskService.createPollableTask(
            null, testIdWatcher.getEntityName("testFinishTask"), null, 2);
    assertNull(createPollableTask.getMessage());
    assertEquals(2, createPollableTask.getExpectedSubTaskNumber());

    String message =
        "<iframe src=\"https://app.box.com/embed_widget/s/63s3l1bv7e38c6ombwdi?view=list&sort=date&theme=blue\" width=\"500\" height=\"400\" show_parent_path=\"yes\" frameborder=\"0\" allowfullscreen webkitallowfullscreen mozallowfullscreen oallowfullscreen msallowfullscreen></iframe>";
    pollableTaskService.finishTask(createPollableTask.getId(), message, null, null);

    PollableTask pollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertEquals(message, pollableTask.getMessage());
    assertEquals(2, pollableTask.getExpectedSubTaskNumber());

    pollableTaskService.finishTask(createPollableTask.getId(), null, null, 5);

    pollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertEquals(message, pollableTask.getMessage());
    assertEquals(5, pollableTask.getExpectedSubTaskNumber());
  }

  @Test
  public void testCompoundStatusSingleTask() {

    PollableTask createParentTask =
        pollableTaskService.createPollableTask(
            null, testIdWatcher.getEntityName("testCompoundStatusSingleTask"), null, 0);
    assertFalse(createParentTask.isAllFinished());

    PollableTask finishTask =
        pollableTaskService.finishTask(createParentTask.getId(), null, null, null);
    assertTrue(finishTask.isAllFinished());
  }

  @Test
  public void testCompoundStatusWithSubTasks() {

    PollableTask createPollableTask =
        pollableTaskService.createPollableTask(
            null, testIdWatcher.getEntityName("testCompoundStatusWithSubTasks"), null, 2);
    PollableTask sub1 =
        pollableTaskService.createPollableTask(
            createPollableTask.getId(),
            testIdWatcher.getEntityName("testCompoundStatusParentWithSubTaskFinished-Sub1"),
            null,
            0);
    PollableTask sub2 =
        pollableTaskService.createPollableTask(
            createPollableTask.getId(),
            testIdWatcher.getEntityName("testCompoundStatusParentWithSubTaskFinished-Sub2"),
            null,
            0);

    createPollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertFalse(createPollableTask.isAllFinished());

    pollableTaskService.finishTask(sub1.getId(), null, null, null);
    createPollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertFalse(createPollableTask.isAllFinished());

    pollableTaskService.finishTask(createPollableTask.getId(), null, null, null);
    createPollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertFalse(createPollableTask.isAllFinished());

    pollableTaskService.finishTask(sub2.getId(), null, null, null);
    createPollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertTrue(createPollableTask.isAllFinished());
  }

  @Test
  public void testCompoundStatusParentNotFinishedBecauseOfExpectedSubtasksMissing() {
    PollableTask createPollableTask =
        pollableTaskService.createPollableTask(
            null,
            testIdWatcher.getEntityName(
                "testCompoundStatusParentNotFinishedBecauseOfExpectedSubtasksMissing"),
            null,
            1);
    pollableTaskService.finishTask(createPollableTask.getId(), null, null, null);
    createPollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertFalse(createPollableTask.isAllFinished());
  }

  @Test
  public void test3Level() {
    PollableTask createParentTask =
        pollableTaskService.createPollableTask(
            null, testIdWatcher.getEntityName("test3Level"), null, 1);
    PollableTask sub =
        pollableTaskService.createPollableTask(
            createParentTask.getId(), testIdWatcher.getEntityName("test3Level-sub"), null, 1);
    PollableTask subsub =
        pollableTaskService.createPollableTask(
            sub.getId(), testIdWatcher.getEntityName("test3Level-sub-sub"), null, 0);

    createParentTask = pollableTaskService.getPollableTask(createParentTask.getId());
    assertFalse(createParentTask.isAllFinished());

    pollableTaskService.finishTask(sub.getId(), null, null, null);
    createParentTask = pollableTaskService.getPollableTask(createParentTask.getId());
    assertFalse(createParentTask.isAllFinished());

    pollableTaskService.finishTask(createParentTask.getId(), null, null, null);
    createParentTask = pollableTaskService.getPollableTask(createParentTask.getId());
    assertFalse(createParentTask.isAllFinished());

    pollableTaskService.finishTask(subsub.getId(), null, null, null);
    createParentTask = pollableTaskService.getPollableTask(createParentTask.getId());
    assertTrue(createParentTask.isAllFinished());
  }

  @Test
  public void testUpdateExpectedSubTaskNumber() {
    PollableTask createPollableTask =
        pollableTaskService.createPollableTask(
            null, testIdWatcher.getEntityName("testUpdateExpectedSubTaskNumber"), null, 1);
    assertEquals(1, createPollableTask.getExpectedSubTaskNumber());
    pollableTaskService.updateExpectedSubTaskNumber(createPollableTask.getId(), 12);

    createPollableTask = pollableTaskService.getPollableTask(createPollableTask.getId());
    assertEquals(12, createPollableTask.getExpectedSubTaskNumber());
  }
}
