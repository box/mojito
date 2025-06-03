package com.box.l10n.mojito.sarif.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public class Region {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Integer startLine;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Integer endLine;

  public Region() {}

  public Region(Integer startLine) {
    this.startLine = startLine;
  }

  public Region(Integer startLine, Integer endLine) {
    this.startLine = startLine;
    this.endLine = endLine;
  }

  public Integer getStartLine() {
    return startLine;
  }

  public void setStartLine(Integer startLine) {
    this.startLine = startLine;
  }

  public Integer getEndLine() {
    return endLine;
  }

  public void setEndLine(Integer endLine) {
    this.endLine = endLine;
  }
}
