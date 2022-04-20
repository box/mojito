package com.box.l10n.mojito.rest.machinetranslation;

import java.util.List;

/**
 * DTO to translate one source string for one target language.
 *
 * @author garion
 */
public class TranslationRequestDTO {
    String textSource;
    String sourceBcp47Tag;
    String targetBcp47Tag;
    boolean skipFunctionalProtection;
    boolean skipLeveraging;
    List<Long> repositoryIds;
    List<String> repositoryNames;

    public String getTextSource() {
        return textSource;
    }

    public void setTextSource(String textSource) {
        this.textSource = textSource;
    }

    public String getSourceBcp47Tag() {
        return sourceBcp47Tag;
    }

    public void setSourceBcp47Tag(String sourceBcp47Tag) {
        this.sourceBcp47Tag = sourceBcp47Tag;
    }

    public String getTargetBcp47Tag() {
        return targetBcp47Tag;
    }

    public void setTargetBcp47Tag(String targetBcp47Tag) {
        this.targetBcp47Tag = targetBcp47Tag;
    }

    public boolean isSkipFunctionalProtection() {
        return skipFunctionalProtection;
    }

    public void setSkipFunctionalProtection(boolean skipFunctionalProtection) {
        this.skipFunctionalProtection = skipFunctionalProtection;
    }

    public boolean isSkipLeveraging() {
        return skipLeveraging;
    }

    public void setSkipLeveraging(boolean skipLeveraging) {
        this.skipLeveraging = skipLeveraging;
    }

    public List<Long> getRepositoryIds() {
        return repositoryIds;
    }

    public void setRepositoryIds(List<Long> repositoryIds) {
        this.repositoryIds = repositoryIds;
    }

    public List<String> getRepositoryNames() {
        return repositoryNames;
    }

    public void setRepositoryNames(List<String> repositoryNames) {
        this.repositoryNames = repositoryNames;
    }
}
