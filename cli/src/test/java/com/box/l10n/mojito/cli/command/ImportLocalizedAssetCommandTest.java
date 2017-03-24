package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeanaurambault
 */
public class ImportLocalizedAssetCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropXliffImportCommandTest.class);

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
    public void importPo() throws Exception {

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
  
    @Test @Ignore(value = "plural not well supported yet, headers needs to be updateds")
    public void importPoPlural() throws Exception {

        String repoName = testIdWatcher.getEntityName("importpoplural1");
        Repository repository = repositoryService.createRepository(repoName, repoName + " description");
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
    public void importPoPluralLimitedSupport() throws Exception {

        String repoName = testIdWatcher.getEntityName("importpoplural1");
        Repository repository = repositoryService.createRepository(repoName, repoName + " description");
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
    public void importPropertiesNoBasename() throws Exception {

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

}
