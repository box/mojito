package com.box.l10n.mojito.quartz.multi;

import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "l10n.org.multi-quartz.enabled", havingValue = "true")
@DependsOn("quartzMultiSchedulerConfig")
public class QuartzMultiSchedulerManager implements QuartzSchedulerManager {

  Logger logger = LoggerFactory.getLogger(QuartzMultiSchedulerManager.class);

  @Autowired List<Scheduler> schedulers;

  Map<String, Scheduler> schedulerByName = new HashMap<>();

  @PostConstruct
  public void init() throws SchedulerException {
    for (Scheduler scheduler : schedulers) {
      if (schedulerByName.containsKey(scheduler.getSchedulerName())) {
        throw new QuartzMultiSchedulerException(
            "Scheduler with name '"
                + scheduler.getSchedulerName()
                + "' already exists. Please configure a unique name for each scheduler");
      }
      schedulerByName.put(scheduler.getSchedulerName(), scheduler);
    }

    if (!schedulerByName.containsKey(DEFAULT_SCHEDULER_NAME)) {
      throw new QuartzMultiSchedulerException(
          "No default scheduler found. Please configure a scheduler with name '"
              + DEFAULT_SCHEDULER_NAME
              + "'");
    }
  }

  @Override
  public Scheduler getScheduler(String schedulerName) {

    if (!schedulerByName.containsKey(schedulerName)) {
      logger.warn(
          "Scheduler with name '{}' not found, scheduling job on default scheduler", schedulerName);
      schedulerName = DEFAULT_SCHEDULER_NAME;
    }

    return schedulerByName.get(schedulerName);
  }

  @Override
  public List<Scheduler> getSchedulers() {
    return schedulers;
  }
}
