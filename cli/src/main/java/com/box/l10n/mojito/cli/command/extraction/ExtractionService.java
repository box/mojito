package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.CommandHelper;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.io.Files;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.FilterConfigurationMappers;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.filters.FilterOptions;
import com.box.l10n.mojito.okapi.steps.CheckForDoNotTranslateStep;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class ExtractionService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ExtractionService.class);

    @Autowired
    @Qualifier("outputIndented")
    ObjectMapper objectMapper;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper;

    @Autowired
    FilterConfigurationMappers filterConfigurationMappers;

    public void fileMatchToAssetExtractionAndSaveToJsonFile(FileMatch sourceFileMatch, List<String> filterOptions,
                                                            String extractionName, ExtractionsPaths extractionsPaths) throws CommandException {
        AssetExtraction assetExtraction = fileMatchToAssetExtraction(sourceFileMatch, filterOptions, extractionName);
        Path assetExtractionPath = extractionsPaths.assetExtractionPath(sourceFileMatch, extractionName);
        objectMapper.createDirectoriesAndWrite(assetExtractionPath, assetExtraction);
    }

    AssetExtraction fileMatchToAssetExtraction(FileMatch sourceFileMatch, List<String> filterOptions, String extractionName) throws CommandException {
        List<TextUnit> textUnits = getExtractionTextUnitsForSourceFileMatch(sourceFileMatch, filterOptions);

        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setTextUnits(textUnits);
        assetExtraction.setName(extractionName);
        assetExtraction.setFilterOptions(filterOptions);
        assetExtraction.setFileType(sourceFileMatch.getFileType().getClass().getSimpleName());

        return assetExtraction;
    }

    List<TextUnit> getExtractionTextUnitsForSourceFileMatch(FileMatch sourceFileMatch, List<String> filterOptions) {
        String sourcePath = sourceFileMatch.getSourcePath();
        String assetContent = commandHelper.getFileContentWithXcodePatch(sourceFileMatch);
        FilterConfigIdOverride filterConfigIdOverride = sourceFileMatch.getFileType().getFilterConfigIdOverride();

        try {
            return getExtractionTextUnitsForAsset(sourcePath, assetContent, filterConfigIdOverride, filterOptions);
        } catch (UnsupportedAssetFilterTypeException uasft) {
            throw new RuntimeException("Source file match must be for a supported file type", uasft);
        }
    }

    List<TextUnit> getExtractionTextUnitsForAsset(String assetPath,
                                                  String assetContent,
                                                  FilterConfigIdOverride filterConfigIdOverride,
                                                  List<String> filterOptions) throws UnsupportedAssetFilterTypeException {

        logger.debug("Configuring pipeline");

        IPipelineDriver driver = new PipelineDriver();

        driver.addStep(new RawDocumentToFilterEventsStep());
        driver.addStep(new CheckForDoNotTranslateStep());
        ConvertToExtractionTextUnitsStep convertToExtractionTextUnitsStep = new ConvertToExtractionTextUnitsStep();
        driver.addStep(convertToExtractionTextUnitsStep);

        logger.debug("Adding all supported filters to the pipeline driver");
        driver.setFilterConfigurationMapper(filterConfigurationMappers.getConfiguredFilterConfigurationMapper());

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

        return convertToExtractionTextUnitsStep.getTextUnits();
    }

    public void deleteExtractionDirectoryIfExists(ExtractionsPaths extractionsPaths, String extractionName) {
        logger.debug("Delete the extraction directory for name: {}", extractionName);
        Files.deleteRecursivelyIfExists(extractionsPaths.extractionPath(extractionName));
    }

}
