package com.box.l10n.mojito.pagerduty;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.PagerDutyIncident;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class PagerDutyClientTest {
  private PagerDutyClient pagerDutyClient;
  private HttpClient httpClient;

  @SuppressWarnings("rawtypes")
  private HttpResponse httpResponse;

  private PagerDutyPayload samplePayload;
  private PagerDutyRetryConfiguration retryConfiguration = new PagerDutyRetryConfiguration();
  private PagerDutyIncidentRepository pagerDutyIncidentRepository;

  @BeforeEach
  public void setup() {
    httpClient = mock(HttpClient.class);
    httpResponse = mock(HttpResponse.class);
    pagerDutyIncidentRepository = mock(PagerDutyIncidentRepository.class);

    pagerDutyClient =
        new PagerDutyClient(
            "xxxyyyzzz", httpClient, retryConfiguration, "test", pagerDutyIncidentRepository);

    Map<String, String> customDetails = new HashMap<>();
    customDetails.put("Example Custom Details", "Example Value");
    samplePayload =
        new PagerDutyPayload(
            "Job Failed.", "Mojito", PagerDutyPayload.Severity.ERROR, customDetails);
  }

  @Test
  public void testNormalTriggerResolve() throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(202);
    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    try {
      pagerDutyClient.triggerIncident("dedpuKey", samplePayload);

      // Respond with fake open incident to allow resolve to go through.
      when(pagerDutyIncidentRepository.findOpenIncident(any(), any()))
          .thenReturn(Optional.of(new PagerDutyIncident()));
      pagerDutyClient.resolveIncident("dedupKey");
    } catch (PagerDutyException e) {
      fail("PagerDutyClient should not throw exception when the response code is 202.");
    }

    verify(httpClient, times(2))
        .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
  }

  @Test
  public void testFailsWithMaxRetries() throws IOException, InterruptedException {
    int statusCode = 500;
    String responseBody = "{ Some type of Response Body }";

    when(httpResponse.statusCode()).thenReturn(statusCode);
    when(httpResponse.body()).thenReturn(responseBody);
    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(
        PagerDutyException.class, () -> pagerDutyClient.triggerIncident("dedpuKey", samplePayload));

    verify(httpClient, times(retryConfiguration.getMaxRetries() + 1))
        .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
  }

  @Test
  public void testRecoversFromFailedAttemptsEarly() throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(500, 202);
    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    try {
      pagerDutyClient.triggerIncident("dedpuKey", samplePayload);
      verify(httpClient, times(2))
          .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
    } catch (PagerDutyException e) {
      fail("PagerDutyClient should have recovered from failing attempts.");
    }
  }

  @Test
  public void testRecoversFromFailedAttemptsMaxRetries() throws IOException, InterruptedException {
    AtomicInteger callCount = new AtomicInteger(0);
    when(httpResponse.statusCode())
        .thenAnswer(
            (Answer<Integer>)
                i -> {
                  if (callCount.getAndIncrement() < retryConfiguration.getMaxRetries() - 1) {
                    return 500;
                  } else {
                    return 202;
                  }
                });

    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    try {
      pagerDutyClient.triggerIncident("dedpuKey", samplePayload);
      verify(httpClient, times(retryConfiguration.getMaxRetries()))
          .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
    } catch (PagerDutyException e) {
      fail("PagerDutyClient should have recovered from failing attempts.");
    }
  }

  @Test
  public void ioExceptionReachesMaxRetries() throws IOException, InterruptedException {
    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenThrow(IOException.class);

    assertThrows(
        PagerDutyException.class, () -> pagerDutyClient.triggerIncident("dedpuKey", samplePayload));

    verify(httpClient, times(retryConfiguration.getMaxRetries() + 1))
        .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
  }

  @Test
  public void testBadRequest() throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(400);
    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(
        PagerDutyException.class, () -> pagerDutyClient.triggerIncident("dedpuKey", samplePayload));

    // Fake open incident
    when(pagerDutyIncidentRepository.findOpenIncident(any(), any()))
        .thenReturn(Optional.of(new PagerDutyIncident()));
    assertThrows(PagerDutyException.class, () -> pagerDutyClient.resolveIncident("dedupKey"));

    verify(httpClient, times(2))
        .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
  }

  @Test
  public void testFailsWithoutDedupKey() throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(
        PagerDutyException.class, () -> pagerDutyClient.triggerIncident("", samplePayload));

    // Respond with fake open incident to allow resolve to go through.
    when(pagerDutyIncidentRepository.findOpenIncident(any(), any()))
        .thenReturn(Optional.of(new PagerDutyIncident()));
    assertThrows(PagerDutyException.class, () -> pagerDutyClient.resolveIncident(null));

    verify(httpClient, never())
        .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
  }

  @Test
  public void testFailsWithoutIntegrationKey() throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PagerDutyClient(
                null, httpClient, retryConfiguration, "test", pagerDutyIncidentRepository));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PagerDutyClient(
                "", httpClient, retryConfiguration, "test", pagerDutyIncidentRepository));
  }

  @Test
  public void testDoesntSendPayload() throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpClient.send(
            Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    try {
      // There is an open incident, the client shouldn't send the trigger payload
      when(pagerDutyIncidentRepository.findOpenIncident(any(), any()))
          .thenReturn(Optional.of(new PagerDutyIncident()));
      pagerDutyClient.triggerIncident("dedpuKey", samplePayload);

      // There is no open incident, the client shouldn't send the resolve payload
      when(pagerDutyIncidentRepository.findOpenIncident(any(), any())).thenReturn(Optional.empty());
      pagerDutyClient.resolveIncident("dedupKey");
    } catch (PagerDutyException e) {
      fail("PagerDutyClient should not throw exception when the response code is 202.");
    }

    verify(httpClient, times(0)).send(any(), any());
  }
}
