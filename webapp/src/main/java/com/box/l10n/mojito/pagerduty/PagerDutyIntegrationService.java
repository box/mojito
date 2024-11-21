package com.box.l10n.mojito.pagerduty;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service for serving PagerDuty clients created from the app properties. To use the default PD
 * client method the default client must be set under `l10n.pagerduty.defaultIntegration`
 *
 * @author mattwilshire
 */
@Component
public class PagerDutyIntegrationService {

  private final PagerDutyIntegrationConfiguration pagerDutyIntegrationConfiguration;
  private final PagerDutyRetryConfiguration pagerDutyRetryConfiguration;
  private Map<String, PagerDutyClient> pagerDutyClients;
  private final PagerDutyIncidentRepository pagerDutyIncidentRepository;

  @Autowired
  public PagerDutyIntegrationService(
      PagerDutyIntegrationConfiguration pagerDutyIntegrationsConfiguration,
      PagerDutyRetryConfiguration pagerDutyRetryConfiguration,
      PagerDutyIncidentRepository pagerDutyIncidentRepository) {
    this.pagerDutyIntegrationConfiguration = pagerDutyIntegrationsConfiguration;
    this.pagerDutyRetryConfiguration = pagerDutyRetryConfiguration;
    this.pagerDutyIncidentRepository = pagerDutyIncidentRepository;

    createClientsFromConfiguration();
  }

  public Optional<PagerDutyClient> getPagerDutyClient(String integration) {
    return pagerDutyClients.containsKey(integration)
        ? Optional.of(pagerDutyClients.get(integration))
        : Optional.empty();
  }

  public Optional<PagerDutyClient> getDefaultPagerDutyClient() {
    if (pagerDutyIntegrationConfiguration.getDefaultIntegration() == null) return Optional.empty();
    return getPagerDutyClient(pagerDutyIntegrationConfiguration.getDefaultIntegration());
  }

  private void createClientsFromConfiguration() {
    pagerDutyClients =
        pagerDutyIntegrationConfiguration.getIntegrations().entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e ->
                        new PagerDutyClient(
                            e.getValue(),
                            HttpClient.newHttpClient(),
                            pagerDutyRetryConfiguration,
                            e.getKey(),
                            pagerDutyIncidentRepository)));
  }
}
