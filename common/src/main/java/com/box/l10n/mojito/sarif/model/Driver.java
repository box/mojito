package com.box.l10n.mojito.sarif.model;

public class Driver {
  private String name;
  private String informationUri;

  public Driver(String name, String informationUri) {
    this.name = name;
    this.informationUri = informationUri;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getInformationUri() {
    return informationUri;
  }

  public void setInformationUri(String informationUri) {
    this.informationUri = informationUri;
  }
}
