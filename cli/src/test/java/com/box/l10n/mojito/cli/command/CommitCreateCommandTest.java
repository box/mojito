package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.commit.CommitService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author garion
 */
public class CommitCreateCommandTest extends CLITestBase {

    @Autowired
    CommitService commitService;

    @Test
    public void testCommitCreate() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        String commitHash = "ABC123";
        String authorEmail = "authorEmail";
        String authorName = "authorName";
        DateTime creationDate = DateTime.now();

        L10nJCommander l10nJCommander = getL10nJCommander();
        l10nJCommander.run("commit-create", "-r", repository.getName(),
                           Param.COMMIT_HASH_LONG, commitHash,
                           Param.AUTHOR_EMAIL_LONG, authorEmail,
                           Param.AUTHOR_NAME_LONG, authorName,
                           Param.CREATION_DATE_LONG, creationDate.toString());
        assertEquals(0, l10nJCommander.getExitCode());
        Commit createdCommit =
                commitService.getCommits(repository.getId(),
                                         Collections.singletonList(commitHash),
                                         null,
                                         null,
                                         null,
                                         null,
                                         Pageable.unpaged()).stream().findFirst().get();

        assertEquals(commitHash, createdCommit.getName());
        assertEquals(authorEmail, createdCommit.getAuthorEmail());
        assertEquals(authorName, createdCommit.getAuthorName());
        assertEquals(creationDate, createdCommit.getSourceCreationDate());
    }
}
