package com.box.l10n.mojito.okapi.extractor;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.filters.FilterOptions;
import com.box.l10n.mojito.okapi.steps.CheckForDoNotTranslateStep;
import java.util.List;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssetExtractor {

  static Logger logger = LoggerFactory.getLogger(AssetExtractor.class);

  @Autowired AssetPathToFilterConfigMapper assetPathToFilterConfigMapper;

  @Autowired IFilterConfigurationMapper filterConfigurationMapper;

  public List<AssetExtractorTextUnit> getAssetExtractorTextUnitsForAsset(
      String assetPath,
      String assetContent,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions,
      List<String> md5sToSkip)
      throws UnsupportedAssetFilterTypeException {

    logger.debug("Configuring pipeline");
    IPipelineDriver driver = new PipelineDriver();

    driver.addStep(new RawDocumentToFilterEventsStep());
    driver.addStep(new CheckForDoNotTranslateStep());
    AssetExtractionStep assetExtractionStep = new AssetExtractionStep(md5sToSkip);
    driver.addStep(assetExtractionStep);

    logger.debug("Adding all supported filters to the pipeline driver");
    driver.setFilterConfigurationMapper(filterConfigurationMapper);

    RawDocument rawDocument = new RawDocument(assetContent, LocaleId.ENGLISH);

    String filterConfigId = null;

    if (filterConfigIdOverride != null) {
      filterConfigId = filterConfigIdOverride.getOkapiFilterId();
    } else {
      filterConfigId = assetPathToFilterConfigMapper.getFilterConfigIdFromPath(assetPath);
    }

    rawDocument.setFilterConfigId(filterConfigId);
    logger.debug("Set filter config {} for asset {}", filterConfigId, assetPath);

    logger.debug("Filter options: {}", filterOptions);
    rawDocument.setAnnotation(new FilterOptions(filterOptions));

    driver.addBatchItem(rawDocument);

    logger.debug("Start processing batch");
    driver.processBatch();

    return assetExtractionStep.getAssetExtractorTextUnits();
  }
}
