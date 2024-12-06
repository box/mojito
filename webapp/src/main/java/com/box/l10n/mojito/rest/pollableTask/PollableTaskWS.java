package com.box.l10n.mojito.rest.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.pollableTask.PollableTaskBlobStorage;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.tm.TMXliffRepository;
import io.swagger.v3.oas.annotations.Operation;
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

  @Autowired PollableTaskService pollableTaskService;

  @Autowired TMXliffRepository tmXliffRepository;

  @Autowired PollableTaskBlobStorage pollableTaskBlobStorage;

  /**
   * Gets a {@link PollableTask} by id.
   *
   * @param pollableTaskId
   * @return
   */
  @Operation(summary = "Get a Pollable Task by ID")
  @RequestMapping(method = RequestMethod.GET, value = "/api/pollableTasks/{pollableTaskId}")
  public PollableTask getPollableTaskById(@PathVariable Long pollableTaskId) {
    return pollableTaskService.getPollableTask(pollableTaskId);
  }

  @Operation(summary = "Get output JSON for a Pollable Task")
  @RequestMapping(method = RequestMethod.GET, value = "/api/pollableTasks/{pollableTaskId}/output")
  public String getPollableTaskOutput(@PathVariable Long pollableTaskId) {
    String outputJson = pollableTaskBlobStorage.getOutputJson(pollableTaskId);
    return outputJson;
  }

  @Operation(summary = "Get input JSON for a Pollable Task")
  @RequestMapping(method = RequestMethod.GET, value = "/api/pollableTasks/{pollableTaskId}/input")
  public String getPollableTaskInput(@PathVariable Long pollableTaskId) {
    String inputJson = pollableTaskBlobStorage.getInputJson(pollableTaskId);
    return inputJson;
  }
}
