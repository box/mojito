package com.box.l10n.mojito.pagerduty;

import com.box.l10n.mojito.entity.PagerDutyIncident;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
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

  private final HttpClient httpClient;
  private final String integrationKey;
  private final PagerDutyRetryConfiguration retryConfiguration;
  private final String clientName;
  private final PagerDutyIncidentRepository pagerDutyIncidentRepository;
  private final Duration triggerTimeThreshold;

  public PagerDutyClient(
      String integrationKey,
      HttpClient httpClient,
      PagerDutyRetryConfiguration retryConfiguration,
      String clientName,
      PagerDutyIncidentRepository pagerDutyIncidentRepository,
      Duration triggerTimeThreshold) {
    if (integrationKey == null || integrationKey.isEmpty())
      throw new IllegalArgumentException("Pager Duty integration key is null or empty.");
    this.integrationKey = integrationKey;
    this.httpClient = httpClient;
    this.retryConfiguration = retryConfiguration;
    this.clientName = clientName;
    this.pagerDutyIncidentRepository = pagerDutyIncidentRepository;
    this.triggerTimeThreshold = triggerTimeThreshold;
  }

  /**
   * Trigger incident using a deduplication key and send PagerDutyPayload with it. The client will
   * attempt to send the event request 'maxRetries' times if an internal error is received from the
   * server. If the max number of retries is reached, a bad request is received or the payload
   * cannot be serialized a PagerDutyException is thrown.
   */
  public void triggerIncident(String dedupKey, PagerDutyPayload payload) throws PagerDutyException {
    Optional<PagerDutyIncident> incidentOpt =
        pagerDutyIncidentRepository.findOpenIncident(clientName, dedupKey);
    if (incidentOpt.isPresent()) {
      PagerDutyIncident incident = incidentOpt.get();
      ZonedDateTime beforeNow = ZonedDateTime.now().minus(this.triggerTimeThreshold);
      if (!incident.getTriggeredAt().isBefore(beforeNow)) {
        // The incident was triggered within the last trigger threshold minutes, don't send another
        // request
        logger.info(
            "Open incident exists for deduplication key: '{}', ignoring trigger request.",
            dedupKey);
        return;
      }
    }
    sendPayload(dedupKey, payload, PagerDutyPostData.EventAction.TRIGGER);
  }

  /**
   * Resolve incident using a deduplication key. The client will attempt to send the event request
   * 'maxRetries' times if an internal error is received from the server. If the max number of
   * retries is reached, a bad request is received or the payload cannot be serialized a
   * PagerDutyException is thrown.
   */
  public void resolveIncident(String dedupKey) throws PagerDutyException {
    if (pagerDutyIncidentRepository.findOpenIncident(clientName, dedupKey).isEmpty()) {
      logger.debug(
          "No open incident for deduplication key: '{}', ignoring resolve request.", dedupKey);
      return;
    }
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
      sendRequestWithRetries(request, dedupKey, eventAction).block();
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

  private Mono<Void> sendRequestWithRetries(
      HttpRequest request, String dedupKey, PagerDutyPostData.EventAction eventAction) {
    return Mono.fromCallable(() -> this.sendRequest(request, dedupKey, eventAction))
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

  private HttpResponse<String> sendRequest(
      HttpRequest request, String dedupKey, PagerDutyPostData.EventAction eventAction)
      throws PagerDutyException {
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      int statusCode = response.statusCode();
      if (statusCode == 200 || statusCode == 202) {
        Optional<PagerDutyIncident> incidentOpt =
            pagerDutyIncidentRepository.findOpenIncident(clientName, dedupKey);
        PagerDutyIncident incident = incidentOpt.orElse(new PagerDutyIncident());
        if (eventAction == PagerDutyPostData.EventAction.TRIGGER) {
          incident.setClientName(clientName);
          incident.setDedupKey(dedupKey);
          incident.setTriggeredAt(ZonedDateTime.now());
        } else {
          incident = pagerDutyIncidentRepository.findOpenIncident(clientName, dedupKey).get();
          incident.setResolvedAt(ZonedDateTime.now());
        }
        pagerDutyIncidentRepository.save(incident);
        return response;
      }

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
