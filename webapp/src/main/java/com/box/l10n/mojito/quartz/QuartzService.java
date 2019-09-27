package com.box.l10n.mojito.quartz;


import com.box.l10n.mojito.service.asset.AssetService;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class QuartzService {

    /**
     * logger
     */
    static Logger logger = getLogger(QuartzService.class);

    @Autowired
    Scheduler scheduler;

    public List<String> getDynamicJobs() throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(DYNAMIC_GROUP_NAME));
        return jobKeys.stream().map(jobKey -> jobKey.getName()).collect(Collectors.toList());
    }

    public void deleteAllDynamicJobs() throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(DYNAMIC_GROUP_NAME));
        scheduler.deleteJobs(new ArrayList<>(jobKeys));
    }

}
