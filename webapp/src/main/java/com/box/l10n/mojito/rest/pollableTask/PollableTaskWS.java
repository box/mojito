package com.box.l10n.mojito.rest.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * WS to get access to {@link PollableTask}s
 *
 * @author Jean
 */
@RestController
public class PollableTaskWS {

    @Autowired
    PollableTaskService pollableTaskService;

    /**
     * Gets a {@link PollableTask} by id.
     *
     * @param pollableTaskId
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/api/pollableTasks/{pollableTaskId}")
    public PollableTask getPollableTaskById(@PathVariable Long pollableTaskId) {
        return pollableTaskService.getPollableTask(pollableTaskId);
    }
}
