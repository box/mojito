package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.Commit;
import com.box.l10n.mojito.rest.entity.CommitBody;
import com.box.l10n.mojito.rest.entity.CommitToPushRunBody;
import org.springframework.stereotype.Component;

/**
 * @author garion
 */
@Component
public class CommitClient extends BaseClient {
    @Override
    public String getEntityName() {
        return "commits";
    }

    public Commit createCommit(String commitHash, Long repositoryId) {
        CommitBody commitBody = new CommitBody();
        commitBody.setCommitName(commitHash);
        commitBody.setRepositoryId(repositoryId);

        return authenticatedRestTemplate.postForObject(
                getBasePathForEntity(),
                commitBody,
                Commit.class);
    }

    public void associateCommitToPushRun(String commitHash, Long repositoryId, String pushRunName) {
        CommitToPushRunBody commitToPushRunBody = new CommitToPushRunBody();
        commitToPushRunBody.setCommitName(commitHash);
        commitToPushRunBody.setRepositoryId(repositoryId);
        commitToPushRunBody.setPushRunName(pushRunName);

        authenticatedRestTemplate.postForObject(
                getBasePathForEntity() + "/pushRun",
                commitToPushRunBody,
                Void.class);
    }
}
