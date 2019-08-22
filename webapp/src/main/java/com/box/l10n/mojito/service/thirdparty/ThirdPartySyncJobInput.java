package com.box.l10n.mojito.service.thirdparty;

/**
 * @author jaurambault
 */
public class ThirdPartySyncJobInput {

    Long repositoryId;

    String thirdPartyProjectId;

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
}
