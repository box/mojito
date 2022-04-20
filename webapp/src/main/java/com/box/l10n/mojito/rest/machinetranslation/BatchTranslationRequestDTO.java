package com.box.l10n.mojito.rest.machinetranslation;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO to batch translate a set of source strings against a set of target languages.
 *
 * @author garion
 */
public class BatchTranslationRequestDTO {
    List<String> textSources;
    String sourceBcp47Tag;
    ArrayList<String> targetBcp47Tags;
    boolean skipFunctionalProtection;
    boolean skipLeveraging;
    ArrayList<Long> repositoryIds;
    ArrayList<String> repositoryNames;

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

    public ArrayList<String> getTargetBcp47Tags() {
        return targetBcp47Tags;
    }

    public void setTargetBcp47Tags(ArrayList<String> targetBcp47Tags) {
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

    public ArrayList<Long> getRepositoryIds() {
        return repositoryIds;
    }

    public void setRepositoryIds(ArrayList<Long> repositoryIds) {
        this.repositoryIds = repositoryIds;
    }

    public ArrayList<String> getRepositoryNames() {
        return repositoryNames;
    }

    public void setRepositoryNames(ArrayList<String> repositoryNames) {
        this.repositoryNames = repositoryNames;
    }
}
