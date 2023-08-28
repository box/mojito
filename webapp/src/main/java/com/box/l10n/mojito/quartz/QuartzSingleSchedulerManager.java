package com.box.l10n.mojito.quartz;

import java.util.Collections;
import java.util.List;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "l10n.org.multi-quartz.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class QuartzSingleSchedulerManager implements QuartzSchedulerManager {

  @Autowired Scheduler scheduler;

  @Override
  public Scheduler getScheduler(String schedulerName) {
    return scheduler;
  }

  @Override
  public List<Scheduler> getSchedulers() {
    return Collections.singletonList(scheduler);
  }
}
