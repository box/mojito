package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeanaurambault
 */
public class ITSEnginePatchFilterTest {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetExtractionService.class);

    @Test
    public void testITS() throws Exception {
        String content = Resources.toString(Resources.getResource("com/box/l10n/mojito/okapi/filters/itsenginepatch_comment.xml"), Charsets.UTF_8);
        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep());
        RawDocument rawDocument = new RawDocument(content, LocaleId.ENGLISH);
        FilterConfigurationMapper filterConfigurationMapper = new FilterConfigurationMapper();
        filterConfigurationMapper.addConfigurations(ITSEnginePatchFilter.class.getCanonicalName());
        driver.setFilterConfigurationMapper(filterConfigurationMapper);
        rawDocument.setFilterConfigId(ITSEnginePatchFilter.FILTER_CONFIG_ID);
        driver.addBatchItem(rawDocument);
        driver.processBatch();
    }
}
