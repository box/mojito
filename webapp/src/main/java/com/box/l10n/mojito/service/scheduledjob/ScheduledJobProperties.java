package com.box.l10n.mojito.service.scheduledjob;

public abstract class ScheduledJobProperties {
  private int version = 1;

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
