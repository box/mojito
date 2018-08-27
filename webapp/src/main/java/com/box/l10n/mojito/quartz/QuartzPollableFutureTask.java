package com.box.l10n.mojito.quartz;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Configurable
public class QuartzPollableFutureTask<T> implements PollableFuture<T> {

    @Autowired
    PollableTaskService pollableTaskService;

    PollableTask pollableTask;

    public QuartzPollableFutureTask(PollableTask pollableTask) {
        this.pollableTask = pollableTask;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        pollableTaskService.waitForPollableTask(pollableTask.getId());
        return null;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        pollableTaskService.waitForPollableTask(pollableTask.getId(), timeout);
        return null;
    }

    @Override
    public PollableTask getPollableTask() {
        return pollableTask;
    }
}
