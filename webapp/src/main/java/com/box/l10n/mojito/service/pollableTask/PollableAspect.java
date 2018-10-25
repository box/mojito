package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareError;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Aspect to make a function execution "Pollable". Wraps the function to keep
 * track of it's execution and record the state in the {@link PollableTask}
 * entity.
 *
 * <p>
 * The function execution can be synchronous or asynchronous.
 * <p>
 * To get access to the {@link PollableTask} entity that was created, the
 * instrumented function can return a {@link PollableFuture}.
 * <p>
 * {@link PollableFuture} is an extension of {@link Future} and behave in a
 * similar way to retrieve results and for exception handling.
 * <p>
 * Asynchronous function must use void or a {@link PollableFuture} as return
 * type.
 * <p>
 * Synchronous function can return any type. If an exception occurs it will be
 * thrown as usual, except when returning {@link PollableFuture}.
 * <p>
 * For synchronous function that returns {@link PollableFuture}, retrieving
 * result and exception handling becomes the same as for an asynchronous
 * function (exception will be thrown only when retrieving the result).
 *
 * @author jaurambault
 */
@Aspect
public class PollableAspect {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PollableAspect.class);

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    @Qualifier("pollableTaskExecutor")
    AsyncTaskExecutor pollableTaskExecutor;

    @Around("methods()")
    @SuppressWarnings("FinallyDiscardsException")
    public Object createPollableWrapper(ProceedingJoinPoint pjp) throws Throwable {

        Object returnedValue;

        PollableAspectParameters pollableAspectParameters = new PollableAspectParameters(pjp);

        logger.debug("Create the PollableTask to keep track of method: {} execution", pollableAspectParameters.getName());
        PollableTask pollableTask = pollableTaskService.createPollableTask(
                pollableAspectParameters.getParentId(),
                pollableAspectParameters.getName(),
                pollableAspectParameters.getMessage(),
                pollableAspectParameters.getExpectedSubTaskNumber(),
                pollableAspectParameters.getTimeout());

        logger.debug("Create the PollableFutureTask that will hold the method result and Pollable instance");
        PollableFutureTask pollableFuture = PollableFutureTask.create(pollableTask, pjp);

        if (pollableAspectParameters.isAsync()) {
            returnedValue = asyncExecute(pollableFuture, getFunctionReturnType(pjp));
        } else {
            returnedValue = syncExecute(pollableFuture, getFunctionReturnType(pjp));
        }

        return returnedValue;
    }

    /**
     * Executes the instrumented function synchronously.
     *
     * <p>
     * Functions that are executed synchronously can return any type. That
     * includes void and {@link PollableFuture}.
     *
     * <p>
     * If the return type is {@link PollableFuture}, the result is simply
     * returned (no exception will be thrown even though the instrumented
     * function might have failed).
     *
     * <p>
     * If the return type is not {@link PollableFuture}, the result is fetched
     * via {@link PollableFutureTask#get()}. This can throw an
     * {@link ExecutionException} that contains the original exception thrown by
     * the instrumented function. In that case, the cause is unwrapped and
     * re-thrown by this function.
     *
     * @param pollableFuture contains the logic to be executed
     * @param functionReturnType function return type used to extract the result
     *
     * @return the object returned by the instrumented function
     * @throws Throwable If the return type is not {@link PollableFuture}, the
     * error thrown by the instrumented function
     */
    private Object syncExecute(PollableFutureTask pollableFuture, Class functionReturnType) throws Throwable {

        Object returnedValue = null;

        logger.debug("Execute the method synchronously");
        pollableFuture.run();

        if (PollableFuture.class.isAssignableFrom(functionReturnType)) {
            logger.debug("Sync method with PollableFuture return type, return the PollableFuture instance (exception not thrown)");
            returnedValue = pollableFuture;
        } else {
            logger.debug("Sync method without PollableFuture, return the result from the pollableFuture (potentially throws exceptions)");
            try {
                returnedValue = pollableFuture.get();
            } catch (ExecutionException ee) {
                throw ee.getCause();
            }
        }

        return returnedValue;
    }

    /**
     * Executes the instrumented function asynchronously.
     *
     * <p>
     * Function that are executed asynchronously can return void or
     * {@link PollableFuture}.
     *
     * <p>
     * The result of the instrumented function can be retrieved via the
     * {@link PollableFuture#get() }.
     *
     * @param pollableFuture contains the logic to be executed
     * @param functionReturnType function return type used to extract the result
     *
     * @return the pollableFuture passed as input
     * @throws RuntimeException if the instrumented function does return a
     * proper type
     */
    private Object asyncExecute(PollableFutureTask pollableFuture, Class functionReturnType) throws RuntimeException {

        logger.debug("Check the return type for async execution");

        if (!PollableFuture.class.isAssignableFrom(functionReturnType) && !Void.TYPE.isAssignableFrom(functionReturnType)) {
            String msg = "@Pollable(async = \"true\") must be placed on a method that returns void or PollableFuture";
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        logger.debug("Execute the method asynchronously");
        pollableTaskExecutor.submit(pollableFuture);

        logger.debug("Async method, return the PollableFuture instance (void is ignored)");
        return pollableFuture;
    }

    /**
     * Gets the return type of the instrumented method.
     *
     * @param pjp contains the instrumented method
     * @return the return type of the instrumented method.
     */
    private Class getFunctionReturnType(ProceedingJoinPoint pjp) {
        logger.debug("Get the return type of the instrumented method");
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        return methodSignature.getReturnType();
    }

    @Pointcut("execution(@Pollable * *(..))")
    private void methods() {
    }

    @DeclareError("execution(!@Pollable * *(.., @ParentTask (*),..))")
    private static final String parentTaskShouldBeOnPollable = "@ParentTask should be applied on methods annotated with @Pollable";

    @DeclareError("execution(!@Pollable * *(.., @InjectCurrentTask (*),..))")
    private static final String injectTaskShouldBeOnPollable = "@InjectCurrentTask should be applied on methods annotated with @Pollable";

    @DeclareError("execution(!@Pollable * *(.., @MsgArg (*),..))")
    private static final String msgArgTaskShouldBeOnPollable = "@MsgArg should be applied on methods annotated with @Pollable";

}
