package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.aspect.util.AspectJUtils;
import com.box.l10n.mojito.entity.PollableTask;
import java.util.List;
import java.util.concurrent.Callable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * See {@link PollableAspect}.
 * <p>
 * Contains the logic to call the instrumented function and record its state
 * when finished. Implementing {@link Callable} to potentially execute the
 * function asynchronously.
  *
 * @author jaurambault
 */
@Configurable
public class PollableCallable implements Callable {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PollableCallable.class);

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    AspectJUtils aspectJUtils;

    PollableTask pollableTask;
    ProceedingJoinPoint pjp;

    PollableFutureTask pollableFutureTask;

    public PollableCallable(PollableTask pollableTask, ProceedingJoinPoint pjp) {
        this.pollableTask = pollableTask;
        this.pjp = pjp;
    }

    @Override
    public Object call() throws Exception {

        ExceptionHolder exceptionHolder = new ExceptionHolder(pollableTask);
        PollableFuture pollableFuture = new PollableFutureTaskResult();

        try {
            Object proceed = pjp.proceed(getInjectedArgs(pjp, pollableTask));

            if (proceed instanceof PollableFuture) {
                pollableFuture = (PollableFuture) proceed;
            } else {
                ((PollableFutureTaskResult) pollableFuture).setResult(proceed);
            }

        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                logger.error("Unexpected error happened while executing the task "
                        + "(if the error is known to happen it should be caught and wrapped "
                        + "into an checked exception to stop logging it as an error)", t);
                exceptionHolder.setExpected(false);
                exceptionHolder.setException((Exception) t);
            } else if (t instanceof Exception) {
                logger.debug("Error happened during task execution", t);
                exceptionHolder.setExpected(true);
                exceptionHolder.setException((Exception) t);
            } else {
                String msg = "A throwable was thrown while executing the task, this is most likely a severe issue.";
                logger.error(msg, t);
                exceptionHolder.setExpected(false);
                exceptionHolder.setException(new Exception(msg, t));
            }

            throw exceptionHolder.getException();
        } finally {

            pollableTask = pollableTaskService.finishTask(
                    pollableTask.getId(),
                    getMessageOverride(pollableFuture),
                    exceptionHolder,
                    getExpectedSubTaskNumberOverride(pollableFuture));

            pollableFutureTask.setPollableTask(pollableTask);
        }

        return pollableFuture.get();
    }

    /**
     * Gets the message override value if the {@link PollableFuture} is an
     * instance of {@link PollableFutureTaskResult}.
     *
     * @param pollableFuture to extract the message override from
     * @return the message override or {@code null} if no message override or if
     * the pollableFuture is not an instance of {@link PollableFutureTaskResult}
     */
    private String getMessageOverride(PollableFuture pollableFuture) {
        String message = null;

        if (pollableFuture instanceof PollableFutureTaskResult) {
            message = ((PollableFutureTaskResult) pollableFuture).getMessageOverride();
        }

        return message;
    }

    /**
     * Gets the excepted sub task number override value if the
     * {@link PollableFuture} is an instance of
     * {@link PollableFutureTaskResult}.
     *
     * @param pollableFuture to extract the message override from
     * @return the expected sub task number override or {@code null} if no
     * message override or if the pollableFuture is not an instance of
     * {@link PollableFutureTaskResult}
     */
    private Integer getExpectedSubTaskNumberOverride(PollableFuture pollableFuture) {
        Integer expectedSubTaskNumberOverride = null;

        if (pollableFuture instanceof PollableFutureTaskResult) {
            expectedSubTaskNumberOverride = ((PollableFutureTaskResult) pollableFuture).getExpectedSubTaskNumberOverride();
        }

        return expectedSubTaskNumberOverride;
    }

    /**
     * Gets the injected method arguments.
     *
     * Any argument annotated with {@link InjectCurrentTask} will be substituted
     * by an instance of the provided {@link PollableTask}
     *
     * @param pjp contains the args to be processed
     * @param pollableTask task to be injected
     * @return the inject method arguments
     */
    private Object[] getInjectedArgs(ProceedingJoinPoint pjp, PollableTask pollableTask) {

        Object[] args = pjp.getArgs();

        List<AnnotatedMethodParam<InjectCurrentTask>> findAnnotatedMethodParams = aspectJUtils.findAnnotatedMethodParams(pjp, InjectCurrentTask.class);

        for (AnnotatedMethodParam<InjectCurrentTask> annotatedMethodParam : findAnnotatedMethodParams) {
            args[annotatedMethodParam.getIndex()] = pollableTask;
        }

        return args;
    }

    void setPollableFutureTask(PollableFutureTask pollableFutureTask) {
        this.pollableFutureTask = pollableFutureTask;
    }

}
