package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import java.util.HashSet;
import java.util.Set;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author aloison
 */
//TODO(P1) We should probably move this to the service directory and keep
//this folder more for classes related to the okapi framework itself
@Configurable
public class AssetExtractionStep extends BasePipelineStep {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetExtractionStep.class);

    /**
     * when developer does not provide comment, some tools auto-generate comment
     * auto-generated comments should be ignored
     */
    private static final String COMMENT_TO_IGNORE = "No comment provided by engineer";

    private Long assetExtractionId;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    TextUnitUtils textUnitUtils;

    Set<String> assetTextUnitMD5s;

    /**
     * @param assetExtractionId ID of the assetExtraction object to associate
     * this step with
     */
    public AssetExtractionStep(Long assetExtractionId) {
        super();
        this.assetExtractionId = assetExtractionId;
    }

    @Override
    public String getName() {
        return "Asset Content Extraction";
    }

    @Override
    public String getDescription() {
        return "Convert asset content into AssetTextUnits."
                + " Expects: raw document. Sends back: original events.";
    }

    @Override
    protected Event handleStartDocument(Event event) {
        assetTextUnitMD5s = new HashSet<>();
        return super.handleStartDocument(event);
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {
            String name = StringUtils.isEmpty(textUnit.getName()) ? textUnit.getId() : textUnit.getName();
            String source = textUnit.getSource().toString();
            String comments = textUnitUtils.getNote(textUnit);
            if (StringUtils.contains(comments, COMMENT_TO_IGNORE)) {
                comments = null;
            }

            String md5 = DigestUtils.md5Hex(name + source + comments);

            if (!assetTextUnitMD5s.contains(md5)) {
                assetTextUnitMD5s.add(md5);
                assetExtractionService.createAssetTextUnit(assetExtractionId, name, source, comments);
            } else {
                logger.debug("Duplicate assetTextUnit found, skip it");
            }
        }

        return event;
    }

}
