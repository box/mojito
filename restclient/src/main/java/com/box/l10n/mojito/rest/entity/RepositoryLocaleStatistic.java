package com.box.l10n.mojito.rest.entity;

/**
 *
 * @author jyi
 */
public class RepositoryLocaleStatistic {

    private Locale locale;
    private Long forTranslationCount = 0L;

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Long getForTranslationCount() {
        return forTranslationCount;
    }

    public void setForTranslationCount(Long forTranslationCount) {
        this.forTranslationCount = forTranslationCount;
    }

}
