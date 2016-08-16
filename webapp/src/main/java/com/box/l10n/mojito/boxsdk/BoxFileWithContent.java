package com.box.l10n.mojito.boxsdk;

import com.box.sdk.BoxFile;


/**
 * @author jaurambault
 */
public class BoxFileWithContent {

    BoxFile boxFile;
    String content;

    public BoxFile getBoxFile() {
        return boxFile;
    }

    public void setBoxFile(BoxFile boxFile) {
        this.boxFile = boxFile;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
