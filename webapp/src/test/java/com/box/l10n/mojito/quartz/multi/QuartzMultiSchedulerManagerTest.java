package com.box.l10n.mojito.quartz.multi;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.quartz.QuartzConfig;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      QuartzMultiSchedulerConfigurationProperties.class,
      QuartzMultiSchedulerConfig.class,
      QuartzMultiSchedulerManager.class,
      QuartzConfig.class,
      QuartzMultiSchedulerManagerTest.class
    },
    properties = {
      "l10n.org.multi-quartz.enabled=true",
      "l10n.org.multi-quartz.schedulers.default.quartz.threadPool.threadCount=10",
      "l10n.org.multi-quartz.schedulers.scheduler2.quartz.threadPool.threadCount=5"
    })
@EnableConfigurationProperties(QuartzMultiSchedulerConfigurationProperties.class)
public class QuartzMultiSchedulerManagerTest {

  @Autowired QuartzMultiSchedulerManager quartzMultiSchedulerManager;

  @Test
  public void testSchedulerGeneratedFromProperties() throws SchedulerException {
    Scheduler defaultScheduler = quartzMultiSchedulerManager.getScheduler("default");
    Scheduler scheduler2 = quartzMultiSchedulerManager.getScheduler("scheduler2");

    assertEquals("default", defaultScheduler.getSchedulerName());
    assertEquals(10, defaultScheduler.getMetaData().getThreadPoolSize());
    assertEquals("scheduler2", scheduler2.getSchedulerName());
    assertEquals(5, scheduler2.getMetaData().getThreadPoolSize());
    await()
        .atMost(Duration.of(10, ChronoUnit.SECONDS))
        .until(() -> defaultScheduler.isStarted() && scheduler2.isStarted());
  }

  @Test(expected = QuartzMultiSchedulerException.class)
  public void testExceptionThrownIfNoDefaultSchedulerConfigured() throws SchedulerException {
    List<Scheduler> schedulers = new ArrayList<>();
    Scheduler schedulerMock = mock(Scheduler.class);
    when(schedulerMock.getSchedulerName()).thenReturn("scheduler1");
    schedulers.add(schedulerMock);

    QuartzMultiSchedulerManager quartzMultiSchedulerManager = new QuartzMultiSchedulerManager();
    quartzMultiSchedulerManager.schedulers = schedulers;
    quartzMultiSchedulerManager.init();
  }

  @Test(expected = QuartzMultiSchedulerException.class)
  public void testExceptionThrownIfDuplicateSchedulerConfigured() throws SchedulerException {
    List<Scheduler> schedulers = new ArrayList<>();
    Scheduler schedulerMock = mock(Scheduler.class);
    Scheduler schedulerMock2 = mock(Scheduler.class);
    when(schedulerMock.getSchedulerName()).thenReturn("scheduler1");
    when(schedulerMock2.getSchedulerName()).thenReturn("scheduler1");
    schedulers.add(schedulerMock);
    schedulers.add(schedulerMock2);

    QuartzMultiSchedulerManager quartzMultiSchedulerManager = new QuartzMultiSchedulerManager();
    quartzMultiSchedulerManager.schedulers = schedulers;
    quartzMultiSchedulerManager.init();
  }

  @Test
  public void testDefaultSchedulerReturnedIfRequestedDoesntExist() throws SchedulerException {
    assertEquals(
        "default",
        quartzMultiSchedulerManager.getScheduler("schedulerNotExist").getSchedulerName());
  }

  @Test
  public void testScheduleJobOnSeparateSchedulers() throws SchedulerException {
    Scheduler defaultScheduler = quartzMultiSchedulerManager.getScheduler("default");
    Scheduler scheduler2 = quartzMultiSchedulerManager.getScheduler("scheduler2");

    await()
        .atMost(Duration.of(10, ChronoUnit.SECONDS))
        .until(() -> defaultScheduler.isStarted() && scheduler2.isStarted());

    JobDetail jobDetail = JobBuilder.newJob(TestJob.class).withIdentity("job1", "DYNAMIC").build();
    Trigger trigger =
        TriggerBuilder.newTrigger().withIdentity("trigger1", "DYNAMIC").startNow().build();
    defaultScheduler.scheduleJob(jobDetail, trigger);

    assertTrue(defaultScheduler.checkExists(jobDetail.getKey()));

    assertFalse(scheduler2.checkExists(jobDetail.getKey()));

    await()
        .atMost(Duration.of(10, ChronoUnit.SECONDS))
        .until(() -> jobDetail.getJobDataMap() != null);
    assertEquals("default", TestJob.getExecutingScheduler());

    JobDetail jobDetail2 = JobBuilder.newJob(TestJob.class).withIdentity("job2", "DYNAMIC").build();
    Trigger trigger2 =
        TriggerBuilder.newTrigger().withIdentity("trigger2", "DYNAMIC").startNow().build();
    scheduler2.scheduleJob(jobDetail2, trigger2);

    assertTrue(scheduler2.checkExists(jobDetail2.getKey()));
    assertFalse(defaultScheduler.checkExists(jobDetail2.getKey()));

    await()
        .atMost(Duration.of(10, ChronoUnit.SECONDS))
        .until(() -> jobDetail2.getJobDataMap() != null);
    assertEquals("scheduler2", TestJob.getExecutingScheduler());
  }
}
