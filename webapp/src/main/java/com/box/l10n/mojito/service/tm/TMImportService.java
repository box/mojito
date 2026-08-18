package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.ImportExportTextUnitUtils;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault
 */
@Component
public class TMImportService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TMImportService.class);

  @Autowired LocaleService localeService;

  @Autowired TMService tmService;

  @Autowired AssetRepository assetRepository;

  @Autowired AssetService assetService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired ObjectMapper objectMapper;

  @Autowired ImportExportTextUnitUtils importExportTextUnitUtils;

  /**
   * Import the exported XLIFF using Okapi driver into repository asynchronously.
   *
   * @param repository
   * @param xliffContent
   * @param updateTM indicates if the TM should be updated or if the translation can be imported
   *     assuming that there is no translation yet.
   * @return a {@link PollableFuture} to track the import progress
   */
  @Pollable(async = true, message = "Importing XLIFF")
  public PollableFuture<Void> importXLIFF(
      Repository repository, String xliffContent, boolean updateTM) {

    ImportExportedXliffStep importExportedXliffStep =
        new ImportExportedXliffStep(repository, xliffContent, updateTM);
    doImportXLIFF(importExportedXliffStep, xliffContent);
    return new PollableFutureTaskResult<>();
  }

  /**
   * Import the exported XLIFF using Okapi driver for a specific asset.
   *
   * @param assetId
   * @param xliffContent
   * @param updateTM
   */
  public void importXLIFF(Long assetId, String xliffContent, boolean updateTM) {

    Asset asset = assetRepository.findById(assetId).orElse(null);
    ImportExportedXliffStep importExportedXliffStep =
        new ImportExportedXliffStep(asset, xliffContent, updateTM);
    doImportXLIFF(importExportedXliffStep, xliffContent);
  }

  @Transactional
  private void doImportXLIFF(ImportExportedXliffStep importExportedXliffStep, String xliffContent) {

    IPipelineDriver driver = new PipelineDriver();
    XLIFFFilter xliffFilter = new XLIFFFilter();
    driver.addStep(new RawDocumentToFilterEventsStep(xliffFilter));

    importExportedXliffStep.setXliffFilter(xliffFilter);
    driver.addStep(importExportedXliffStep);

    RawDocument rawDocument = new RawDocument(xliffContent, LocaleId.ENGLISH);

    driver.addBatchItem(rawDocument);

    logger.debug("Start importing XLIFF");
    driver.processBatch();
  }
}
