package com.box.l10n.mojito.sarif.model;

import java.util.ArrayList;
import java.util.List;

public class Run {
  private Tool tool;
  private List<Result> results = new ArrayList<>();

  public Run(Tool tool) {
    this.tool = tool;
  }

  public List<Result> getResults() {
    return results;
  }

  public void setResults(List<Result> results) {
    this.results = results;
  }

  public Tool getTool() {
    return tool;
  }

  public void setTool(Tool tool) {
    this.tool = tool;
  }

  public void addResult(Result result) {
    this.results.add(result);
  }
}
