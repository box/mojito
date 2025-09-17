package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
public class ImportTextUnitJob extends QuartzPollableJob<ImportTextUnitJobInput, Void> {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ImportTextUnitJob.class);

  @Autowired TextUnitBatchImporterService textUnitBatchImporterService;

  @Autowired PollableTaskService pollableTaskService;

  @Override
  public Void call(ImportTextUnitJobInput input) throws Exception {
    logger.debug("Run ImportTextUnitJob");
    List<TextUnitDTO> textUnitDTOs = input.getTextUnitDTOs();

    PollableTask currentTask = getCurrentPollableTask();
    String taskId = currentTask != null ? currentTask.getId().toString() : "unknown";
    String importComment = String.format("Imported via workbench UI (taskId=%s)", taskId);

    textUnitBatchImporterService.importTextUnits(
        textUnitDTOs,
        input.getIntegrityChecksType(),
        TextUnitBatchImporterService.ImportMode.ALWAYS_IMPORT,
        importComment);
    return null;
  }
}
