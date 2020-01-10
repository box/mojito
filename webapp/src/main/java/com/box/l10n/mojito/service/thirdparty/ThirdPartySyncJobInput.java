package com.box.l10n.mojito.service.thirdparty;

import java.util.List;

/**
 * @author jaurambault
 */
public class ThirdPartySyncJobInput {

    Long repositoryId;
    String thirdPartyProjectId;
    List<ThirdPartyService.Action> actions;
    String pluralSeparator;
    String localeMapping;
    List<String> options;

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getThirdPartyProjectId() {
        return thirdPartyProjectId;
    }

    public void setThirdPartyProjectId(String thirdPartyProjectId) {
        this.thirdPartyProjectId = thirdPartyProjectId;
    }

    public List<ThirdPartyService.Action> getActions() {
        return actions;
    }

    public void setActions(List<ThirdPartyService.Action> actions) {
        this.actions = actions;
    }

    public String getPluralSeparator() {
        return pluralSeparator;
    }

    public void setPluralSeparator(String pluralSeparator) {
        this.pluralSeparator = pluralSeparator;
    }

    public String getLocaleMapping() {
        return localeMapping;
    }

    public void setLocaleMapping(String localeMapping) {
        this.localeMapping = localeMapping;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
