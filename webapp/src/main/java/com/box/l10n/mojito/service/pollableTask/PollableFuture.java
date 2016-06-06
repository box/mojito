package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A PollableFuture represents the result of a function execution instrumented
 * with {@link Pollable}.
 * <p>
 * It behaves as {@link Future} and provides additional information about the
 * execution state.
 *
 * @author jaurambault
 * @param <T>
 */
public interface PollableFuture<T> {

    /**
     * See {@link Future#get() }
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public T get() throws InterruptedException, ExecutionException;

    /**
     * See {@link Future#get() }
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Gets the {@link PollableTask} that contains information about the
     * execution of the instrumented function.
     *
     * @return
     */
    public PollableTask getPollableTask();
}
