package com.box.l10n.mojito.service.sla;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * @author jeanaurambault
 */
@Profile("!disablescheduling")
@Component
public class SlaCheckerScheduler {

    static Logger logger = LoggerFactory.getLogger(SlaCheckerScheduler.class);

    @Value("${l10n.slaChecker.incidentCheck.cron:}")
    String incidentCheckCron;

    @Autowired
    TaskScheduler taskScheduler;

    @Autowired
    SlaCheckerService slaCheckerService;

    @PostConstruct
    public void checkForIncidentOnSchedule() throws InterruptedException {
        if (!incidentCheckCron.isEmpty()) {
            taskScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    slaCheckerService.checkForIncidents();
                }
            }, new CronTrigger(incidentCheckCron));
        }
    }

}
