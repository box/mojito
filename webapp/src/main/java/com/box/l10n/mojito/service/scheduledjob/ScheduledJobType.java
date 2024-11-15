package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.service.scheduledjob.jobs.ScheduledThirdPartySync;
import com.box.l10n.mojito.service.scheduledjob.jobs.ScheduledThirdPartySyncProperties;

public enum ScheduledJobType {
  THIRD_PARTY_SYNC(
      ScheduledThirdPartySync.class.getName(), ScheduledThirdPartySyncProperties.class);

  final String jobClassName;
  final Class<? extends ScheduledJobProperties> propertiesClass;

  ScheduledJobType(String jobClassName, Class<? extends ScheduledJobProperties> propertiesClass) {
    this.jobClassName = jobClassName;
    this.propertiesClass = propertiesClass;
  }

  public String getJobClassName() {
    return jobClassName;
  }

  public Class<? extends ScheduledJobProperties> getPropertiesClass() {
    return propertiesClass;
  }
}
