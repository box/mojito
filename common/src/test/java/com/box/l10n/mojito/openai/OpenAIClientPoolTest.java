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
import java.util.concurrent.ScheduledFuture;
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
    long timeout = 5000;

    OpenAIClientPool openAIClientPool =
        new OpenAIClientPool(
            numberOfClients, numberOfParallelRequestPerClient, sizeOfAsyncProcessors, API_KEY);

    Stopwatch stopwatch = Stopwatch.createStarted();

    try (TranslationMonitor translationMonitor = new TranslationMonitor(true)) {
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
                      translationMonitor.requestCounter.incrementAndGet();
                      translationMonitor.requestSubmissionTimes.add(
                          requestStopwatch.elapsed(TimeUnit.MILLISECONDS));
                      return chatCompletions;
                    })
                .thenApply(
                    chatCompletionsResponse -> {
                      translationMonitor.responseCounter.incrementAndGet();
                      translationMonitor.responseTimes.add(
                          requestStopwatch.elapsed(TimeUnit.MILLISECONDS));
                      return chatCompletionsResponse;
                    })
                .exceptionally(
                    e -> {
                      translationMonitor.responseCounter.incrementAndGet();
                      translationMonitor.responseTimes.add(timeout);
                      translationMonitor.timedOut.incrementAndGet();
                      return null;
                    });

        responses.add(response);
      }

      Stopwatch started = Stopwatch.createStarted();
      CompletableFuture.allOf(responses.toArray(new CompletableFuture[responses.size()])).join();
      logger.info("Waiting for join: " + started.elapsed());

      LongSummaryStatistics statistics =
          translationMonitor.responseTimes.stream()
              .collect(Collectors.summarizingLong(Long::longValue));

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
              + translationMonitor.timedOut.get());
    }
  }

  public class TranslationMonitor implements AutoCloseable {
    private final AtomicInteger responseCounter = new AtomicInteger();
    private final AtomicInteger requestCounter = new AtomicInteger();
    private final AtomicInteger timedOut = new AtomicInteger();
    private final Stopwatch stopwatch = Stopwatch.createStarted();

    private final List<Long> requestSubmissionTimes =
        Collections.synchronizedList(new ArrayList<>());
    private final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> statsFuture = null;

    public TranslationMonitor(boolean start) {
      this.statsFuture =
          start ? executor.scheduleAtFixedRate(this::printStats, 0, 5, TimeUnit.SECONDS) : null;
    }

    public void incrementSubmitted(long time) {
      requestCounter.incrementAndGet();
      requestSubmissionTimes.add(time);
    }

    public void incrementResponse(long time) {
      responseCounter.incrementAndGet();
      responseTimes.add(time);
    }

    public void incrementTimeout() {
      timedOut.incrementAndGet();
    }

    private void printStats() {
      try {
        List<Long> copySubmissionTimes;
        synchronized (requestSubmissionTimes) {
          copySubmissionTimes = new ArrayList<>(requestSubmissionTimes);
        }
        List<Long> copyResponseTimes;
        synchronized (responseTimes) {
          copyResponseTimes = new ArrayList<>(responseTimes);
        }
        double elapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        double avg = copyResponseTimes.stream().collect(Collectors.averagingLong(Long::longValue));

        Logger logger = LoggerFactory.getLogger(TranslationMonitor.class);
        logger.info(
            "request per second: {}, submission count: {}, last submissions took: {}\nresponse per second: {}, average response time: {}, response count: {}, last elapsed times: {}",
            elapsed > 0 ? requestCounter.get() / elapsed : "n/a",
            requestCounter.get(),
            copySubmissionTimes.subList(
                Math.max(0, copySubmissionTimes.size() - 20), copySubmissionTimes.size()),
            elapsed > 0 ? responseCounter.get() / elapsed : "n/a",
            Math.round(avg),
            responseCounter.get(),
            copyResponseTimes.subList(
                Math.max(0, copyResponseTimes.size() - 20), copyResponseTimes.size()));

      } catch (Throwable t) {
        LoggerFactory.getLogger(TranslationMonitor.class).error("Error displaying stats", t);
      }
    }

    @Override
    public void close() {
      if (statsFuture != null) {
        statsFuture.cancel(false);
      }
      executor.shutdown();
    }
  }
}
