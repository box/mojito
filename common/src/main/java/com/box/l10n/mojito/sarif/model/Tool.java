package com.box.l10n.mojito.sarif.model;

public class Tool {
  private Driver driver;

  public Tool(Driver driver) {
    this.driver = driver;
  }

  public Driver getDriver() {
    return driver;
  }

  public void setDriver(Driver driver) {
    this.driver = driver;
  }
}
