package com.box.l10n.mojito.service.branch.notification;

import java.util.List;

/**
 * Some information for notification are expansive to compute, so we store them in the object to share them between
 * the different notification methods.
 */
public class BranchNotificationInfo {

    String contentMd5;

    List<String> sourceStrings;

    public String getContentMd5() {
        return contentMd5;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public List<String> getSourceStrings() {
        return sourceStrings;
    }

    public void setSourceStrings(List<String> sourceStrings) {
        this.sourceStrings = sourceStrings;
    }
}
