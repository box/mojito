package com.box.l10n.mojito.smartling.response;

import java.util.Date;

public class GlossaryDetails {

    String createdByUserId;
    Date createdDate;
    String description;
    String glossaryUid;
    String name;
    String sourceLocaleId;

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGlossaryUid() {
        return glossaryUid;
    }

    public void setGlossaryUid(String glossaryUid) {
        this.glossaryUid = glossaryUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceLocaleId() {
        return sourceLocaleId;
    }

    public void setSourceLocaleId(String sourceLocaleId) {
        this.sourceLocaleId = sourceLocaleId;
    }
}
