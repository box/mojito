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

    FilterConfigIdOverride filterConfigIdOverride;

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

    public FilterConfigIdOverride getFilterConfigIdOverride() {
        return filterConfigIdOverride;
    }

    public void setFilterConfigIdOverride(FilterConfigIdOverride filterConfigIdOverride) {
        this.filterConfigIdOverride = filterConfigIdOverride;
    }

}
