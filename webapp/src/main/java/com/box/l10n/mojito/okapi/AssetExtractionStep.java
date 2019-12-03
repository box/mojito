package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.okapi.filters.PluralFormAnnotation;
import com.box.l10n.mojito.okapi.filters.UsagesAnnotation;
import com.box.l10n.mojito.okapi.steps.AbstractMd5ComputationStep;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.pluralform.PluralFormService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.MoreObjects;
import net.sf.okapi.common.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author aloison
 */
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

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    PluralFormService pluralFormService;

    Set<String> assetTextUnitMD5s;

    AssetExtraction assetExtraction;

    List<String> md5sToSkip;

    /**
     * @param assetExtractionId ID of the assetExtraction object to associate
     *                          this step with
     * @param md5sToSkip
     */
    public AssetExtractionStep(AssetExtraction assetExtraction, List<String> md5sToSkip) {
        this.assetExtraction = assetExtraction;
        this.md5sToSkip = MoreObjects.firstNonNull(md5sToSkip, Collections.emptyList());
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
            if (md5sToSkip.contains(md5)) {
                logger.debug("{} in list of md5s to be skipped", md5);
            } else if (!assetTextUnitMD5s.contains(md5)) {
                assetTextUnitMD5s.add(md5);

                PluralFormAnnotation annotation = textUnit.getAnnotation(PluralFormAnnotation.class);
                PluralForm pluralForm = null;
                String pluralFormOther = null;

                if (annotation != null) {
                    pluralForm = pluralFormService.findByPluralFormString(annotation.getName());
                    pluralFormOther = annotation.getOtherName();
                }

                assetExtractionService.createAssetTextUnit(
                        assetExtraction.getId(),
                        name,
                        source,
                        comments,
                        pluralForm,
                        pluralFormOther,
                        false,
                        getUsages(),
                        assetExtraction.getAssetContent().getBranch());

            } else {
                logger.debug("Duplicate assetTextUnit found, skip it");
            }
        }

        return event;
    }

    Set<String> getUsages() {
        Set<String> usages = null;

        UsagesAnnotation usagesAnnotation = textUnit.getAnnotation(UsagesAnnotation.class);

        if (usagesAnnotation != null) {
            usages = usagesAnnotation.getUsages();
        }

        return usages;
    }

}
