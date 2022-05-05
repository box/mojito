package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Implementation of {@link PollableFuture} that extends {@link FutureTask} to
 * perform asynchronous method executing using an {@link Executor} and keeping
 * track of the state in a {@link PollableTask}.
 *
 * @author jaurambault
 */
class PollableFutureTask<T> extends FutureTask<T> implements PollableFuture<T> {

    PollableTask pollableTask;

    /**
     * Private ctor to prevent creating an instance without properly link it
     * into the {@link PollableCallable}.
     *
     * @param callable
     * @param pollableTask
     */
    private PollableFutureTask(PollableCallable pollableCallable, PollableTask pollableTask) {
        super(pollableCallable);
        this.pollableTask = pollableTask;
    }

    /**
     * Creates an instance.
     *
     * @param <T> function return type
     * @param pollableTask for function execution tracking
     * @param pjp contains the method to be instrumented
     * @return the result of the function execution
     */
    public static <T> PollableFutureTask<T> create(final PollableTask pollableTask, final ProceedingJoinPoint pjp) {
        PollableCallable pollableCallable = new PollableCallable(pollableTask, pjp);
        PollableFutureTask<T> pollableFutureTask = new PollableFutureTask<>(pollableCallable, pollableTask);
        pollableCallable.setPollableFutureTask(pollableFutureTask);
        return pollableFutureTask;
    }

    @Override
    public PollableTask getPollableTask() {
        return pollableTask;
    }

    public void setPollableTask(PollableTask pollableTask) {
        this.pollableTask = pollableTask;
    }

}
