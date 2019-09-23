package com.box.l10n.mojito.service.tm.search;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import org.joda.time.DateTime;

/**
 * DTO to build text unit histories from TM entity variants
 *
 * @author ehoogerbeets
 */
public class TextUnitHistoryDTO {

    private Long tmTextUnitId;
    private Long tmTextUnitVariantId;
    private Long localeId;
    private String userName;
    private String target;
    private String targetLocale;
    private TMTextUnitVariant.Status status;
    private DateTime date;

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public void setTmTextUnitId(Long tmTextUnitId) {
        this.tmTextUnitId = tmTextUnitId;
    }

    public Long getTmTextUnitVariantId() {
        return tmTextUnitVariantId;
    }

    public void setTmTextUnitVariantId(Long tmTextUnitVariantId) {
        this.tmTextUnitVariantId = tmTextUnitVariantId;
    }

    public Long getLocaleId() {
        return localeId;
    }

    public void setLocaleId(Long localeId) {
        this.localeId = localeId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        this.userName = name;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTargetLocale() {
        return targetLocale;
    }

    public void setTargetLocale(String targetLocale) {
        this.targetLocale = targetLocale;
    }

    public boolean isTranslated() {
        return tmTextUnitVariantId != null;
    }

    public TMTextUnitVariant.Status getStatus() {
        return status;
    }

    public void setStatus(TMTextUnitVariant.Status status) {
        this.status = status;
    }

    public DateTime getDate() {
        return date;
    }

    public void setCreatedDate(DateTime createdDate) {
        this.date = createdDate;
    }
}
