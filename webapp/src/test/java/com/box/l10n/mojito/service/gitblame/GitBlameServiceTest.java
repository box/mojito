package com.box.l10n.mojito.service.gitblame;

import com.box.l10n.mojito.entity.GitBlame;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class GitBlameServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameServiceTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    GitBlameService gitBlameService;

    @Test
    public void getGitBlameWithUsages() throws Exception {
        TMTestData tmTestData = new TMTestData(testIdWatcher);

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
        textUnitSearcherParameters.setForRootLocale(true);
        textUnitSearcherParameters.setPluralFormsFiltered(false);

        List<GitBlameWithUsage> gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);

        assertEquals(3, gitBlameWithUsages.size());

        logger.debug("Check none of the entry have git information");
        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            logger.info(gitBlameWithUsage.getTextUnitName());
            Assert.assertNull(gitBlameWithUsage.getGitBlame());
        }

        logger.debug("Save without info");
        gitBlameService.saveGitBlameWithUsages(gitBlameWithUsages).get();

        logger.debug("Save GitBlame");

        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            GitBlame gitBlame = new GitBlame();
            gitBlame.setAuthorName(gitBlameWithUsage.getTextUnitName() + "-author-name");
            gitBlame.setAuthorEmail(gitBlameWithUsage.getTextUnitName() + "-author-email");
            gitBlame.setCommitName(gitBlameWithUsage.getTextUnitName() + "-commit-name");
            gitBlame.setCommitTime(gitBlameWithUsage.getTextUnitName() + "-commit-time");
            gitBlameWithUsage.setGitBlame(gitBlame);
        }

        gitBlameService.saveGitBlameWithUsages(gitBlameWithUsages).get();
        List<GitBlameWithUsage> gitBlameWithUsagesAfterSave = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);

        assertEquals(3, gitBlameWithUsages.size());

        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            GitBlame gitBlame = gitBlameWithUsage.getGitBlame();
            assertEquals(gitBlameWithUsage.getTextUnitName() + "-author-name", gitBlame.getAuthorName());
            assertEquals(gitBlameWithUsage.getTextUnitName() + "-author-email", gitBlame.getAuthorEmail());
            assertEquals(gitBlameWithUsage.getTextUnitName() + "-commit-name", gitBlame.getCommitName());
            assertEquals(gitBlameWithUsage.getTextUnitName() + "-commit-time", gitBlame.getCommitTime());
        }

        logger.info("Update GitBlame");

        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            GitBlame gitBlame = new GitBlame();
            gitBlame.setAuthorName(gitBlameWithUsage.getTextUnitName() + "-author-name-up");
            gitBlame.setAuthorEmail(gitBlameWithUsage.getTextUnitName() + "-author-email-up");
            gitBlame.setCommitName(gitBlameWithUsage.getTextUnitName() + "-commit-name-up");
            gitBlame.setCommitTime(gitBlameWithUsage.getTextUnitName() + "-commit-time-up");
            gitBlameWithUsage.setGitBlame(gitBlame);
        }

        gitBlameService.saveGitBlameWithUsages(gitBlameWithUsages).get();

        List<GitBlameWithUsage> gitBlameWithUsagesAfterUpdate = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);

        assertEquals(3, gitBlameWithUsagesAfterUpdate.size());

        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            GitBlame gitBlame = gitBlameWithUsage.getGitBlame();
            assertEquals(gitBlameWithUsage.getTextUnitName() + "-author-name-up", gitBlame.getAuthorName());
            assertEquals(gitBlameWithUsage.getTextUnitName() + "-author-email-up", gitBlame.getAuthorEmail());
            assertEquals(gitBlameWithUsage.getTextUnitName() + "-commit-name-up", gitBlame.getCommitName());
            assertEquals(gitBlameWithUsage.getTextUnitName() + "-commit-time-up", gitBlame.getCommitTime());
        }
    }

    @Test
    public void testgetGitBlameWithUsagesByTmTextUnitIdRemovesDuplicates() {

        List<GitBlameWithUsage> gitBlameWithUsages = new ArrayList<>();

        GitBlameWithUsage gitBlameWithUsage = new GitBlameWithUsage();
        gitBlameWithUsage.setTmTextUnitId(1L);
        gitBlameWithUsages.add(gitBlameWithUsage);

        gitBlameWithUsage = new GitBlameWithUsage();
        gitBlameWithUsage.setTmTextUnitId(2L);
        gitBlameWithUsages.add(gitBlameWithUsage);

        gitBlameWithUsage = new GitBlameWithUsage();
        gitBlameWithUsage.setTmTextUnitId(1L);
        gitBlameWithUsages.add(gitBlameWithUsage);

        gitBlameWithUsage = new GitBlameWithUsage();
        gitBlameWithUsage.setTmTextUnitId(3L);
        gitBlameWithUsages.add(gitBlameWithUsage);

        Set<Long> gitBlameWithUsagesByTmTextUnitId = gitBlameService.getGitBlameWithUsagesByTmTextUnitId(gitBlameWithUsages).keySet();
        assertEquals(Sets.newHashSet(1L, 2L, 3L), gitBlameWithUsagesByTmTextUnitId);
    }

}