package com.box.l10n.mojito.rest.commit;

import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author garion
 */
public class CommitToPushRunBody {

    /**
     * {@link Repository#id}
     */
    Long repositoryId;

    /**
     * The name of the commit.
     */
    String commitName;

    /**
     * The name of the PushRun, see: {@link PushRun#name}.
     */
    String pushRunName;

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getCommitName() {
        return commitName;
    }

    public void setCommitName(String commitName) {
        this.commitName = commitName;
    }

    public String getPushRunName() {
        return pushRunName;
    }

    public void setPushRunName(String pushRunName) {
        this.pushRunName = pushRunName;
    }
}
