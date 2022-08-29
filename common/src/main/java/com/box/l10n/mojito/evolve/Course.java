package com.box.l10n.mojito.evolve;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Course {

  @JsonProperty("_id")
  String id;

  @JsonProperty("_state")
  String state;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
