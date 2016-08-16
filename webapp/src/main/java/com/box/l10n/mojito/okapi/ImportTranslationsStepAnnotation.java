package com.box.l10n.mojito.okapi;

import net.sf.okapi.common.annotation.IAnnotation;

/**
 * Contains information about the import process implemented in 
 * {@link ImportTranslationsStep}.
 * 
 * @author jaurambault
 */
public class ImportTranslationsStepAnnotation implements IAnnotation {

    String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    
}
