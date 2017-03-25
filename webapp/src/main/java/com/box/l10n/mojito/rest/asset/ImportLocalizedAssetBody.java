package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep;

/**
 * @author jaurambault
 */
public class ImportLocalizedAssetBody {

    /**
     * bcp47 tag of the locale content
     */
    String bcp47Tag;

    /**
     * content to be imported
     */
    String content;

    ImportTranslationsFromLocalizedAssetStep.StatusForSourceEqTarget sourceEqualTargetProcessing;

    public ImportLocalizedAssetBody() {
    }

    public ImportLocalizedAssetBody(String bcp47Tag, String content) {
        this.bcp47Tag = bcp47Tag;
        this.content = content;
    }

    public String getBcp47Tag() {
        return bcp47Tag;
    }

    public void setBcp47Tag(String bcp47Tag) {
        this.bcp47Tag = bcp47Tag;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ImportTranslationsFromLocalizedAssetStep.StatusForSourceEqTarget getSourceEqualTargetProcessing() {
        return sourceEqualTargetProcessing;
    }

    public void setSourceEqualTargetProcessing(ImportTranslationsFromLocalizedAssetStep.StatusForSourceEqTarget sourceEqualTargetProcessing) {
        this.sourceEqualTargetProcessing = sourceEqualTargetProcessing;
    }

}
