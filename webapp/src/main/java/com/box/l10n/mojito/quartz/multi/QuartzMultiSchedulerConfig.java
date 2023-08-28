package com.box.l10n.mojito.quartz.multi;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import com.box.l10n.mojito.monitoring.QuartzMetricsReportingJobListener;
import com.box.l10n.mojito.quartz.AutoWiringSpringBeanJobFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "l10n.org.multi-quartz.enabled", havingValue = "true")
public class QuartzMultiSchedulerConfig {

  Logger logger = LoggerFactory.getLogger(QuartzMultiSchedulerConfig.class);

  @Autowired ApplicationContext applicationContext;

  @Autowired
  QuartzMultiSchedulerConfigurationProperties quartzMultiSchedulerConfigurationProperties;

  @Autowired(required = false)
  QuartzMetricsReportingJobListener quartzMetricsReportingJobListener;

  @Autowired(required = false)
  List<Trigger> triggers = new ArrayList<>();

  @PostConstruct
  public void createSchedulers() {

    for (Map.Entry<String, SchedulerConfigurationProperties> entry :
        quartzMultiSchedulerConfigurationProperties.getSchedulers().entrySet()) {
      Map<String, String> quartzProps = entry.getValue().getQuartz();
      Properties properties = new Properties();

      for (Map.Entry<String, String> quartzEntry : quartzProps.entrySet()) {
        properties.put("org.quartz." + quartzEntry.getKey(), quartzEntry.getValue());
        logger.debug("org.quartz.{}={}", quartzEntry.getKey(), quartzEntry.getValue());
      }
      SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
      applySchedulerFactoryProperties(schedulerFactory, properties);

      if (entry.getKey().equalsIgnoreCase(DEFAULT_SCHEDULER_NAME)) {
        schedulerFactory.setTriggers(triggers.toArray(new Trigger[] {}));
      }

      schedulerFactory.setBeanName(entry.getKey());

      ConfigurableApplicationContext configurableApplicationContext =
          (ConfigurableApplicationContext) applicationContext;
      configurableApplicationContext
          .getBeanFactory()
          .initializeBean(schedulerFactory, entry.getKey());
      configurableApplicationContext.getBeanFactory().autowireBean(schedulerFactory);
      configurableApplicationContext
          .getBeanFactory()
          .registerSingleton(entry.getKey(), schedulerFactory);
    }
  }

  public void applySchedulerFactoryProperties(
      SchedulerFactoryBean schedulerFactory, Properties quartzProperties) {
    schedulerFactory.setQuartzProperties(quartzProperties);
    schedulerFactory.setJobFactory(createSpringBeanJobFactory());
    schedulerFactory.setOverwriteExistingJobs(true);
    schedulerFactory.setAutoStartup(false);

    if (quartzMetricsReportingJobListener != null) {
      schedulerFactory.setGlobalJobListeners(quartzMetricsReportingJobListener);
    }
  }

  public SpringBeanJobFactory createSpringBeanJobFactory() {
    AutoWiringSpringBeanJobFactory jobFactory = new AutoWiringSpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);
    return jobFactory;
  }
}
