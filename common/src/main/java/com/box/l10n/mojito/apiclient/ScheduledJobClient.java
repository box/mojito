package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.model.ScheduledJobDTO;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobClient {

  @Autowired private ScheduledJobWsApi scheduledJobWsApi;

  public ScheduledJobDTO createJob(ScheduledJobDTO scheduledJobDTO) {
    return scheduledJobWsApi.createJob(scheduledJobDTO);
  }

  public ScheduledJobDTO updateJob(UUID uuid, ScheduledJobDTO scheduledJobDTO) {
    return scheduledJobWsApi.updateJob(scheduledJobDTO, uuid);
  }

  public void deleteJob(UUID uuid) {
    scheduledJobWsApi.deleteJob(uuid);
  }

  public void restoreJob(UUID uuid) {
    scheduledJobWsApi.restoreJob(uuid);
  }
}
