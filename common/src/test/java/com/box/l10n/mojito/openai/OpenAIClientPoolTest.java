package com.box.l10n.mojito.openai;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;

import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsResponse;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    int numberOfParallelRequestPerClient = 50;
    int numberOfRequests = 10000;
    int sizeOfAsyncProcessors = 10;
    int totalExecutions = numberOfClients * numberOfParallelRequestPerClient;

    OpenAIClientPool openAIClientPool =
        new OpenAIClientPool(
            numberOfClients, numberOfParallelRequestPerClient, sizeOfAsyncProcessors, API_KEY);

    AtomicInteger responseCounter = new AtomicInteger();
    AtomicInteger submitted = new AtomicInteger();
    Stopwatch stopwatch = Stopwatch.createStarted();

    ArrayList<Long> submissionTimes = new ArrayList<>();
    ArrayList<Long> responseTimes = new ArrayList<>();

    List<CompletableFuture<ChatCompletionsResponse>> responses = new ArrayList<>();
    for (int i = 0; i < numberOfRequests; i++) {
      String message = "Is %d prime?".formatted(i);
      Stopwatch requestStopwatch = Stopwatch.createStarted();
      OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
          chatCompletionsRequest()
              .model("gpt-4o-2024-08-06")
              .messages(
                  List.of(
                      systemMessageBuilder()
                          .content("You're an engine designed to check prime numbers")
                          .build(),
                      userMessageBuilder().content(message).build()))
              .build();

      CompletableFuture<ChatCompletionsResponse> response =
          openAIClientPool.submit(
              openAIClient -> {
                CompletableFuture<ChatCompletionsResponse> chatCompletions =
                    openAIClient.getChatCompletions(chatCompletionsRequest);
                submissionTimes.add(requestStopwatch.elapsed(TimeUnit.SECONDS));
                if (submitted.incrementAndGet() % 100 == 0) {
                  logger.info(
                      "--> request per second: "
                          + submitted.get() / (stopwatch.elapsed(TimeUnit.SECONDS) + 0.00001)
                          + ", submission count: "
                          + submitted.get()
                          + ", future response count: "
                          + responses.size()
                          + ", last submissions took: "
                          + submissionTimes.subList(
                              Math.max(0, submissionTimes.size() - 100), submissionTimes.size()));
                }
                return chatCompletions;
              });

      response.thenApply(
          chatCompletionsResponse -> {
            responseTimes.add(requestStopwatch.elapsed(TimeUnit.MILLISECONDS));
            if (responseCounter.incrementAndGet() % 10 == 0) {
              double avg =
                  responseTimes.stream().collect(Collectors.averagingLong(Long::longValue));
              logger.info(
                  "<-- response per second: "
                      + responseCounter.get() / stopwatch.elapsed(TimeUnit.SECONDS)
                      + ", average response time: "
                      + Math.round(avg)
                      + " (rps: "
                      + Math.round(totalExecutions / (avg / 1000.0))
                      + "), response count from counter: "
                      + responseCounter.get()
                      + ", last elapsed times: "
                      + responseTimes.subList(responseTimes.size() - 20, responseTimes.size()));
            }
            return chatCompletionsResponse;
          });

      responses.add(response);
    }

    Stopwatch started = Stopwatch.createStarted();
    CompletableFuture.allOf(responses.toArray(new CompletableFuture[responses.size()])).join();
    logger.info("Waiting for join: " + started.elapsed());

    double avg = responseTimes.stream().collect(Collectors.averagingLong(Long::longValue));
    logger.info(
        "Total time: "
            + stopwatch.elapsed().toString()
            + ", request per second: "
            + Math.round((double) numberOfRequests / stopwatch.elapsed(TimeUnit.SECONDS))
            + ", average response time: "
            + Math.round(avg)
            + " (theory rps: "
            + Math.round(totalExecutions / (avg / 1000.0))
            + ")");
  }
}
