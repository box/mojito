package com.box.l10n.mojito.monitoring;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

@ConditionalOnProperty(value = "l10n.management.metrics.quartz.enabled", havingValue = "true")
@Component
public class QuartzMetricsReportingJobListener implements JobListener {

    private MeterRegistry meterRegistry;

    public QuartzMetricsReportingJobListener(@Autowired MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        Tags timerTags = Tags.of("jobGroup", context.getJobDetail().getKey().getGroup(),
                                 "jobClass", context.getJobDetail().getJobClass().getSimpleName());

        meterRegistry.timer("quartz.jobs.execution", timerTags)
                     .record(context.getJobRunTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) { }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) { }

}
