package com.box.l10n.mojito;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

/**
 *
 * @author jaurambault
 */
public class AsyncConfigTest extends ServiceTestBase {

    @Test
    public void testAsync() throws InterruptedException, ExecutionException {
        String threadName = Thread.currentThread().getName();
        Future<String> doAsync = doAsync();
        assertNotEquals(threadName, doAsync.get());
    }

    @Async
    public Future<String> doAsync() {
        return new AsyncResult<>(Thread.currentThread().getName());
    }

}
