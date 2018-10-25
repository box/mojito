package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
public class ImportTextUnitJob extends QuartzPollableJob<ImportTextUnitJobInput, Void> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ImportTextUnitJob.class);

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Override
    public Void call(ImportTextUnitJobInput input) throws Exception {
        logger.debug("Run ImportTextUnitJob");
        textUnitBatchImporterService.importTextUnits(
                input.getTextUnitDTOs(),
                input.isIntegrityCheckSkipped(),
                input.isIntegrityCheckKeepStatusIfFailedAndSameTarget());
        return null;
    }
}
