package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.okapi.InheritanceMode;

/**
 * @author wyau
 */
public class LocalizedAssetBody {

    /**
     * bcp47 tag of the locale content
     */
    String bcp47Tag;

    /**
     * content to be localized (similar to the asset content stored in TMS) in
     * the request and localized asset in the response.
     */
    String content;

    /**
     * Optional, can be null. Allows to generate the file for a bcp47 tag that
     * is different from the repository locale (which is still used to fetch the
     * translations). This can be used to generate a file with tag "fr" even if
     * the translations are stored with fr-FR repository locale.
     *
     */
    String outputBcp47tag;

    /**
     * Optional, can be null. Allows to specify
     * a specific Okapi filter to use to process the asset
     */
    FilterConfigIdOverride filterConfigIdOverride;

    InheritanceMode inheritanceMode = InheritanceMode.USE_PARENT;

    public LocalizedAssetBody() {
    }

    public LocalizedAssetBody(String bcp47Tag, String content) {
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

    public String getOutputBcp47tag() {
        return outputBcp47tag;
    }

    public void setOutputBcp47tag(String outputBcp47tag) {
        this.outputBcp47tag = outputBcp47tag;
    }

    public FilterConfigIdOverride getFilterConfigIdOverride() {
        return filterConfigIdOverride;
    }

    public void setFilterConfigIdOverride(FilterConfigIdOverride filterConfigIdOverride) {
        this.filterConfigIdOverride = filterConfigIdOverride;
    }

    public InheritanceMode getInheritanceMode() {
        return inheritanceMode;
    }

    public void setInheritanceMode(InheritanceMode inheritanceMode) {
        this.inheritanceMode = inheritanceMode;
    }
}
