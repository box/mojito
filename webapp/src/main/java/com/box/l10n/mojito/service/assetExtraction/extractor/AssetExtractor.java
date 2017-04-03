package com.box.l10n.mojito.service.assetExtraction.extractor;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.okapi.AssetExtractionStep;
import com.box.l10n.mojito.okapi.CheckForDoNotTranslateStep;
import com.box.l10n.mojito.okapi.POExtraPluralAnnotation;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.filters.CSVFilter;
import com.box.l10n.mojito.okapi.filters.MacStringsFilter;
import com.box.l10n.mojito.okapi.filters.POFilter;
import com.box.l10n.mojito.okapi.filters.XMLFilter;
import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;
import com.box.l10n.mojito.rest.asset.SourceAsset;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.DefaultFilters;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for setting up the extraction pipeline and
 * extracting text units from assets.
 *
 * @author aloison
 */
@Component
public class AssetExtractor {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetExtractor.class);

    @Autowired
    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper;

    /**
     * Processes the {@link Asset} given the associated {@link AssetExtraction}.
     * The CSV format should follow the old Box WebApp syntax.
     *
     * @param assetExtraction the Asset extraction
     * @param filterConfigIdOverride Optional, can be null. Allows to specify
     * a specific Okapi filter to use to process the asset
     * @param parentTask
     */
    @Transactional
    @Pollable(message = "Extracting text units from asset")
    public void performAssetExtraction(
            AssetExtraction assetExtraction,
            FilterConfigIdOverride filterConfigIdOverride,
            @ParentTask PollableTask parentTask) {

        logger.debug("Configuring pipeline");

        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep());
        driver.addStep(new CheckForDoNotTranslateStep());
        driver.addStep(new AssetExtractionStep(assetExtraction.getId()));

        //TODO(10) Is this actually used as we have our own logic to set the filter to be used, see following todo
        logger.debug("Adding all supported filters to the pipeline driver");
        driver.setFilterConfigurationMapper(getConfiguredFilterConfigurationMapper());

        Asset asset = assetExtraction.getAsset();
        RawDocument rawDocument = new RawDocument(asset.getContent(), LocaleId.ENGLISH);
        rawDocument.setAnnotation(new POExtraPluralAnnotation());

        //TODO(P1) I think Okapi already implement this logic
        String filterConfigId;
        
        if (filterConfigIdOverride != null) {
            filterConfigId = filterConfigIdOverride.getOkapiFilterId();
        } else {
            filterConfigId = getFilterConfigIdForAsset(asset);
        }
        
        rawDocument.setFilterConfigId(filterConfigId);
        logger.debug("Set filter config {} for asset {}", filterConfigId, asset.getPath());
        
        driver.addBatchItem(rawDocument);

        logger.debug("Start processing batch");
        driver.processBatch();
    }

    /**
     * @return A {@link FilterConfigurationMapper}, which has been configured with the default mappings
     */
    public IFilterConfigurationMapper getConfiguredFilterConfigurationMapper() {

        IFilterConfigurationMapper mapper = new FilterConfigurationMapper();

        // Adding default filter mappings
        DefaultFilters.setMappings(mapper, false, true);

        // Adding custom filters mappings
        mapper.addConfigurations(CSVFilter.class.getName());
        mapper.addConfigurations(POFilter.class.getName());
        mapper.addConfigurations(XMLFilter.class.getName());
        mapper.addConfigurations(MacStringsFilter.class.getName());

        return mapper;
    }

    /**
     * @param asset The asset to be extracted
     * @return The ID of the filter config to use to extract text units for the given asset
     * @throws UnsupportedAssetFilterTypeException If no filter config found for the given asset
     */
    public String getFilterConfigIdForAsset(Asset asset) {

        String assetPath = asset.getPath();
        return assetPathToFilterConfigMapper.getFilterConfigIdFromPath(assetPath);
    }
}
