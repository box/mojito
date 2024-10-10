package com.box.l10n.mojito.pagerduty;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * PagerDuty client for creating (triggering) and resolving incidents.
 *
 * @author mattwilshire
 */
public class PagerDutyClient {

  static Logger logger = LoggerFactory.getLogger(PagerDutyClient.class);

  static final String BASE_URL = "https://events.pagerduty.com";
  static final String ENQUEUE_PATH = "/v2/enqueue";

  public static final int MAX_RETRIES = 3;

  private final HttpClient httpClient;
  private final String integrationKey;
  private final PagerDutyRetryConfiguration retryConfiguration;

  public PagerDutyClient(
      String integrationKey,
      HttpClient httpClient,
      PagerDutyRetryConfiguration retryConfiguration) {
    if (integrationKey == null || integrationKey.isEmpty())
      throw new IllegalArgumentException("Pager Duty integration key is null or empty.");
    this.integrationKey = integrationKey;
    this.httpClient = httpClient;
    this.retryConfiguration = retryConfiguration;
  }

  /**
   * Trigger incident using a deduplication key and send PagerDutyPayload with it. The client will
   * attempt to send the event request MAX_RETRIES times if an internal error is received from the
   * server. If the max number of retries is reached, a bad request is received or the payload
   * cannot be serialized a PagerDutyException is thrown.
   */
  public void triggerIncident(String dedupKey, PagerDutyPayload payload) throws PagerDutyException {
    sendPayload(dedupKey, payload, PagerDutyPostData.EventAction.TRIGGER);
  }

  /**
   * Resolve incident using a deduplication key. The client will attempt to send the event request
   * MAX_RETRIES times if an internal error is received from the server. If the max number of
   * retries is reached, a bad request is received or the payload cannot be serialized a
   * PagerDutyException is thrown.
   */
  public void resolveIncident(String dedupKey) throws PagerDutyException {
    sendPayload(dedupKey, null, PagerDutyPostData.EventAction.RESOLVE);
  }

  private void sendPayload(
      String dedupKey, PagerDutyPayload payload, PagerDutyPostData.EventAction eventAction)
      throws PagerDutyException {

    if (dedupKey == null || dedupKey.isEmpty())
      throw new PagerDutyException("Deduplication key should not be null or empty.");

    PagerDutyPostData postBody = new PagerDutyPostData(integrationKey, dedupKey);
    postBody.setPayload(payload);
    postBody.setEventAction(eventAction);

    try {
      HttpRequest request = buildRequest(postBody.serialize());
      sendRequestWithRetries(request).block();
    } catch (JsonProcessingException e) {
      throw new PagerDutyException("Failed to serialize PagerDutyPostRequest to a JSON string.");
    } catch (Exception e) {
      // Reactor core library can throw wrapped error, unwrap it and rethrow
      if (e.getCause() != null && e.getCause() instanceof PagerDutyException) {
        throw (PagerDutyException) e.getCause();
      }
      throw e;
    }
  }

  private Mono<Void> sendRequestWithRetries(HttpRequest request) {
    return Mono.fromCallable(() -> this.sendRequest(request))
        .retryWhen(
            Retry.backoff(
                    retryConfiguration.getMaxRetries(),
                    Duration.ofMillis(retryConfiguration.getMinBackOffDelay()))
                .maxBackoff(Duration.ofMillis(retryConfiguration.getMaxBackOffDelay()))
                .filter(
                    throwable ->
                        !(throwable instanceof PagerDutyException
                            && ((PagerDutyException) throwable).getStatusCode() == 400))
                .onRetryExhaustedThrow(
                    (retryBackoffSpec, retrySignal) -> {
                      Throwable throwable = retrySignal.failure();
                      if (throwable instanceof PagerDutyException) {
                        return throwable;
                      }
                      return new PagerDutyException(
                          "PagerDuty failed to send event payload: ", throwable);
                    }))
        .then();
  }

  private HttpResponse<String> sendRequest(HttpRequest request) throws PagerDutyException {
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      int statusCode = response.statusCode();
      if (statusCode == 200 || statusCode == 202) return response;

      throw new PagerDutyException(statusCode, response.body());
    } catch (IOException | InterruptedException e) {
      throw new PagerDutyException("Failed to send PagerDuty request: " + e.getMessage());
    }
  }

  public HttpRequest buildRequest(String postBody) {
    return HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + ENQUEUE_PATH))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(postBody))
        .build();
  }
}
