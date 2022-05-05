package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author jaurambault
 */
public class PollableAspectTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(PollableAspectTest.class);

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    PollableTaskRepository pollableTaskRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testSimpleMethodWithResult() throws InterruptedException, ExecutionException {

        long count = pollableTaskRepository.count();

        String simpleMethodWithResult = simpleMethodWithResult();
        assertEquals("simpleMethodWithResult", simpleMethodWithResult);

        // TODO this seems sensitive to job running at the same time - is the profile to disable cron job not working
        // anymore ?
        // Error:  testSimpleMethodWithResult  Time elapsed: 0.022 s  <<< FAILURE!
        // java.lang.AssertionError: expected:<1310> but was:<1315>
        //at com.box.l10n.mojito.service.pollableTask.PollableAspectTest.testSimpleMethodWithResult(PollableAspectTest.java:44)
        assertEquals(count + 1, pollableTaskRepository.count());
    }

    @Pollable
    public String simpleMethodWithResult() {
        return "simpleMethodWithResult";
    }

    @Test
    public void testSimpleMethodWithFutureResult() throws InterruptedException, ExecutionException {

        PollableFuture<String> simpleMethodWithResult = simpleMethodWithFutureResult();

        String result = simpleMethodWithResult.get();

        PollableTask pollableTask = pollableTaskService.getPollableTask(
                simpleMethodWithResult.
                getPollableTask().
                getId());

        assertEquals("Message related to the task", pollableTask.getMessage());
        assertNotNull(pollableTask.getFinishedDate());
        assertEquals("The actual result of this function", result);
    }

    @Pollable
    public PollableFuture<String> simpleMethodWithFutureResult() {

        PollableFutureTaskResult<String> pollableTaskResult = new PollableFutureTaskResult<>();

        pollableTaskResult.setMessageOverride("Message related to the task");
        pollableTaskResult.setResult("The actual result of this function");

        return pollableTaskResult;
    }

    @Test
    public void testSimpleAsyncMethodWithResult() throws Exception {
        PollableFuture<String> simpleMethodWithResult = simpleAsyncMethodWithResult();

        String result = simpleMethodWithResult.get();

        PollableTask pollableTask = pollableTaskService.getPollableTask(
                simpleMethodWithResult.
                getPollableTask().
                getId());

        assertEquals("Message related to the task", pollableTask.getMessage());
        assertNotNull(pollableTask.getFinishedDate());
        assertEquals("The actual result of this function", result);
    }

    @Pollable(async = true)
    public PollableFuture<String> simpleAsyncMethodWithResult() {

        PollableFutureTaskResult<String> pollableTaskResult = new PollableFutureTaskResult<>();

        pollableTaskResult.setMessageOverride("Message related to the task");
        pollableTaskResult.setResult("The actual result of this function");

        return pollableTaskResult;
    }

    @Test
    public void testSimpleAsyncMethodWithVoidResult() throws Exception {
        simpleAsyncMethodVoidResult();
    }

    @Pollable(async = true)
    public void simpleAsyncMethodVoidResult() {
    }

    @Test
    public void testMethodThatThrowsException() throws InterruptedException {
        try {
            methodThatThrowsException();
            fail("This call should have thrown an Exception");
        } catch (Exception e) {
            assertEquals("This is a test that expects an exception to be thrown: methodThatThrowsException", e.getMessage());
        }
    }

    @Pollable
    public String methodThatThrowsException() {
        throw new RuntimeException("This is a test that expects an exception to be thrown: methodThatThrowsException");
    }

    @Test
    public void testMethodWithFutureResultThatThrowsException() throws InterruptedException {
        PollableFuture<String> methodThatThrowsException = methodWithFutureResultThatThrowsException();

        try {
            methodThatThrowsException.get();
            fail("This call should have thrown an Exception");
            assertEquals("methodWithFutureResultThatThrowsException - message", methodThatThrowsException.getPollableTask().getMessage());
            assertEquals(5, (int) methodThatThrowsException.getPollableTask().getExpectedSubTaskNumber());
        } catch (ExecutionException ee) {
            PollableTask pollableTask = pollableTaskService.getPollableTask(methodThatThrowsException.getPollableTask().getId());
            assertNotNull(pollableTask.getFinishedDate());
            assertEquals("This is a test that expects an exception to be thrown: methodThatThrowsException", ee.getCause().getMessage());
        }
    }

    @Pollable(message = "methodWithFutureResultThatThrowsException - message", expectedSubTaskNumber = 5)
    public PollableFuture<String> methodWithFutureResultThatThrowsException() {
        throw new RuntimeException("This is a test that expects an exception to be thrown: methodThatThrowsException");
    }

    @Test
    public void testAsyncMethodWithFutureResultThatThrowsException() throws InterruptedException {
        PollableFuture<String> methodThatThrowsException = asyncMethodWithFutureResultThatThrowsException();

        try {
            methodThatThrowsException.get();
            fail("This call should have thrown an Exception");
            assertEquals("asyncMethodWithFutureResultThatThrowsException - message", methodThatThrowsException.getPollableTask().getMessage());
            assertEquals(6, (int) methodThatThrowsException.getPollableTask().getExpectedSubTaskNumber());
        } catch (ExecutionException ee) {
            PollableTask pollableTask = pollableTaskService.getPollableTask(methodThatThrowsException.getPollableTask().getId());
            assertNotNull(pollableTask.getFinishedDate());
            assertEquals("This is a test that expects an exception to be thrown: methodThatThrowsException", ee.getCause().getMessage());
        }
    }

    @Pollable(async = true, message = "asyncMethodWithFutureResultThatThrowsException - message", expectedSubTaskNumber = 6)
    public PollableFuture<String> asyncMethodWithFutureResultThatThrowsException() {
        throw new RuntimeException("This is a test that expects an exception to be thrown: methodThatThrowsException");
    }

    @Pollable
    public void compiles() {
    }

    @Test
    public void testGetWithParentId() {
        PollableTask createParentTask = pollableTaskService.createPollableTask(null, "getWithParentId", null, 0);
        withParentId(createParentTask.getId());
        withParentIdLong(createParentTask.getId());
        withParentTask(createParentTask);
    }

    @Pollable
    public void withParentId(@ParentTask long parentId) {

    }

    @Pollable
    public void withParentIdLong(@ParentTask Long parentId) {

    }

    @Pollable
    public void withParentTask(@ParentTask PollableTask pollableTask) {

    }

    @Test
    public void testGetWithParentIdBadParam() {
        try {
            withParentIdBadParam('c');
            fail("Should throw an exception");
        } catch (Exception e) {

        }
    }

    @Pollable
    public void withParentIdBadParam(@ParentTask char parentId) {

    }

    @Test
    public void testParent() throws JsonProcessingException {
        PollableFuture<Void> parent = parent(null);
        logger.debug(objectMapper.writeValueAsString(parent.getPollableTask()));
    }

    @Pollable(message = "parent task, id: {parentId}", expectedSubTaskNumber = 2)
    public PollableFuture<Void> parent(
            @MsgArg(name = "parentId", accessor = "getId") @InjectCurrentTask PollableTask pollableTask) {

        sub(pollableTask, 1L);
        sub(pollableTask, 2L);
        return null;
    }

    @Pollable(message = "sub {id}")
    public void sub(
            @ParentTask PollableTask parentTask,
            @MsgArg(name = "id") Long id) {
    }

    public String getInAnnotation() {
        return "'this' to get message value";
    }

    @Test
    public void testMsgArgInAnnotation() throws JsonProcessingException {
        PollableFuture<Void> msgArgInAnnotation = msgArgInAnnotation();
        assertEquals(msgArgInAnnotation.getPollableTask().getMessage(),
                "Message with MsgArg in annotation uses 'this' to get message value");
    }

    @Pollable(message = "Message with MsgArg in annotation uses {inAnnotationName}",
            msgArgs = {
                @MsgArg(name = "inAnnotationName", accessor = "getInAnnotation")
            })
    public PollableFuture<Void> msgArgInAnnotation() {
        return null;
    }

    /**
     * This is to test the aspect checks but because it break compilation it
     * cannot be activated. Just uncomment for one off testing.
     *
     */
//    @Pollable
//    public int mustNotCompile() {
//     return 0;
//    }
//
//    public void badParentTask(@ParentTask long id) {
//
//    }
//    public void badInjectCurrentTask(@InjectCurrentTask long id) {
//
//    }
//
//    public void badMsgArg(@MsgArg long id) {
//
//    }
//
//
//    @Pollable
//    public void badParentTask(@ParentTask long id) {
//
//    }
}
