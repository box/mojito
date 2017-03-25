package com.box.l10n.mojito.rest.entity;

/**
 *
 * @author jaurambault
 */
public class ImportLocalizedAssetBody {

    public enum StatusForSourceEqTarget {
        SKIPPED,
        REVIEW_NEEDED,
        TRANSLATION_NEEDED,
        APPROVED
    };

    String content;

    StatusForSourceEqTarget statusSourceEqTarget;

    public ImportLocalizedAssetBody() {
    }
    
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public StatusForSourceEqTarget getStatusSourceEqTarget() {
        return statusSourceEqTarget;
    }

    public void setStatusSourceEqTarget(StatusForSourceEqTarget statusSourceEqTarget) {
        this.statusSourceEqTarget = statusSourceEqTarget;
    }

}
