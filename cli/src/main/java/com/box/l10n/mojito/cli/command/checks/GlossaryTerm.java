package com.box.l10n.mojito.cli.command.checks;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class GlossaryTerm {

  private String term;

  @JsonProperty("severity")
  private GlossaryTermSeverity severity;

  public String getTerm() {
    return term;
  }

  public void setTerm(String term) {
    this.term = term;
  }

  public GlossaryTermSeverity getSeverity() {
    return severity;
  }

  public void setSeverity(GlossaryTermSeverity severity) {
    this.severity = severity;
  }
}
