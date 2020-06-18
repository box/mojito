package com.box.l10n.mojito.monitoring;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class EmptyTestJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
    }
}
