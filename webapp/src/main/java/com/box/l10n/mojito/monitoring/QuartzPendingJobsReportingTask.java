package com.box.l10n.mojito.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

@Profile("!disablescheduling")
@Component
@ConditionalOnProperty(value = "l10n.management.metrics.quartz.sql-queue-monitoring.enabled", havingValue = "true")
public class QuartzPendingJobsReportingTask {

    private JdbcTemplate jdbcTemplate;
    private MeterRegistry meterRegistry;

    public QuartzPendingJobsReportingTask(@Autowired DataSource dataSource,
                                          @Autowired MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Scheduled(fixedRateString = "${l10n.management.metrics.quartz.sql-queue-monitoring.execution-rate}")
    public void reportPendingJobs() {
        fetchResults().forEach(this::reportResults);
    }

    private void reportResults(PendingJob pendingJob){
        meterRegistry.gauge("quartz.pending.jobs",
                            Tags.of("jobClass", pendingJob.jobClass, "jobGroup", pendingJob.jobGroup),
                            pendingJob.count);
    }

    List<PendingJob> fetchResults(){
        return jdbcTemplate.query(
            "SELECT job_class_name, job_group, COUNT(*) FROM QRTZ_JOB_DETAILS GROUP BY job_class_name, job_group",
            (rs, num) -> new PendingJob(extractClassName(rs.getString(1)), rs.getString(2), rs.getLong(3))
        );
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
        public Long   count;

        public PendingJob(String jobClass, String jobGroup, Long count) {
            this.jobClass = jobClass;
            this.jobGroup = jobGroup;
            this.count = count;
        }
    }

}
