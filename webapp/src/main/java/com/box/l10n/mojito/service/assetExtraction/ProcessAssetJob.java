package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessAssetJob extends QuartzPollableJob<ProcessAssetJobInput, Void> {

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Override
    public Void call(ProcessAssetJobInput input) throws Exception {

        assetExtractionService.processAsset(
                input.getUsername(),
                input.getAssetContentId(),
                input.getFilterConfigIdOverride(),
                getCurrentPollableTask());

        return null;
    }
}
