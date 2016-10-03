package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import java.util.HashSet;
import java.util.Set;
import net.sf.okapi.common.Event;
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
public class AssetExtractionStep extends AbstractMd5ComputationStep {

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
        event = super.handleTextUnit(event);

        if (textUnit.isTranslatable()) {
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
