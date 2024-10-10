package com.box.l10n.mojito.pagerduty;

import com.box.l10n.mojito.json.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.Serializable;

public class PagerDutyPostData implements Serializable {
  private PagerDutyPayload payload;
  private String routingKey;
  private String dedupKey;
  private EventAction eventAction;

  public PagerDutyPostData(String integrationKey, String dedupKey) {
    this.routingKey = integrationKey;
    this.dedupKey = dedupKey;
  }

  public String serialize() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    return objectMapper.writeValueAsString(this);
  }

  public enum EventAction {
    RESOLVE,
    TRIGGER;

    @JsonValue
    public String toLowerCase() {
      return name().toLowerCase();
    }
  }

  public PagerDutyPayload getPayload() {
    return payload;
  }

  public void setPayload(PagerDutyPayload payload) {
    this.payload = payload;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getDedupKey() {
    return dedupKey;
  }

  public void setDedupKey(String dedupKey) {
    this.dedupKey = dedupKey;
  }

  public EventAction getEventAction() {
    return eventAction;
  }

  public void setEventAction(EventAction eventAction) {
    this.eventAction = eventAction;
  }
}
