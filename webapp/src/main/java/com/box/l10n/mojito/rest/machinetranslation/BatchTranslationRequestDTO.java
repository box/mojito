package com.box.l10n.mojito.rest.machinetranslation;

import java.util.List;

/**
 * DTO to batch translate a set of source strings against a set of target languages.
 *
 * @author garion
 */
public class BatchTranslationRequestDTO {
    List<String> textSources;
    String sourceBcp47Tag;
    List<String> targetBcp47Tags;
    boolean skipFunctionalProtection;
    boolean skipLeveraging;
    List<Long> repositoryIds;
    List<String> repositoryNames;

    public List<String> getTextSources() {
        return textSources;
    }

    public void setTextSources(List<String> textSources) {
        this.textSources = textSources;
    }

    public String getSourceBcp47Tag() {
        return sourceBcp47Tag;
    }

    public void setSourceBcp47Tag(String sourceBcp47Tag) {
        this.sourceBcp47Tag = sourceBcp47Tag;
    }

    public List<String> getTargetBcp47Tags() {
        return targetBcp47Tags;
    }

    public void setTargetBcp47Tags(List<String> targetBcp47Tags) {
        this.targetBcp47Tags = targetBcp47Tags;
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
