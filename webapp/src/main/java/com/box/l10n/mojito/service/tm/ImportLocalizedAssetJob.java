package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImportLocalizedAssetJob extends QuartzPollableJob<ImportLocalizedAssetJobInput, Void> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ImportLocalizedAssetJob.class);

    @Autowired
    TMService tmService;

    @Override
    public Void call(ImportLocalizedAssetJobInput input) throws Exception {

        logger.debug("Run ImportLocalizedAssetJob");
        tmService.importLocalizedAsset(
                input.getAssetId(),
                input.getContent(),
                input.getLocaleId(),
                input.getStatusForEqualtarget(),
                input.getFilterConfigIdOverride());

        return null;
    }


}
