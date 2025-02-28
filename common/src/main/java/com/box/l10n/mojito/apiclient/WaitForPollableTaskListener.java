package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.model.PollableTask;

/**
 * Listener to be passed to {@link
 * com.box.l10n.mojito.apiclient.PollableTaskClient#waitForPollableTask(Long, long,
 * com.box.l10n.mojito.apiclient.WaitForPollableTaskListener) } to react to polling process.
 *
 * @author jaurambault
 */
public interface WaitForPollableTaskListener {
  /**
   * Called after each poll
   *
   * @param pollableTask the {@link PollableTask} that was just retrieved
   */
  void afterPoll(PollableTask pollableTask);
}
