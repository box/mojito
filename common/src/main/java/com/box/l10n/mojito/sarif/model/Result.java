package com.box.l10n.mojito.sarif.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Result {
  private String ruleId;
  private ResultLevel level;
  private Message message;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<Location> locations = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, String> properties;

  public Result(String ruleId, String text, ResultLevel level, Map<String, String> properties) {
    this.ruleId = ruleId;
    this.message = new Message(text);
    this.level = level;
    this.properties = properties;
  }

  public Result(String ruleId, String text, ResultLevel level) {
    this.ruleId = ruleId;
    this.message = new Message(text);
    this.level = level;
  }

  public List<Location> getLocations() {
    return locations;
  }

  public void setLocations(List<Location> locations) {
    this.locations = locations;
  }

  public String getRuleId() {
    return ruleId;
  }

  public void setRuleId(String ruleId) {
    this.ruleId = ruleId;
  }

  public ResultLevel getLevel() {
    return level;
  }

  public void setLevel(ResultLevel level) {
    this.level = level;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public void addLocation(Location location) {
    this.locations.add(location);
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
