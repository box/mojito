package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
public class ThirdPartySyncJob extends QuartzPollableJob<ThirdPartySyncJobInput, Void> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ThirdPartySyncJob.class);

    @Autowired
    ThirdPartyService thirdPartyService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Override
    public Void call(ThirdPartySyncJobInput input) throws Exception {
        logger.debug("Run ThirdPartyMapJob");
        thirdPartyService.syncMojitoWithThirdPartyTMS(
                input.getRepositoryId(),
                input.getThirdPartyProjectId(),
                input.getActions(),
                input.getPluralSeparator(),
                input.getLocaleMapping(),
                input.getOptions());
        return null;
    }
}
