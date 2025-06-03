package com.box.l10n.mojito.sarif.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class Sarif {
  @JsonProperty("$schema")
  public final String schema = "https://json.schemastore.org/sarif-2.1.0.json";

  public final String version = "2.1.0";
  public List<Run> runs = new ArrayList<>();

  public String getSchema() {
    return schema;
  }

  public String getVersion() {
    return version;
  }

  public List<Run> getRuns() {
    return runs;
  }

  public void setRuns(List<Run> runs) {
    this.runs = runs;
  }
}
