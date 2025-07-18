package com.box.l10n.mojito.openai;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;

import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsResponse;
import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled
public class OpenAIClientPoolTest {

  static Logger logger = LoggerFactory.getLogger(OpenAIClientPoolTest.class);

  static final String API_KEY;

  static {
    try {
      //      API_KEY =
      //
      // Files.readString(Paths.get(System.getProperty("user.home")).resolve(".keys/openai"))
      //              .trim();
      API_KEY = "test-api-key";
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void test() {
    int numberOfClients = 10;
    int numberOfParallelRequestPerClient = 100;
    int numberOfRequests = 5000;
    int sizeOfAsyncProcessors = 1;
    int totalExecutions = numberOfClients * numberOfParallelRequestPerClient;

    OpenAIClientPool openAIClientPool =
        new OpenAIClientPool(
            numberOfClients, numberOfParallelRequestPerClient, sizeOfAsyncProcessors, API_KEY);

    AtomicInteger responseCounter = new AtomicInteger();
    AtomicInteger submitted = new AtomicInteger();
    AtomicInteger timedOut = new AtomicInteger();
    Stopwatch stopwatch = Stopwatch.createStarted();

    List<Long> submissionTimes = Collections.synchronizedList(new ArrayList<>());
    List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

    ScheduledExecutorService scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        () -> {
          try {

            List<Long> copySubmissionTimes;
            synchronized (submissionTimes) {
              copySubmissionTimes = new ArrayList<>(submissionTimes);
            }

            List<Long> copyResponseTimes;
            synchronized (responseTimes) {
              copyResponseTimes = new ArrayList<>(responseTimes);
            }

            double elapsed = stopwatch.elapsed(TimeUnit.SECONDS) + 0.00001;
            double avg =
                copyResponseTimes.stream().collect(Collectors.averagingLong(Long::longValue));

            logger.info(
                "request per second: "
                    + submitted.get() / elapsed
                    + ", submission count: "
                    + submitted.get()
                    + ", last submissions took: "
                    + copySubmissionTimes.subList(
                        Math.max(0, copySubmissionTimes.size() - 100), copySubmissionTimes.size()));

            logger.info(
                "response per second: "
                    + responseCounter.get() / elapsed
                    + ", average response time: "
                    + Math.round(avg)
                    + " (rps: "
                    + Math.round(totalExecutions / (avg / 1000.0))
                    + "), response count from counter: "
                    + responseCounter.get()
                    + ", last elapsed times: "
                    + copyResponseTimes.subList(
                        Math.max(0, copyResponseTimes.size() - 20), copyResponseTimes.size()));

          } catch (Throwable t) {
            logger.error("Error displaying stats", t);
          }
        },
        0,
        1,
        TimeUnit.SECONDS);

    long timeout = 5000;

    List<CompletableFuture<ChatCompletionsResponse>> responses = new ArrayList<>();
    for (int i = 0; i < numberOfRequests; i++) {
      String message = "Is %d prime?".formatted(i);
      Stopwatch requestStopwatch = Stopwatch.createStarted();
      OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
          chatCompletionsRequest()
              .model("gpt-4.1")
              .messages(
                  List.of(
                      systemMessageBuilder()
                          .content("You're an engine designed to check prime numbers")
                          .build(),
                      userMessageBuilder().content(message).build()))
              .build();

      CompletableFuture<ChatCompletionsResponse> response =
          openAIClientPool
              .submit(
                  openAIClient -> {
                    CompletableFuture<ChatCompletionsResponse> chatCompletions =
                        openAIClient.getChatCompletions(
                            chatCompletionsRequest, Duration.of(5000, ChronoUnit.MILLIS));
                    submitted.incrementAndGet();
                    submissionTimes.add(requestStopwatch.elapsed(TimeUnit.MILLISECONDS));
                    return chatCompletions;
                  })
              .thenApply(
                  chatCompletionsResponse -> {
                    responseCounter.incrementAndGet();
                    responseTimes.add(requestStopwatch.elapsed(TimeUnit.MILLISECONDS));
                    return chatCompletionsResponse;
                  })
              .exceptionally(
                  e -> {
                    responseCounter.incrementAndGet();
                    responseTimes.add(timeout);
                    timedOut.incrementAndGet();
                    return null;
                  });

      responses.add(response);
    }

    Stopwatch started = Stopwatch.createStarted();
    CompletableFuture.allOf(responses.toArray(new CompletableFuture[responses.size()])).join();
    logger.info("Waiting for join: " + started.elapsed());

    LongSummaryStatistics statistics =
        responseTimes.stream().collect(Collectors.summarizingLong(Long::longValue));

    logger.info(
        "Total time: "
            + stopwatch.elapsed().toString()
            + ", total request: "
            + numberOfRequests
            + ", request per second: "
            + Math.round((double) numberOfRequests / stopwatch.elapsed(TimeUnit.SECONDS))
            + ", average response time: "
            + Math.round(statistics.getAverage())
            + " (theory rps: "
            + Math.round(totalExecutions / (statistics.getAverage() / 1000.0))
            + ")"
            + ", timed out: "
            + timedOut.get());
  }
}
