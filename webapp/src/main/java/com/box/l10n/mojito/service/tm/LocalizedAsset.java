package com.box.l10n.mojito.service.tm;

/**
 * @author wyau
 */
public class LocalizedAsset {

    String bcp47Tag;
    String content;

    public LocalizedAsset() {
    }

    public LocalizedAsset(String bcp47Tag, String content) {
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
}
