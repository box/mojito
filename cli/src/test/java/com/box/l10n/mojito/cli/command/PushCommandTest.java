package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import org.eclipse.jgit.api.Git;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author wyau
 */
public class PushCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PushCommandTest.class);

    @Autowired
    AssetClient assetClient;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    LocaleService localeService;

    @Test
    public void testCommandName() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");

        logger.debug("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", sourceDirectory.getAbsolutePath());

        String outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));

        Matcher matcher = Pattern.compile("- Uploading:\\s*(.*?)\\s").matcher(outputString);
        matcher.find();
        String sourcePath = matcher.group(1);
        logger.debug("Source path is [{}]", sourcePath);

        Asset assetByPathAndRepositoryId = assetClient.getAssetByPathAndRepositoryId(sourcePath, repository.getId());
        assertEquals(sourcePath, assetByPathAndRepositoryId.getPath());
    }

    @Test
    public void testDeleteAsset() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        File parentSourceDirectory = getInputResourcesTestDir("delete");
        File childSourceDirectory = getInputResourcesTestDir("delete/dir");
        List<String> locales = Arrays.asList("fr-FR");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", parentSourceDirectory.getAbsolutePath());
        String outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));

        // delete/source-xliff.xliff has 5 strings
        // delete/dir/source-xliff.xliff has 5 strings
        checkNumberOfUsedUntranslatedTextUnit(repository, locales, 10);
        checkNumberOfUnusedUntranslatedTextUnit(repository, locales, 0);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", childSourceDirectory.getAbsolutePath());
        outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));

        // delete/source-xliff.xliff should be marked as deleted
        // delete/dir/source-xliff.xliff has 5 strings
        checkNumberOfUsedUntranslatedTextUnit(repository, locales, 5);
        checkNumberOfUnusedUntranslatedTextUnit(repository, locales, 5);

    }

    @Test
    public void testRenameAsset() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        File originalSourceDirectory = getInputResourcesTestDir("source");
        File renamedSourceDirectory = getInputResourcesTestDir("rename");
        List<String> locales = Arrays.asList("fr-FR");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", originalSourceDirectory.getAbsolutePath());
        String outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));

        // source/source-xliff.xliff has 5 strings
        checkNumberOfUsedUntranslatedTextUnit(repository, locales, 5);
        checkNumberOfUnusedUntranslatedTextUnit(repository, locales, 0);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", renamedSourceDirectory.getAbsolutePath());
        outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));

        // source/source-xliff.xliff should be marked as deleted
        // rename/source-xliff.xliff has 5 strings
        checkNumberOfUsedUntranslatedTextUnit(repository, locales, 5);
        checkNumberOfUnusedUntranslatedTextUnit(repository, locales, 5);

    }

    @Test
    public void testBranches() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setForRootLocale(true);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);

        logger.debug("Push one string to the master branch");

        File masterDirectory = getInputResourcesTestDir("master");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", masterDirectory.getAbsolutePath(), "-b", "master");

        List<TextUnitDTO> textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);
        assertEquals(1L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        logger.debug("Push 2 strings to branch 1. One string has same key but different english as the master branch, the other is a new string");

        File branch1 = getInputResourcesTestDir("branch1");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", branch1.getAbsolutePath(), "-b", "branch1");

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(3L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        assertEquals("from.master", textUnitDTOS.get(1).getName());
        assertEquals("master value changed branch 1", textUnitDTOS.get(1).getSource());

        assertEquals("from.branch1", textUnitDTOS.get(2).getName());
        assertEquals("value added in branch 1", textUnitDTOS.get(2).getSource());

        logger.debug("Push to branch 2 with no change compared to master");

        File branch2 = getInputResourcesTestDir("branch2");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", branch2.getAbsolutePath(), "-b", "branch2");

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(3L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        assertEquals("from.master", textUnitDTOS.get(1).getName());
        assertEquals("master value changed branch 1", textUnitDTOS.get(1).getSource());

        assertEquals("from.branch1", textUnitDTOS.get(2).getName());
        assertEquals("value added in branch 1", textUnitDTOS.get(2).getSource());

        logger.debug("Push a new string to branch 2");
        File branch2Update = getInputResourcesTestDir("branch2Update");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", branch2Update.getAbsolutePath(), "-b", "branch2");

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(4L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        assertEquals("from.master", textUnitDTOS.get(1).getName());
        assertEquals("master value changed branch 1", textUnitDTOS.get(1).getSource());

        assertEquals("from.branch1", textUnitDTOS.get(2).getName());
        assertEquals("value added in branch 1", textUnitDTOS.get(2).getSource());

        assertEquals("from.branch2", textUnitDTOS.get(3).getName());
        assertEquals("value added in branch 2", textUnitDTOS.get(3).getSource());

        logger.debug("Add new file in branch 1");

        File branch1Update = getInputResourcesTestDir("branch1Update");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", branch1Update.getAbsolutePath(), "-b", "branch1");

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(5L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        assertEquals("from.master", textUnitDTOS.get(1).getName());
        assertEquals("master value changed branch 1", textUnitDTOS.get(1).getSource());

        assertEquals("from.branch1", textUnitDTOS.get(2).getName());
        assertEquals("value added in branch 1", textUnitDTOS.get(2).getSource());

        assertEquals("from.branch2", textUnitDTOS.get(3).getName());
        assertEquals("value added in branch 2", textUnitDTOS.get(3).getSource());

        assertEquals("from.branch1.app2", textUnitDTOS.get(4).getName());
        assertEquals("value added in branch 1 in app2.properties", textUnitDTOS.get(4).getSource());
        assertEquals("app2.properties", textUnitDTOS.get(4).getAssetPath());

        logger.debug("Remove file in branch 2");

        File branch2Update2 = getInputResourcesTestDir("branch2Update2");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", branch2Update2.getAbsolutePath(), "-b", "branch2");

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(4L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        assertEquals("from.master", textUnitDTOS.get(1).getName());
        assertEquals("master value changed branch 1", textUnitDTOS.get(1).getSource());

        assertEquals("from.branch1", textUnitDTOS.get(2).getName());
        assertEquals("value added in branch 1", textUnitDTOS.get(2).getSource());

        assertEquals("from.branch1.app2", textUnitDTOS.get(3).getName());
        assertEquals("value added in branch 1 in app2.properties", textUnitDTOS.get(3).getSource());
        assertEquals("app2.properties", textUnitDTOS.get(3).getAssetPath());

        logger.debug("Remove branch 1");
        getL10nJCommander().run("branch-delete", "-r", repository.getName(), "-n", "branch1");

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(1L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        logger.debug("Re-push branch 1");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", branch1Update.getAbsolutePath(), "-b", "branch1");

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(4L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        assertEquals("from.master", textUnitDTOS.get(1).getName());
        assertEquals("master value changed branch 1", textUnitDTOS.get(1).getSource());

        assertEquals("from.branch1", textUnitDTOS.get(2).getName());
        assertEquals("value added in branch 1", textUnitDTOS.get(2).getSource());

        assertEquals("from.branch1.app2", textUnitDTOS.get(3).getName());
        assertEquals("value added in branch 1 in app2.properties", textUnitDTOS.get(3).getSource());
        assertEquals("app2.properties", textUnitDTOS.get(3).getAssetPath());
    }

    @Test
    public void testBranchesNull() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setForRootLocale(true);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);

        logger.debug("Push 1 string to the master null");

        File masterDirectory = getInputResourcesTestDir("null");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", masterDirectory.getAbsolutePath());

        List<TextUnitDTO> textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);
        assertEquals(1L, textUnitDTOS.size());
        assertEquals("from.null", textUnitDTOS.get(0).getName());
        assertEquals("value from null", textUnitDTOS.get(0).getSource());

        logger.debug("Push 1 string to master branch");

        File branch1 = getInputResourcesTestDir("master");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", branch1.getAbsolutePath(), "-b", "master");

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(2L, textUnitDTOS.size());
        assertEquals("from.null", textUnitDTOS.get(0).getName());
        assertEquals("value from null", textUnitDTOS.get(0).getSource());

        assertEquals("from.master", textUnitDTOS.get(1).getName());
        assertEquals("value from master", textUnitDTOS.get(1).getSource());

        logger.debug("Remove null branch");
        getL10nJCommander().run("branch-delete", "-r", repository.getName());

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(1L, textUnitDTOS.size());
        assertEquals("from.master", textUnitDTOS.get(0).getName());
        assertEquals("value from master", textUnitDTOS.get(0).getSource());

        logger.debug("Re push null branch");
        File branch2Update = getInputResourcesTestDir("null");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", branch2Update.getAbsolutePath());

        textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);

        assertEquals(2L, textUnitDTOS.size());
        assertEquals("from.null", textUnitDTOS.get(0).getName());
        assertEquals("value from null", textUnitDTOS.get(0).getSource());

        assertEquals("from.master", textUnitDTOS.get(1).getName());
        assertEquals("value from master", textUnitDTOS.get(1).getSource());
    }

    @Test
    public void testDifferentSourceLocale() throws Exception {

        String repoName = testIdWatcher.getEntityName("repository");

        Locale frFRLocale = localeService.findByBcp47Tag("fr-FR");

        Repository repository = repositoryService.createRepository(repoName, repoName + " description", frFRLocale, false);

        repositoryService.addRepositoryLocale(repository, "en", "fr-FR", true);
        repositoryService.addRepositoryLocale(repository, "fr-CA", "fr-FR", false);
        repositoryService.addRepositoryLocale(repository, "ja-JP");

        File sourceDirectory = getInputResourcesTestDir("source");

        logger.debug("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", sourceDirectory.getAbsolutePath());

        TextUnitSearcherParameters textUnitSearcherParametersForSource = new TextUnitSearcherParameters();
        textUnitSearcherParametersForSource.setRepositoryIds(repository.getId());
        textUnitSearcherParametersForSource.setForRootLocale(true);

        List<TextUnitDTO> sourceTextUnitDTOS = textUnitSearcher.search(textUnitSearcherParametersForSource);

        assertEquals("k1", sourceTextUnitDTOS.get(0).getName());
        assertEquals("en francais", sourceTextUnitDTOS.get(0).getSource());
        assertEquals("fr-FR", sourceTextUnitDTOS.get(0).getTargetLocale());

        TextUnitSearcherParameters textUnitSearcherParametersForTarget = new TextUnitSearcherParameters();
        textUnitSearcherParametersForTarget.setRepositoryIds(repository.getId());

        List<TextUnitDTO> targetTextUnitDTOS = textUnitSearcher.search(textUnitSearcherParametersForTarget);

        assertEquals("k1", targetTextUnitDTOS.get(0).getName());
        assertEquals("en francais", targetTextUnitDTOS.get(0).getSource());
        assertEquals("en", targetTextUnitDTOS.get(0).getTargetLocale());

        assertEquals("k1", targetTextUnitDTOS.get(1).getName());
        assertEquals("en francais", targetTextUnitDTOS.get(1).getSource());
        assertEquals("fr-CA", targetTextUnitDTOS.get(1).getTargetLocale());

        assertEquals("k1", targetTextUnitDTOS.get(2).getName());
        assertEquals("en francais", targetTextUnitDTOS.get(2).getSource());
        assertEquals("ja-JP", targetTextUnitDTOS.get(2).getTargetLocale());
    }

    @Test
    public void testInitialPushWithNofile() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        L10nJCommander l10nJCommander = getL10nJCommander();
        l10nJCommander.run("push", "-r", repository.getName(), "-s", getInputResourcesTestDir().getAbsolutePath());
        assertEquals(0, l10nJCommander.getExitCode());
    }

    @Test
    public void testProcessDiffAsset() throws Exception {

        Repository repository = createTestRepoUsingRepoService("processDiff", false);

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setForRootLocale(true);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);

        File testDir = getInputResourcesTestDir("branch1"),
                f1 = getInputResourcesTestDir("branch1/app1.properties"),
                f2 = getInputResourcesTestDir("branch1/app2.properties"),
                f3 = getInputResourcesTestDir("branch1/app3.properties");
        CharSink charSink1 = Files.asCharSink(f1, Charsets.UTF_8, FileWriteMode.APPEND),
                charSink2 = Files.asCharSink(f2, Charsets.UTF_8, FileWriteMode.APPEND),
                charSink3 = Files.asCharSink(f3, Charsets.UTF_8, FileWriteMode.APPEND);
        GitRepository gitRepository = new GitRepository();
        gitRepository.init(testDir.getAbsolutePath());
        Git git = new Git(gitRepository.jgitRepository);
        String gitPath = gitRepository.getDirectory().getPath().replace(".git", "");
        charSink1.writeLines(Arrays.asList("included.1.git_branch1=included value 1 git_branch1", "included.2.git_branch1=included value 2 git_branch1"), "\n");
        charSink2.writeLines(Arrays.asList("excluded.1.git_branch1=excluded value 1 git_branch1", "excluded.2.git_branch1=excluded value 2 git_branch1"), "\n");
        charSink3.writeLines(Arrays.asList("excluded.3.git_branch1=excluded value 3 git_branch1", "included.3.git_branch1=included value 3 git_branch1"), "\n");
        git.add().addFilepattern(testDir.getAbsolutePath().substring(gitPath.length())).call();

        logger.debug("Push two diff changed files and one file match git ignore regex filter");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", testDir.getAbsolutePath(), "-b", "git_branch1", "-gd", "true", "-gdi", "^\\+excluded");

        textUnitSearcherParameters.setBranchId(repositoryClient.getBranch(repository.getId(), "git_branch1").getId());
        List<TextUnitDTO> textUnitDTOS = getTextUnitDTOsSortedById(textUnitSearcherParameters);
        ImmutableMap pushed = ImmutableMap.of("included.1.git_branch1", "included value 1 git_branch1",
                "included.2.git_branch1", "included value 2 git_branch1",
                "excluded.3.git_branch1", "excluded value 3 git_branch1",
                "included.3.git_branch1", "included value 3 git_branch1");
        assertTrue(equalsToTextUnitDTOList(pushed, textUnitDTOS));

        logger.debug("Push two diff changed files and no regex be set");
        testDir = getInputResourcesTestDir("branch2");
        f1 = getInputResourcesTestDir("branch2/app1.properties");
        f2 = getInputResourcesTestDir("branch2/app2.properties");
        charSink1 = Files.asCharSink(f1, Charsets.UTF_8, FileWriteMode.APPEND);
        charSink2 = Files.asCharSink(f2, Charsets.UTF_8, FileWriteMode.APPEND);
        charSink1.writeLines(Arrays.asList("included.1.git_branch2=included value 1 git_branch2"), "\n");
        charSink2.writeLines(Arrays.asList("excluded.1.git_branch2=excluded value 1 git_branch2"), "\n");
        git.add().addFilepattern(testDir.getAbsolutePath().substring(gitPath.length())).call();
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", testDir.getAbsolutePath(), "-b", "git_branch2", "-gd", "true");

        textUnitSearcherParameters.setBranchId(repositoryClient.getBranch(repository.getId(), "git_branch2").getId());
        List<TextUnitDTO> textUnitDTOS1 = getTextUnitDTOsSortedById(textUnitSearcherParameters);
        ImmutableMap pushed1 = ImmutableMap.of("included.1.git_branch2", "included value 1 git_branch2",
                "excluded.1.git_branch2", "excluded value 1 git_branch2");
        assertTrue(equalsToTextUnitDTOList(pushed1, textUnitDTOS1));


        logger.debug("Push two diff changed files and no regex be set");
        testDir = getInputResourcesTestDir("branch3");
        f1 = getInputResourcesTestDir("branch3/app1.properties");
        f2 = getInputResourcesTestDir("branch3/app2.properties");
        f3 = getInputResourcesTestDir("branch3/app3.properties");
        charSink1 = Files.asCharSink(f1, Charsets.UTF_8, FileWriteMode.APPEND);
        charSink2 = Files.asCharSink(f2, Charsets.UTF_8, FileWriteMode.APPEND);
        charSink3 = Files.asCharSink(f3, Charsets.UTF_8, FileWriteMode.APPEND);
        charSink1.writeLines(Arrays.asList("included.1.git_branch3=included value 1 git_branch3", "included.2.git_branch3=included value 2 git_branch3"), "\n");
        charSink2.writeLines(Arrays.asList("excluded.1.git_branch3=excluded value 1 git_branch3", "excluded.2.git_branch3=excluded value 2 git_branch3"), "\n");
        charSink3.writeLines(Arrays.asList("excluded.3.git_branch3=excluded value 3 git_branch3", "included.3.git_branch3=included value 3 git_branch3"), "\n");
        git.add().addFilepattern(testDir.getAbsolutePath().substring(gitPath.length())).call();
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", testDir.getAbsolutePath(), "-b", "git_branch3", "-gd", "true", "-gdi", "^\\+excluded|\\+included\\.3");

        textUnitSearcherParameters.setBranchId(repositoryClient.getBranch(repository.getId(), "git_branch3").getId());
        List<TextUnitDTO> textUnitDTOS2 = getTextUnitDTOsSortedById(textUnitSearcherParameters);
        ImmutableMap pushed2 = ImmutableMap.of("included.1.git_branch3", "included value 1 git_branch3",
                "included.2.git_branch3", "included value 2 git_branch3");
        assertTrue(equalsToTextUnitDTOList(pushed2, textUnitDTOS2));

        git.reset().call();
        git.checkout().setAllPaths(true).call();

        logger.debug("Git diff not enable, extract all source file");
        testDir = getInputResourcesTestDir("branch4");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", testDir.getAbsolutePath(), "-b", "git_branch4");

        textUnitSearcherParameters.setBranchId(repositoryClient.getBranch(repository.getId(), "git_branch4").getId());
        List<TextUnitDTO> textUnitDTOS3 = getTextUnitDTOsSortedById(textUnitSearcherParameters);
        ImmutableMap pushed3 = ImmutableMap.of("git_branch4.committed.k1", "git_branch4 committed v1",
                "git_branch4.committed.k2", "git_branch4 committed v2");
        assertTrue(equalsToTextUnitDTOList(pushed3, textUnitDTOS3));

        logger.debug("No git diff change and no branch will be created");
        testDir = getInputResourcesTestDir("branch5");
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", testDir.getAbsolutePath(), "-gd", "true", "-b", "git_branch5");
        assertNull(repositoryClient.getBranch(repository.getId(), "git_branch5"));
    }

    private void checkNumberOfUsedUntranslatedTextUnit(Repository repository, List<String> locales, int expectedNumberOfUnstranslated) {
        checkNumberOfUntranslatedTextUnit(repository, locales, true, expectedNumberOfUnstranslated);
    }

    private void checkNumberOfUnusedUntranslatedTextUnit(Repository repository, List<String> locales, int expectedNumberOfUnstranslated) {
        checkNumberOfUntranslatedTextUnit(repository, locales, false, expectedNumberOfUnstranslated);
    }

    private void checkNumberOfUntranslatedTextUnit(Repository repository, List<String> locales, boolean used, int expectedNumberOfUnstranslated) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.UNTRANSLATED);
        textUnitSearcherParameters.setLocaleTags(locales);

        if (used) {
            textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        } else {
            textUnitSearcherParameters.setUsedFilter(UsedFilter.UNUSED);
        }

        List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
        assertEquals(expectedNumberOfUnstranslated, search.size());
        for (TextUnitDTO textUnitDTO : search) {
            assertEquals(used, textUnitDTO.isUsed());
        }
    }

    private List<TextUnitDTO> getTextUnitDTOsSortedById(TextUnitSearcherParameters textUnitSearcherParameters){
        List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(textUnitSearcherParameters);

        Comparator<TextUnitDTO> textUnitDTOComparator = Comparator.comparingLong(TextUnitDTO::getTmTextUnitId);
        Collections.sort(textUnitDTOS, textUnitDTOComparator);

        for (TextUnitDTO textUnitDTO : textUnitDTOS) {
            logger.debug("name: {}, source: {}", textUnitDTO.getName(), textUnitDTO.getSource());
        }

        return textUnitDTOS;
    }

    private boolean equalsToTextUnitDTOList(ImmutableMap<String, String> pushed,  List<TextUnitDTO> textUnitDTOS) {
        HashMap<String, String> textUnitMap = new HashMap();
        textUnitDTOS.stream().forEach(textUnitDTO -> textUnitMap.put(textUnitDTO.getName(), textUnitDTO.getSource()));
        boolean result = true;
        for (Map.Entry entry: pushed.entrySet()) {
            if(textUnitMap.containsKey(entry.getKey()) && textUnitMap.get(entry.getKey()).equals(entry.getValue())) {
                textUnitMap.remove(entry.getKey());
            } else {
                result = false;
                break;
            }
        }
        return result && textUnitMap.isEmpty();
    }

}