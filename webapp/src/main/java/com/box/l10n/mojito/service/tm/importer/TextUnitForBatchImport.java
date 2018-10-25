package com.box.l10n.mojito.service.tm.importer;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;

/**
 *
 * @author jeanaurambault
 */
public class TextUnitForBatchImport {

    Repository repository;
    Asset asset;
    Locale locale;
    String content;
    String name;
    String comment;
    Long tmTextUnitId;
    TextUnitDTO currentTextUnit;
    boolean includedInLocalizedFile;
    private TMTextUnitVariant.Status status;

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public void setTmTextUnitId(Long tmTextUnitId) {
        this.tmTextUnitId = tmTextUnitId;
    }

    public TextUnitDTO getCurrentTextUnit() {
        return currentTextUnit;
    }

    public void setCurrentTextUnit(TextUnitDTO currentTextUnit) {
        this.currentTextUnit = currentTextUnit;
    }

    public boolean isIncludedInLocalizedFile() {
        return includedInLocalizedFile;
    }

    public void setIncludedInLocalizedFile(boolean includedInLocalizedFile) {
        this.includedInLocalizedFile = includedInLocalizedFile;
    }

    public TMTextUnitVariant.Status getStatus() {
        return status;
    }

    public void setStatus(TMTextUnitVariant.Status status) {
        this.status = status;
    }
}
