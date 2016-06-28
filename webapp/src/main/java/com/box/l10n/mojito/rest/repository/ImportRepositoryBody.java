package com.box.l10n.mojito.rest.repository;

/**
 *
 * @author jaurambault
 */
public class ImportRepositoryBody {
    
    String xliffContent;
    /**
     * Indicates if the TM should be updated or if the translation can be
     * imported assuming that there is no translation yet.
     */
    boolean updateTM = false;

    public String getXliffContent() {
        return xliffContent;
    }

    public void setXliffContent(String xliffContent) {
        this.xliffContent = xliffContent;
    }

    public boolean isUpdateTM() {
        return updateTM;
    }

    public void setUpdateTM(boolean updateTM) {
        this.updateTM = updateTM;
    }
    
}
