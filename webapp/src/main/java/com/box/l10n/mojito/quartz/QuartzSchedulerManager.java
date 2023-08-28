package com.box.l10n.mojito.quartz;

import java.util.List;
import org.quartz.Scheduler;

public interface QuartzSchedulerManager {

  String DEFAULT_SCHEDULER_NAME = "default";

  Scheduler getScheduler(String schedulerName);

  List<Scheduler> getSchedulers();
}
