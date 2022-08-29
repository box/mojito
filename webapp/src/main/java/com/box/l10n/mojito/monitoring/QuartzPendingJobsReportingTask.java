package com.box.l10n.mojito.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("!disablescheduling")
@Component
@ConditionalOnProperty(
    value = "l10n.management.metrics.quartz.sql-queue-monitoring.enabled",
    havingValue = "true")
public class QuartzPendingJobsReportingTask {

  private JdbcTemplate jdbcTemplate;
  private MeterRegistry meterRegistry;
  private Map<String, AtomicLong> queueSizes;

  public QuartzPendingJobsReportingTask(
      @Autowired DataSource dataSource, @Autowired MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.queueSizes = new ConcurrentHashMap<>();
  }

  @Scheduled(
      fixedRateString = "${l10n.management.metrics.quartz.sql-queue-monitoring.execution-rate}")
  public void reportPendingJobs() {
    Map<String, PendingJob> results = fetchResults();
    updateQueueSizes(results);
    results.forEach(this::registerJobQueueSize);
  }

  private void registerJobQueueSize(String key, PendingJob pendingJob) {
    queueSizes.computeIfAbsent(key, k -> createGauge(pendingJob)).set(pendingJob.count);
  }

  private AtomicLong createGauge(PendingJob pendingJob) {
    return meterRegistry.gauge(
        "quartz.pending.jobs",
        Tags.of("jobClass", pendingJob.jobClass, "jobGroup", pendingJob.jobGroup),
        new AtomicLong(pendingJob.count));
  }

  private void updateQueueSizes(Map<String, PendingJob> pendingJobs) {
    queueSizes.forEach(
        (key, val) -> {
          Long size = pendingJobs.containsKey(key) ? pendingJobs.get(key).count : 0L;
          // If the list of yielded results doesn't contains the pendingjob, then we update its
          // value to be zero
          queueSizes.get(key).set(size);
        });
  }

  Map<String, PendingJob> fetchResults() {
    List<PendingJob> result =
        jdbcTemplate.query(
            "SELECT job_class_name, job_group, COUNT(*) FROM QRTZ_JOB_DETAILS GROUP BY job_class_name, job_group",
            (rs, num) ->
                new PendingJob(extractClassName(rs.getString(1)), rs.getString(2), rs.getLong(3)));

    return result.stream().collect(Collectors.toMap(PendingJob::getKey, Function.identity()));
  }

  static String extractClassName(String input) {
    String[] parts = input.split("\\.");
    return parts.length > 0 ? parts[parts.length - 1] : "";
  }

  /*
   * This class represents data associated with a group of jobs pending to be executed in our Quartz instance
   * */
  static class PendingJob {
    public String jobClass;
    public String jobGroup;
    public Long count;

    public PendingJob(String jobClass, String jobGroup, Long count) {
      this.jobClass = jobClass;
      this.jobGroup = jobGroup;
      this.count = count;
    }

    public String getKey() {
      return jobClass + "-" + jobGroup;
    }
  }
}
