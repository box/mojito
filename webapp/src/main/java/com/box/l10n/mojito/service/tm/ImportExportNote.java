package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import org.joda.time.DateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to serialize/deserialize the note element used for import/export.
 *
 * <p>
 * The note element is used to store different information and is used instead
 * of some XLIFF attributes because Okapi doesn't provide access to all of them.
 *
 * @author jaurambault
 */
public class ImportExportNote {

    String sourceComment;
    String targetComment;
    boolean reviewNeeded = false;
    boolean includedInLocalizedFile = true;
    TMTextUnitVariant.Status status = TMTextUnitVariant.Status.APPROVED;
    List<TMTextUnitVariantComment> variantComments = new ArrayList<>();
    DateTime createdDate;

    public String getSourceComment() {
        return sourceComment;
    }

    public void setSourceComment(String sourceComment) {
        this.sourceComment = sourceComment;
    }

    public String getTargetComment() {
        return targetComment;
    }

    public void setTargetComment(String targetComment) {
        this.targetComment = targetComment;
    }

    public TMTextUnitVariant.Status getStatus() {
        return status;
    }

    public void setStatus(TMTextUnitVariant.Status status) {
        this.status = status;
    }

    public boolean isIncludedInLocalizedFile() {
        return includedInLocalizedFile;
    }

    public void setIncludedInLocalizedFile(boolean includedInLocalizedFile) {
        this.includedInLocalizedFile = includedInLocalizedFile;
    }

    public List<TMTextUnitVariantComment> getVariantComments() {
        return variantComments;
    }

    public void setVariantComments(List<TMTextUnitVariantComment> variantComments) {
        this.variantComments = variantComments;
    }

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(DateTime createdDate) {
        this.createdDate = createdDate;
    }
}
