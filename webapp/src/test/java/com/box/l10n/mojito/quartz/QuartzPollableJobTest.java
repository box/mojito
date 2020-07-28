package com.box.l10n.mojito.quartz;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pollableTask.PollableTaskRepository;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import static com.box.l10n.mojito.quartz.QuartzPollableJob.INPUT;
import static com.box.l10n.mojito.quartz.QuartzPollableJob.POLLABLE_TASK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuartzPollableJobTest extends ServiceTestBase {

    @Autowired
    AutowireCapableBeanFactory beanFactory;

    @Autowired
    PollableTaskRepository pollableTaskRepository;

    @Autowired
    @Qualifier("fail_on_unknown_properties_false")
    ObjectMapper objectMapper;

    JobExecutionContext context;
    JobDataMap dataMap;
    StubPollableJob job;
    PollableTask pollableTask;

    @Before
    public void setUp() {
        job = new StubPollableJob();
        beanFactory.autowireBean(job);

        pollableTask = new PollableTask();
        pollableTask.setName("Name");
        pollableTask.setMessage("Message");
        pollableTask = pollableTaskRepository.save(pollableTask);

        dataMap = new JobDataMap();
        dataMap.put(POLLABLE_TASK_ID, pollableTask.getId());
        dataMap.put(INPUT, objectMapper.writeValueAsStringUnchecked(""));

        // We dont really need to build a real-world JobExecutionContext
        context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(dataMap);
    }

    @Test
    public void testPollableTaskMDCKeyIsAdded() throws Exception {

        // Intercept the QuartzPollableJob logger to fetch events added to it
        Logger jobLogger = (Logger) LoggerFactory.getLogger(QuartzPollableJob.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        jobLogger.addAppender(listAppender);
        jobLogger.setLevel(Level.ALL);

        job.execute(context);

        assertThat(listAppender.list).isNotEmpty();
        assertThat(listAppender.list).allSatisfy(event -> {
            assertThat(event.getMDCPropertyMap()).containsKeys(POLLABLE_TASK_ID);
            assertThat(event.getMDCPropertyMap().get(POLLABLE_TASK_ID)).isEqualTo(pollableTask.getId().toString());
        });
    }

    private static class StubPollableJob extends QuartzPollableJob<String, String> {

        @Override
        public String call(String input) {
            return input;
        }
    }

}
