package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author emagalindan
 */
public class GitBlameCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameCommandTest.class);

    @Test
    public void blameAndroidStrings() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath());

        getL10nJCommander().run("git-blame", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath(),
                "-ft", "ANDROID_STRINGS");
    }

    @Test
    public void blamePoFile() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath());

        getL10nJCommander().run("git-blame", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath(),
                "-ft", "PO");
    }

    @Test
    public void blamePoFileFromExtracted() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath());

        getL10nJCommander().run("git-blame", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath(),
                "-ft", "PO",
                "--extracted-prefix", "/extracted/path/prefix/");
    }

    @Test
    public void testSplit() {
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_zero"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_one"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_two"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_few"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_many"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_other"));
        assertEquals("test_test", GitBlameCommand.textUnitNameToStringInSourceFile("test_test"));
    }
}