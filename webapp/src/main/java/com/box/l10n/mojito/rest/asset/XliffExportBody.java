package com.box.l10n.mojito.rest.asset;

/**
 *
 * @author jaurambault
 */
public class XliffExportBody {

    String content;

    public XliffExportBody() {
    }

    public XliffExportBody(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
