package com.box.l10n.mojito.sarif.model;

public class PhysicalLocation {
  private ArtifactLocation artifactLocation;
  private Region region;

  PhysicalLocation(String uri) {
    artifactLocation = new ArtifactLocation(uri);
    region = new Region();
  }

  PhysicalLocation(String uri, Integer startLine) {
    artifactLocation = new ArtifactLocation(uri);
    region = new Region(startLine);
  }

  PhysicalLocation(String uri, Integer startLine, Integer endLine) {
    artifactLocation = new ArtifactLocation(uri);
    region = new Region(startLine, endLine);
  }

  public Region getRegion() {
    return region;
  }

  public void setRegion(Region region) {
    this.region = region;
  }

  public ArtifactLocation getArtifactLocation() {
    return artifactLocation;
  }

  public void setArtifactLocation(ArtifactLocation artifactLocation) {
    this.artifactLocation = artifactLocation;
  }
}
