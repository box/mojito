package com.box.l10n.mojito.service.drop.importer;

import com.box.l10n.mojito.service.NormalizationUtils;

/**
 * Contains information about file from a drop that can be imported.
 *
 * @author jaurambault
 */
public class DropFile {

    String id;
    String content;
    String bcp47Tag;
    String name;
    String extension;

    public DropFile(String id, String bcp47Tag, String name, String extension) {
        this.id = id;
        this.bcp47Tag = bcp47Tag;
        this.name = name;
        this.extension = extension;
    }

    public String getId() {
        return id;
    }

    public String getBcp47Tag() {
        return bcp47Tag;
    }

    public String getExtension() {
        return extension;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = NormalizationUtils.normalize(content);
    }

}
