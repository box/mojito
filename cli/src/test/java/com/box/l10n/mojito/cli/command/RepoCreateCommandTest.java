package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RepoCreateCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepoCreateCommandTest.class);

    protected static final String COMMAND_ERROR_MESSAGE = "There is a conflict";

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    RepositoryService repositoryService;

    @Test
    public void testCreateTestRepo() throws Exception {
        String testRepoName = testIdWatcher.getEntityName("repository");
        String testDescription = testRepoName + " description";

        createTestRepoWith6Locales(testRepoName, testDescription, false);

        assertCreatedRepositoryHas6Locales(testRepoName, testDescription, false);
    }

    protected void createTestRepoWith6Locales(String testRepoName, String testDescription, Boolean hasIntegrityChecker) throws Exception {
        logger.debug("Creating repo with name: {}", testRepoName);

        List<String> args = new ArrayList<>();
        args.add("repo-create");
        args.add(Param.REPOSITORY_NAME_SHORT);
        args.add(testRepoName);
        args.add(Param.REPOSITORY_DESCRIPTION_SHORT);
        args.add(testDescription);

        //TODO need to ensure that if "en" (ie. the default locale) is in this request parameter, the server won't add it in
        args.add(Param.REPOSITORY_LOCALES_SHORT);
        args.add("fr-FR");
        args.add("(fr-CA)->fr-FR");
        args.add("en-GB");
        args.add("(en-CA)->en-GB");
        args.add("en-AU->en-GB");

        if (hasIntegrityChecker) {
            args.add(RepoCreateCommand.INTEGRITY_CHECK_SHORT_PARAM);
            args.add("properties:MESSAGE_FORMAT,properties:TRAILING_WHITESPACE");
        }

        getL10nJCommander().run(args.toArray(new String[args.size()]));
    }

    protected void assertCreatedRepositoryHas6Locales(String testRepoName, String testDescription, Boolean expectIntegrityChecker) {
        assertTrue(outputCapture.toString().contains("--> repository id: "));

        Repository repository = repositoryRepository.findByName(testRepoName);

        assertEquals(testDescription, repository.getDescription());
        assertEquals(6, repository.getRepositoryLocales().size());

        Map<String, RepositoryLocale> repoLocaleMapByBcp47Tag = getRepoLocaleMapByBcp47Tag(repository);

        assertNotNull(repoLocaleMapByBcp47Tag.get("fr-FR"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("fr-CA"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("en-GB"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("en-CA"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("en-AU"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("en"));

        assertEquals("fr-FR", repoLocaleMapByBcp47Tag.get("fr-CA").getParentLocale().getLocale().getBcp47Tag());
        assertEquals("en-GB", repoLocaleMapByBcp47Tag.get("en-CA").getParentLocale().getLocale().getBcp47Tag());
        assertEquals("en-GB", repoLocaleMapByBcp47Tag.get("en-AU").getParentLocale().getLocale().getBcp47Tag());

        assertEquals(true, repoLocaleMapByBcp47Tag.get("fr-FR").isToBeFullyTranslated());
        assertEquals(false, repoLocaleMapByBcp47Tag.get("fr-CA").isToBeFullyTranslated());
        assertEquals(true, repoLocaleMapByBcp47Tag.get("en-GB").isToBeFullyTranslated());
        assertEquals(false, repoLocaleMapByBcp47Tag.get("en-CA").isToBeFullyTranslated());
        assertEquals(true, repoLocaleMapByBcp47Tag.get("en-AU").isToBeFullyTranslated());

        if (expectIntegrityChecker) {
            logger.debug("Asserting 1 integrity checker");
            assertEquals(2, repository.getAssetIntegrityCheckers().size());

            for (AssetIntegrityChecker assetIntegrityChecker : repository.getAssetIntegrityCheckers()) {
                assertEquals("properties", assetIntegrityChecker.getAssetExtension());
                assertTrue(IntegrityCheckerType.MESSAGE_FORMAT == assetIntegrityChecker.getIntegrityCheckerType()
                        || IntegrityCheckerType.TRAILING_WHITESPACE == assetIntegrityChecker.getIntegrityCheckerType());
            }

        }
    }

    @Test
    public void testCreateTestRepoWithIntegrityCheck() throws Exception {
        String testRepoName = testIdWatcher.getEntityName("repository");
        String testDescription = testRepoName + " description";

        createTestRepoWith6Locales(testRepoName, testDescription, true);

        assertCreatedRepositoryHas6Locales(testRepoName, testDescription, true);
    }

    @Test
    public void testCreateTestRepoWithWebAppLocalesCorrectlyCreated() throws Exception {
        String testRepoName = testIdWatcher.getEntityName("repository");
        String testDescription = testRepoName + " description";

        logger.debug("Creating repo with name: {}", testRepoName);

        getL10nJCommander().run(
                "repo-create",
                Param.REPOSITORY_NAME_SHORT, testRepoName,
                Param.REPOSITORY_DESCRIPTION_SHORT, testDescription,
                Param.REPOSITORY_LOCALES_SHORT, "da-DK", "de-DE", "en-GB", "(en-CA)->en-GB", "(en-AU)->en-GB", "es-ES",
                "fi-FI", "fr-FR", "(fr-CA)->fr-FR", "it-IT", "ja-JP", "ko-KR",
                "nb-NO", "nl-NL", "pl-PL", "pt-BR", "ru-RU", "sv-SE",
                "tr-TR", "zh-CN", "zh-TW",
                RepoCreateCommand.INTEGRITY_CHECK_SHORT_PARAM,
                "xliff:PRINTF_LIKE"
        );

        assertTrue(outputCapture.toString().contains("--> repository id: "));

        Repository repository = repositoryRepository.findByName(testRepoName);

        assertEquals(testDescription, repository.getDescription());
        assertEquals("There are 22 repositoryLocale including the root locale", 22, repository.getRepositoryLocales().size());

        Map<String, RepositoryLocale> repoLocaleMapByBcp47Tag = getRepoLocaleMapByBcp47Tag(repository);

        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("da-DK"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("de-DE"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("en-GB"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("en-CA"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("en-AU"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("es-ES"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("fi-FI"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("fr-FR"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("fr-CA"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("it-IT"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("ja-JP"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("ko-KR"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("nb-NO"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("nl-NL"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("pl-PL"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("pt-BR"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("ru-RU"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("sv-SE"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("tr-TR"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("zh-CN"));
        assertNotNull("This locale should exist", repoLocaleMapByBcp47Tag.get("zh-TW"));

        RepositoryLocale en = repoLocaleMapByBcp47Tag.get("en");
        assertNotNull("en is the root locale", en);
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("da-DK").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("de-DE").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("en-GB").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("es-ES").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("fi-FI").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("fr-FR").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("it-IT").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("ja-JP").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("ko-KR").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("nb-NO").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("nl-NL").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("pl-PL").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("pt-BR").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("ru-RU").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("sv-SE").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("tr-TR").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("zh-CN").getParentLocale());
        assertEquals("en should be the root locale", en, repoLocaleMapByBcp47Tag.get("zh-TW").getParentLocale());

        assertEquals("fr-FR should be the parent locale", "fr-FR", repoLocaleMapByBcp47Tag.get("fr-CA").getParentLocale().getLocale().getBcp47Tag());
        assertEquals("en-GB should be the parent locale", "en-GB", repoLocaleMapByBcp47Tag.get("en-CA").getParentLocale().getLocale().getBcp47Tag());
        assertEquals("en-GB should be the parent locale", "en-GB", repoLocaleMapByBcp47Tag.get("en-AU").getParentLocale().getLocale().getBcp47Tag());

        assertFalse("This should be not fully translated", repoLocaleMapByBcp47Tag.get("fr-CA").isToBeFullyTranslated());
        assertFalse("This should be not fully translated", repoLocaleMapByBcp47Tag.get("en-CA").isToBeFullyTranslated());
        assertFalse("This should be not fully translated", repoLocaleMapByBcp47Tag.get("en-AU").isToBeFullyTranslated());

        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("da-DK").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("de-DE").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("en-GB").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("es-ES").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("fi-FI").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("fr-FR").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("it-IT").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("ja-JP").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("ko-KR").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("nb-NO").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("nl-NL").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("pl-PL").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("pt-BR").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("ru-RU").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("sv-SE").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("tr-TR").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("zh-CN").isToBeFullyTranslated());
        assertTrue("This should be fully translated", repoLocaleMapByBcp47Tag.get("zh-TW").isToBeFullyTranslated());

        Set<AssetIntegrityChecker> assetIntegrityCheckers = repository.getAssetIntegrityCheckers();
        assertEquals(1, assetIntegrityCheckers.size());
        AssetIntegrityChecker assetIntegrityChecker = assetIntegrityCheckers.iterator().next();
        assertEquals("xliff", assetIntegrityChecker.getAssetExtension());
        assertEquals(IntegrityCheckerType.PRINTF_LIKE, assetIntegrityChecker.getIntegrityCheckerType());
    }

    protected Map<String, RepositoryLocale> getRepoLocaleMapByBcp47Tag(Repository repository) {
        Map<String, RepositoryLocale> map = new HashMap<>(repository.getRepositoryLocales().size());

        for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
            map.put(repositoryLocale.getLocale().getBcp47Tag(), repositoryLocale);
        }

        return map;
    }

    @Test
    public void testConflict() throws Exception {
        String testRepoName = testIdWatcher.getEntityName("repository");
        String testDescription = testRepoName + " description";

        logger.debug("Creating repo with name: {}", testRepoName);

        repositoryService.createRepository(testRepoName, testDescription, null, false);

        getL10nJCommander().run(
                "repo-create",
                Param.REPOSITORY_NAME_SHORT, testRepoName,
                Param.REPOSITORY_DESCRIPTION_SHORT, testDescription,
                Param.REPOSITORY_LOCALES_SHORT, "fr-FR", "(fr-CA)->fr-FR", "en-GB", "(en-CA)->en-GB", "en-AU->en-GB"
        );

        assertTrue("Error because the repository has already been created", outputCapture.toString().contains("Repository with name [" + testRepoName + "] already exists"));
    }

    @Test
    @Ignore("we don't support multilevel hierarechy yet with cycle check.  Running test will result in: java.lang.IllegalArgumentException: Parent locale: en-CA doesn't exist in repository")
    public void testCreateTestRepoWithMultiLevelHiererchyAcrossTwoChains() throws Exception {
        String testRepoName = testIdWatcher.getEntityName("repository");
        String testDescription = testRepoName + " description";

        logger.debug("Creating repo with name: {}", testRepoName);

        getL10nJCommander().run(
                "repo-create",
                Param.REPOSITORY_NAME_SHORT, testRepoName,
                Param.REPOSITORY_DESCRIPTION_SHORT, testDescription,
                Param.REPOSITORY_LOCALES_SHORT, "fr-FR", "(fr-CA)->fr-FR", "en-GB", "en-CA->en-GB", "en-AU->en-CA"
        );

        assertTrue(outputCapture.toString().contains("--> repository id: "));

        Repository repository = repositoryRepository.findByName(testRepoName);

        assertEquals(testDescription, repository.getDescription());
        assertEquals("Total number of locales including the root locale", 6, repository.getRepositoryLocales().size());

        Map<String, RepositoryLocale> repoLocaleMapByBcp47Tag = getRepoLocaleMapByBcp47Tag(repository);

        assertNotNull(repoLocaleMapByBcp47Tag.get("fr-FR"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("fr-CA"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("en-GB"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("en-CA"));
        assertNotNull(repoLocaleMapByBcp47Tag.get("en-AU"));

        RepositoryLocale en = repoLocaleMapByBcp47Tag.get("en");
        assertNotNull("This is the root locale", en);

        assertEquals("en is the root locale", en, repoLocaleMapByBcp47Tag.get("fr-FR").getParentLocale());
        assertEquals("fr-FR", repoLocaleMapByBcp47Tag.get("fr-CA").getParentLocale().getLocale().getBcp47Tag());
        assertEquals("en-GB", repoLocaleMapByBcp47Tag.get("en-CA").getParentLocale().getLocale().getBcp47Tag());
        assertEquals("en-CA", repoLocaleMapByBcp47Tag.get("en-AU").getParentLocale().getLocale().getBcp47Tag());
        assertEquals("en is the root locale", en, repoLocaleMapByBcp47Tag.get("en-GB").getParentLocale());

        assertTrue(repoLocaleMapByBcp47Tag.get("fr-FR").isToBeFullyTranslated());
        assertFalse(repoLocaleMapByBcp47Tag.get("fr-CA").isToBeFullyTranslated());
        assertTrue(repoLocaleMapByBcp47Tag.get("en-GB").isToBeFullyTranslated());
        assertTrue(repoLocaleMapByBcp47Tag.get("en-CA").isToBeFullyTranslated());
        assertTrue(repoLocaleMapByBcp47Tag.get("en-AU").isToBeFullyTranslated());
    }

    @Test
    @Ignore("This has yet to be implemented")
    public void testUnsupportedMultipleLevelChain() throws Exception {
        String testRepoName = testIdWatcher.getEntityName("repository");
        String testDescription = testRepoName + " description";

        getL10nJCommander().run(
                "repo-create",
                Param.REPOSITORY_NAME_SHORT, testRepoName,
                Param.REPOSITORY_DESCRIPTION_SHORT, testDescription,
                Param.REPOSITORY_LOCALES_SHORT, "fr-FR", "(fr-CA)->fr-FR", "en-GB", "(en-CA)->en-GB", "en-AU->en-CA->en-GB"
        );

        assertTrue("Multi level chain with en-AU->en-CA->en-GB is not supported", outputCapture.toString().contains(COMMAND_ERROR_MESSAGE));
    }

    @Test
    public void testForConflict1() throws Exception {

        getL10nJCommander().run("repo-create",
                Param.REPOSITORY_NAME_SHORT, testIdWatcher.getEntityName("repository"),
                Param.REPOSITORY_DESCRIPTION_SHORT, testIdWatcher.getEntityName("repository") + " description",
                Param.REPOSITORY_LOCALES_SHORT, "en-GB", "en-CA->en-GB", "en-GB->en-CA");

        assertTrue("There should be a conflict for the set of locales", outputCapture.toString().contains(COMMAND_ERROR_MESSAGE));
    }

    @Test
    public void testForConflict2() throws Exception {

        getL10nJCommander().run("repo-create",
                Param.REPOSITORY_NAME_SHORT, testIdWatcher.getEntityName("repository"),
                Param.REPOSITORY_DESCRIPTION_SHORT, testIdWatcher.getEntityName("repository") + " description",
                Param.REPOSITORY_LOCALES_SHORT, "en-GB", "en-CA->en-GB", "en-AU->en-CA", "en-GB->en-AU");

        assertTrue("There should be a conflict for the set of locales", outputCapture.toString().contains(COMMAND_ERROR_MESSAGE));
    }

    @Test
    public void testForConflict3() throws Exception {

        getL10nJCommander().run("repo-create",
                Param.REPOSITORY_NAME_SHORT, testIdWatcher.getEntityName("repository"),
                Param.REPOSITORY_DESCRIPTION_SHORT, testIdWatcher.getEntityName("repository") + " description",
                Param.REPOSITORY_LOCALES_SHORT, "fr-FR", "fr-CH->fr-FR", "fr-CA->fr-CH", "fr-CA->fr-FR");

        assertTrue("There should be a conflict for the set of locales", outputCapture.toString().contains(COMMAND_ERROR_MESSAGE));
    }

    @Test
    public void testForConflictForRootLocale() throws Exception {

        getL10nJCommander().run("repo-create",
                Param.REPOSITORY_NAME_SHORT, testIdWatcher.getEntityName("repository"),
                Param.REPOSITORY_DESCRIPTION_SHORT, testIdWatcher.getEntityName("repository") + " description",
                Param.REPOSITORY_LOCALES_SHORT, "fr-FR", "fr-CH->fr-FR", "fr-CA->fr-FR", "en");

        assertTrue("There should be a conflict for the set of locales", outputCapture.toString().contains("Locale [en] cannot be added because it is the root locale"));
    }

    @Test
    public void testForCycle1() throws Exception {

        getL10nJCommander().run("repo-create",
                Param.REPOSITORY_NAME_SHORT, testIdWatcher.getEntityName("repository"),
                Param.REPOSITORY_DESCRIPTION_SHORT, testIdWatcher.getEntityName("repository") + " description",
                Param.REPOSITORY_LOCALES_SHORT, "fr-FR", "fr-FR->fr-FR");

        assertTrue("There should be a conflict for the set of locales", outputCapture.toString().contains(COMMAND_ERROR_MESSAGE));
    }

    @Test
    public void testForCycle2() throws Exception {
        String testRepoName = testIdWatcher.getEntityName("repository");
        String testDescription = testRepoName + " description";

        getL10nJCommander().run(
                "repo-create",
                Param.REPOSITORY_NAME_SHORT, testRepoName,
                Param.REPOSITORY_DESCRIPTION_SHORT, testDescription,
                Param.REPOSITORY_LOCALES_SHORT, "en-AU->en-CA", "en-CA->en-AU"
        );

        assertTrue("There should be a conflict between \"en-CA->en-GB\", \"en-GB->en-CA\"", outputCapture.toString().contains("Found a cycle"));
    }

    @Test
    public void testDifferentSourceLocale() {
        String testRepoName = testIdWatcher.getEntityName("repository");
        String testDescription = testRepoName + " description";

        getL10nJCommander().run(
                "repo-create",
                Param.REPOSITORY_NAME_SHORT, testRepoName,
                Param.REPOSITORY_DESCRIPTION_SHORT, testDescription,
                Param.REPOSITORY_SOURCE_LOCALE_SHORT, "fr-FR",
                Param.REPOSITORY_LOCALES_SHORT, "en", "it-IT"
        );
    }
}
