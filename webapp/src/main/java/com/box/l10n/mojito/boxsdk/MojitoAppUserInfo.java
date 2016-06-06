package com.box.l10n.mojito.boxsdk;

import javax.persistence.Column;

/**
 * @author wyau
 */
public class MojitoAppUserInfo {

    String rootFolderId;

    String dropsFolderId;

    public String getRootFolderId() {
        return rootFolderId;
    }

    public void setRootFolderId(String rootFolderId) {
        this.rootFolderId = rootFolderId;
    }

    public String getDropsFolderId() {
        return dropsFolderId;
    }

    public void setDropsFolderId(String dropsFolderId) {
        this.dropsFolderId = dropsFolderId;
    }
}
