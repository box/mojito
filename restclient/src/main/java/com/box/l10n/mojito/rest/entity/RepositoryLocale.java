package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.Comparator;

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

    public static Comparator<RepositoryLocale> getComparator() {
        return new Comparator<RepositoryLocale>() {
            @Override
            public int compare(RepositoryLocale repositoryLocale1, RepositoryLocale repositoryLocale2) {
                String bcp47Tag1 = repositoryLocale1.getLocale().getBcp47Tag();
                String bcp47Tag2 = repositoryLocale2.getLocale().getBcp47Tag();
                return bcp47Tag1.compareTo(bcp47Tag2);
            }
        };
    }
}
