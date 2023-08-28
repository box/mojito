package com.box.l10n.mojito.quartz.multi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      QuartzMultiSchedulerConfigurationProperties.class,
      QuartzMultiSchedulerConfigurationPropertiesTest.class
    },
    properties = {
      "l10n.org.multi-quartz.enabled=true",
      "l10n.org.multi-quartz.schedulers.default.quartz.threadPool.threadCount=10",
      "l10n.org.multi-quartz.schedulers.lowPriority.quartz.threadPool.threadCount=5"
    })
@EnableConfigurationProperties(QuartzMultiSchedulerConfigurationProperties.class)
public class QuartzMultiSchedulerConfigurationPropertiesTest {

  @Autowired
  QuartzMultiSchedulerConfigurationProperties quartzMultiSchedulerConfigurationProperties;

  @Test
  public void testSchedulersConfig() {
    Map<String, SchedulerConfigurationProperties> schedulerConfigurationProperties =
        quartzMultiSchedulerConfigurationProperties.getSchedulers();

    assertThat(schedulerConfigurationProperties).containsOnlyKeys("default", "lowPriority");
    SchedulerConfigurationProperties defaultSchedulerConfigurationProperties =
        schedulerConfigurationProperties.get("default");
    SchedulerConfigurationProperties lowPrioritySchedulerConfigurationProperties =
        schedulerConfigurationProperties.get("lowPriority");
    assertThat(defaultSchedulerConfigurationProperties.getQuartz().get("threadPool.threadCount"))
        .isEqualTo("10");
    assertThat(
            lowPrioritySchedulerConfigurationProperties.getQuartz().get("threadPool.threadCount"))
        .isEqualTo("5");
  }
}
