package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.locale.LocaleService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jeanaurambault
 */
public class ImportLocalizedAssetCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropXliffImportCommandTest.class);

    @Autowired
    LocaleService localeService;

    @Test
    public void importAndroidStrings() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importAndroidStringsPlural() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        repositoryService.addRepositoryLocale(repository, "ru-RU");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importMacStrings() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importMacStringsdict() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        repositoryService.addRepositoryLocale(repository, "ru-RU");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importPo() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        repositoryService.addRepositoryLocale(repository, "ar-SA");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-lm", "ar:ar-SA,fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-lm", "ar:ar-SA,fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importPoPlural() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        repositoryService.addRepositoryLocale(repository, "ru-RU");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP,ru-RU:ru-RU");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP,ru-RU:ru-RU");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importProperties() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importPropertiesJava() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_JAVA");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-ft", "PROPERTIES_JAVA");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_JAVA",
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importPropertiesNoBaseName() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME",
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importPropertiesNoBasenameMultiDirectory() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importResw() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importResx() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importJson() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "JSON");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-ft", "JSON");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-ft", "JSON");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importJsonNobasename() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "JSON_NOBASENAME");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-ft", "JSON_NOBASENAME");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-ft", "JSON_NOBASENAME");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importJsonWithNote() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-fo", "noteKeyPattern=note", "extractAllPairs=false", "exceptions=string",
                "-ft", "JSON");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-fo", "noteKeyPattern=note", "extractAllPairs=false", "exceptions=string",
                "-ft", "JSON");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-fo", "noteKeyPattern=note", "extractAllPairs=false", "exceptions=string",
                "-ft", "JSON");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importJsonDefaultFormatJs() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "JSON_NOBASENAME",
                "-fo", "noteKeyPattern=description", "extractAllPairs=false", "exceptions=defaultMessage");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-ft", "JSON_NOBASENAME",
                "-fo", "noteKeyPattern=description", "extractAllPairs=false", "exceptions=defaultMessage");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-ft", "JSON_NOBASENAME",
                "-fo", "noteKeyPattern=description", "extractAllPairs=false", "exceptions=defaultMessage");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importJsonI18NextParser() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
            "-s", getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft", "I18NEXT_PARSER_JSON");

        getL10nJCommander().run("import", "-r", repository.getName(),
            "-s", getInputResourcesTestDir("source").getAbsolutePath(),
            "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
            "-ft", "I18NEXT_PARSER_JSON");

        getL10nJCommander().run("pull", "-r", repository.getName(),
            "-s", getInputResourcesTestDir("source").getAbsolutePath(),
            "-t", getTargetTestDir().getAbsolutePath(),
            "-ft", "I18NEXT_PARSER_JSON");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importXcodeXliff() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-ft", "XCODE_XLIFF");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
                "-ft", "XCODE_XLIFF");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
                "-ft", "XCODE_XLIFF");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importXtb() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-sl", "en-US");

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath(),
                "-sl", "en-US");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath(),
                "-sl", "en-US");

        checkExpectedGeneratedResources();
    }

    @Test
    public void importCsv() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }


    @Test
    public void importUnused() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source2").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-t", getTargetTestDir().getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void importDifferentSourceLocale() throws Exception {
        String repoName = testIdWatcher.getEntityName("repository");

        Locale frFRLocale = localeService.findByBcp47Tag("fr-FR");

        Repository repository = repositoryService.createRepository(repoName, repoName + " description", frFRLocale, false);

        repositoryService.addRepositoryLocale(repository, "en", "fr-FR", true);
        repositoryService.addRepositoryLocale(repository, "en-US", "en", false);
        repositoryService.addRepositoryLocale(repository, "ja-JP");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("import", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getInputResourcesTestDir("translations").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("withNoMapping").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("withMapping").getAbsolutePath(),
                "-lm", "en-US:en-US,en:en,fr-FR:fr-FR");

        checkExpectedGeneratedResources();
    }
}
