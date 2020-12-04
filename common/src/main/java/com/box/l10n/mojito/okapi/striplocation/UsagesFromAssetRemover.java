package com.box.l10n.mojito.okapi.striplocation;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.FilterConfigurationMappers;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.filters.CopyFormsOnImport;
import com.box.l10n.mojito.okapi.filters.FilterOptions;
import com.box.l10n.mojito.okapi.steps.CheckForDoNotTranslateStep;
import com.box.l10n.mojito.okapi.steps.FilterEventsToInMemoryRawDocumentStep;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * To remove location information from an asset (source or localzied files).
 * <p>
 * Assume the filter supports removing location. Filter should honor "removeLocation" filter options
 */
@Component
public class UsagesFromAssetRemover {

    static Logger logger = LoggerFactory.getLogger(UsagesFromAssetRemover.class);

    @Autowired
    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper;

    @Autowired
    FilterConfigurationMappers filterConfigurationMappers;

    public String removeUsages(String assetPath,
                               String assetContent,
                               FilterConfigIdOverride filterConfigIdOverride,
                               List<String> filterOptions,
                               String targetLocaleId) throws UnsupportedAssetFilterTypeException {

        logger.debug("Configuring pipeline");
        IPipelineDriver driver = new PipelineDriver();

        driver.addStep(new RawDocumentToFilterEventsStep());
        driver.addStep(new CheckForDoNotTranslateStep());

        FilterEventsToInMemoryRawDocumentStep filterEventsToInMemoryRawDocumentStep = new FilterEventsToInMemoryRawDocumentStep();
        driver.addStep(filterEventsToInMemoryRawDocumentStep);

        logger.debug("Adding all supported filters to the pipeline driver");
        driver.setFilterConfigurationMapper(filterConfigurationMappers.getConfiguredFilterConfigurationMapper());

        RawDocument rawDocument;

        if (targetLocaleId == null) {
            rawDocument = new RawDocument(assetContent, LocaleId.ENGLISH);
        } else {
            rawDocument = new RawDocument(assetContent, LocaleId.ENGLISH, targetLocaleId);
            rawDocument.setAnnotation(new CopyFormsOnImport());
        }

        String filterConfigId = null;

        if (filterConfigIdOverride != null) {
            filterConfigId = filterConfigIdOverride.getOkapiFilterId();
        } else {
            filterConfigId = assetPathToFilterConfigMapper.getFilterConfigIdFromPath(assetPath);
        }

        rawDocument.setFilterConfigId(filterConfigId);
        logger.debug("Set filter config {} for asset {}", filterConfigId, assetPath);

        // TODO clean!!
        filterOptions = filterOptions == null ? new ArrayList<>() : filterOptions;
        filterOptions.add("removeLocation=true");

        logger.debug("Filter options: {}", filterOptions);
        rawDocument.setAnnotation(new FilterOptions(filterOptions));

        driver.addBatchItem(rawDocument);

        logger.debug("Start processing batch");
        driver.processBatch();

        String locationRemoved = filterEventsToInMemoryRawDocumentStep.getOutput(rawDocument);
        return locationRemoved;
    }

}
