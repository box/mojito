package com.box.l10n.mojito.quartz;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskExecutionException;
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
        return get(PollableTaskService.NO_TIMEOUT);
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long milisecondeTimeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
        return get(milisecondeTimeout);
    }

    T get(long milisecondTimeout) throws InterruptedException, ExecutionException {
        try {
            pollableTaskService.waitForPollableTask(pollableTask.getId(), milisecondTimeout, 100);
        } catch (PollableTaskExecutionException e) {
            throw new ExecutionException(e);
        }
        return null;
    }

    @Override
    public PollableTask getPollableTask() {
        return pollableTask;
    }
}
