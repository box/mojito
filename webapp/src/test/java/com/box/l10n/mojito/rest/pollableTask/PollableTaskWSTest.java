package com.box.l10n.mojito.rest.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;


/**
 * @author jaurambault
 */
public class PollableTaskWSTest extends WSTestBase {

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    PollableTaskClient pollableTaskClient;

    @Test
    @Category(SpringBootTest.class)
    public void testGetPollableTask() throws Exception {

        String pollableTaskName = "testGetPollableTask";
        PollableTask parentTask = pollableTaskService.createPollableTask(null, pollableTaskName, null, 0);
        com.box.l10n.mojito.rest.entity.PollableTask pollableTask = pollableTaskClient.getPollableTask(parentTask.getId());

        assertEquals(pollableTaskName, pollableTask.getName());
        assertNull(pollableTask.getFinishedDate());
        assertFalse(pollableTask.isAllFinished());
    }
}
