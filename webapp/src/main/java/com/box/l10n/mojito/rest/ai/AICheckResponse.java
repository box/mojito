package com.box.l10n.mojito.rest.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AICheckResponse {

  boolean error;

  String errorMessage;

  Map<String, List<AICheckResult>> results;

  public Map<String, List<AICheckResult>> getResults() {
    return results;
  }

  public void setResults(Map<String, List<AICheckResult>> results) {
    this.results = results;
  }

  public boolean isError() {
    return error;
  }

  public void setError(boolean error) {
    this.error = error;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
