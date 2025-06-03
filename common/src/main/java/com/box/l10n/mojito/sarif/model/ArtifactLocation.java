package com.box.l10n.mojito.sarif.model;

public class ArtifactLocation {
  public String uri;

  public ArtifactLocation(String uri) {
    this.uri = uri;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
}
