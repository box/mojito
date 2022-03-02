package com.box.l10n.mojito.smartling.response;

import java.util.Date;

public class GlossaryTermTranslation {

    Date createdDate;
    String localeId;
    boolean lockTranslation;
    Date modifiedDate;
    String notes;
    boolean submittedForTranslation;
    String translatedTerm;
    String translatorUserUid;

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getLocaleId() {
        return localeId;
    }

    public void setLocaleId(String localeId) {
        this.localeId = localeId;
    }

    public boolean isLockTranslation() {
        return lockTranslation;
    }

    public void setLockTranslation(boolean lockTranslation) {
        this.lockTranslation = lockTranslation;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isSubmittedForTranslation() {
        return submittedForTranslation;
    }

    public void setSubmittedForTranslation(boolean submittedForTranslation) {
        this.submittedForTranslation = submittedForTranslation;
    }

    public String getTranslatedTerm() {
        return translatedTerm;
    }

    public void setTranslatedTerm(String translatedTerm) {
        this.translatedTerm = translatedTerm;
    }

    public String getTranslatorUserUid() {
        return translatorUserUid;
    }

    public void setTranslatorUserUid(String translatorUserUid) {
        this.translatorUserUid = translatorUserUid;
    }
}
