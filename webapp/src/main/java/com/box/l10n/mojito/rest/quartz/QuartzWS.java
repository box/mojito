package com.box.l10n.mojito.rest.quartz;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.quartz.QuartzService;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.tm.TMXliffRepository;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class QuartzWS {

    @Autowired
    QuartzService quartzService;

    @RequestMapping(method = RequestMethod.GET, value = "/api/quartz/jobs/dynamic")
    public List<String> getAllDynamicJobs() throws SchedulerException {
        return quartzService.getDynamicJobs();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/api/quartz/jobs/dynamic")
    public void deleteAllDynamicJobs() throws SchedulerException {
        quartzService.deleteAllDynamicJobs();
    }

}
