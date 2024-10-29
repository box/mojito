package com.box.l10n.mojito.openai;

import com.google.common.base.Function;
import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAIClientPool {

  static Logger logger = LoggerFactory.getLogger(OpenAIClientPool.class);

  int numberOfClients;
  OpenAIClientWithSemaphore[] openAIClientWithSemaphores;

  /**
   * Pool to parallelize slower requests (1s+) over HTTP/2 connections.
   *
   * @param numberOfClients Number of OpenAIClient instances with independent HttpClients.
   * @param numberOfParallelRequestPerClient Maximum parallel requests per client, controlled by a
   *     semaphore to prevent overload.
   * @param sizeOfAsyncProcessors Shared async processors across all HttpClients to limit threads,
   *     as request time is the main bottleneck.
   * @param apiKey API key for authentication.
   */
  public OpenAIClientPool(
      int numberOfClients,
      int numberOfParallelRequestPerClient,
      int sizeOfAsyncProcessors,
      String apiKey) {
    ExecutorService asyncExecutor = Executors.newWorkStealingPool(sizeOfAsyncProcessors);
    this.numberOfClients = numberOfClients;
    this.openAIClientWithSemaphores = new OpenAIClientWithSemaphore[numberOfClients];
    for (int i = 0; i < numberOfClients; i++) {
      this.openAIClientWithSemaphores[i] =
          new OpenAIClientWithSemaphore(
              OpenAIClient.builder()
                  .apiKey(apiKey)
                  .asyncExecutor(asyncExecutor)
                  .httpClient(HttpClient.newBuilder().executor(asyncExecutor).build())
                  .build(),
              new Semaphore(numberOfParallelRequestPerClient));
    }
  }

  public <T> CompletableFuture<T> submit(Function<OpenAIClient, CompletableFuture<T>> f) {

    while (true) {
      for (OpenAIClientWithSemaphore openAIClientWithSemaphore : openAIClientWithSemaphores) {
        if (openAIClientWithSemaphore.semaphore().tryAcquire()) {
          return f.apply(openAIClientWithSemaphore.openAIClient())
              .whenComplete((o, e) -> openAIClientWithSemaphore.semaphore().release());
        }
      }

      try {
        logger.debug("can't directly acquire any semaphore, do blocking");
        int randomSemaphoreIndex =
            ThreadLocalRandom.current().nextInt(openAIClientWithSemaphores.length);
        OpenAIClientWithSemaphore randomClientWithSemaphore =
            this.openAIClientWithSemaphores[randomSemaphoreIndex];
        randomClientWithSemaphore.semaphore().acquire();
        return f.apply(randomClientWithSemaphore.openAIClient())
            .whenComplete((o, e) -> randomClientWithSemaphore.semaphore().release());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Can't submit task to the OpenAIClientPool", e);
      }
    }
  }

  record OpenAIClientWithSemaphore(OpenAIClient openAIClient, Semaphore semaphore) {}
}
