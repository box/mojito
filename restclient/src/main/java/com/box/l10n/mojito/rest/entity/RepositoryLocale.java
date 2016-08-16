package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 *
 * @author wyau
 */
public class RepositoryLocale {

    private Long id;

    private Locale locale;

    @JsonBackReference
    private Repository repository;

    private boolean toBeFullyTranslated = true;

    private RepositoryLocale parentLocale;

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public boolean isToBeFullyTranslated() {
        return toBeFullyTranslated;
    }

    public void setToBeFullyTranslated(boolean toBeFullyTranslated) {
        this.toBeFullyTranslated = toBeFullyTranslated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public RepositoryLocale getParentLocale() {
        return parentLocale;
    }

    public void setParentLocale(RepositoryLocale parentLocale) {
        this.parentLocale = parentLocale;
    }
}
