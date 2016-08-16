package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.locale.LocaleService;
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

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMImportService.class);

    @Autowired
    LocaleService localeService;

    @Autowired
    TMService tmService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetService assetService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TextUnitUtils textUnitUtils;

    /**
     * Import the exported XLIFF using Okapi driver into repository.
     *
     * @param repository
     * @param xliffContent
     * @param updateTM indicates if the TM should be updated or if the
     * translation can be imported assuming that there is no translation yet.
     */
    public void importXLIFF(
            Repository repository,
            String xliffContent,
            boolean updateTM) {

        ImportExportedXliffStep importExportedXliffStep = new ImportExportedXliffStep(repository, xliffContent, updateTM);
        importXLIFF(importExportedXliffStep, xliffContent);
    }

    /**
     * Import the exported XLIFF using Okapi driver for a specific asset.
     *
     * @param assetId
     * @param xliffContent
     * @param updateTM
     */
    public void importXLIFF(
            Long assetId,
            String xliffContent,
            boolean updateTM) {

        Asset asset = assetRepository.findOne(assetId);
        ImportExportedXliffStep importExportedXliffStep = new ImportExportedXliffStep(asset, xliffContent, updateTM);
        importXLIFF(importExportedXliffStep, xliffContent);
    }

    @Transactional
    private void importXLIFF(ImportExportedXliffStep importExportedXliffStep, String xliffContent) {

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
