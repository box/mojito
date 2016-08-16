package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.PollableTask;

/**
 * Listener to be passed to {@link PollableTaskClient#waitForPollableTask(java.lang.Long, long, com.box.l10n.mojito.rest.client.WaitForPollableTaskListener)
 * } to react to polling process.
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
