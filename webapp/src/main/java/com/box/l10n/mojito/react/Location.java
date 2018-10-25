package com.box.l10n.mojito.react;

public class Location {

    String url;
    String label;
    String extractorPrefix;
    Boolean useUsage;

    public Boolean getUseUsage() {
        return useUsage;
    }

    public void setUseUsage(Boolean useUsage) {
        this.useUsage = useUsage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public String getExtractorPrefix() {
        return extractorPrefix;
    }

    public void setExtractorPrefix(String extractorPrefix) {
        this.extractorPrefix = extractorPrefix;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
