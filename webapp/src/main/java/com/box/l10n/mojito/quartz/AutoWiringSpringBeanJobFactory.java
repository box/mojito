package com.box.l10n.mojito.quartz;

import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

public final class AutoWiringSpringBeanJobFactory extends SpringBeanJobFactory
    implements ApplicationContextAware {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AutoWiringSpringBeanJobFactory.class);

  private transient AutowireCapableBeanFactory beanFactory;

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    beanFactory = applicationContext.getAutowireCapableBeanFactory();
  }

  @Override
  protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
    try {
      final Object job = super.createJobInstance(bundle);
      beanFactory.autowireBean(job);
      return job;
    } catch (Throwable t) {
      logger.error("Can't create a Quartz job instance, this is a critical error", t);
      throw t;
    }
  }
}
