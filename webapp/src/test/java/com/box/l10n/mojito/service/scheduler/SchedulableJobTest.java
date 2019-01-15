package com.box.l10n.mojito.service.scheduler;

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobDataMap;

import static org.junit.Assert.*;

public class SchedulableJobTest {

    @Test
    public void getUniqueId() {
        SchedulableJob schedulableJob = Mockito.mock(SchedulableJob.class, Mockito.CALLS_REAL_METHODS);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("key1", "value1");
        jobDataMap.put("key2", "value2");
        jobDataMap.put("key3", "value3");
        String uniqueId = schedulableJob.getUniqueId(jobDataMap, "key1", "key2");

        assertEquals("value1_value2", uniqueId);
    }
}