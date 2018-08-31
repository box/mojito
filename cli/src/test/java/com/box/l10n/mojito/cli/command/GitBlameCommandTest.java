package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.entity.TextUnitWithUsage;
import org.eclipse.jgit.blame.BlameResult;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

//    @Test
//    public void blameAndroidStrings() throws Exception {
//        Repository repository = createTestRepoUsingRepoService();
//        File sourceDirectory = getInputResourcesTestDir("source");
//
//        getL10nJCommander().run("push", "-r", repository.getName(),
//                "-s", sourceDirectory.getAbsolutePath());
//
//        getL10nJCommander().run("git-blame", "-r", repository.getName(),
//                "-s", sourceDirectory.getAbsolutePath(),
//                "-ft", "ANDROID_STRINGS");
//    }

//    @Test
//    public void blamePoFile() throws Exception {
//        Repository repository = createTestRepoUsingRepoService();
//        File sourceDirectory = getInputResourcesTestDir("source");
//
//        getL10nJCommander().run("push", "-r", repository.getName(),
//                "-s", sourceDirectory.getAbsolutePath());
//
//        getL10nJCommander().run("git-blame", "-r", repository.getName(),
//                "-s", sourceDirectory.getAbsolutePath(),
//                "-ft", "PO");
//    }
//
//    @Test
//    public void blamePoFileFromExtracted() throws Exception {
//        Repository repository = createTestRepoUsingRepoService();
//        File sourceDirectory = getInputResourcesTestDir("source");
//
//        getL10nJCommander().run("push", "-r", repository.getName(),
//                "-s", sourceDirectory.getAbsolutePath());
//
//        getL10nJCommander().run("git-blame", "-r", repository.getName(),
//                "-s", sourceDirectory.getAbsolutePath(),
//                "-ft", "PO",
//                "--extracted-prefix", "/extracted/path/prefix/");
//    }

//    // GitInfoForTextUnit getBlameResults(int lineNumber, BlameResult blameResultForFile, TextUnitWithUsage textUnitWithUsage) {
//    @Test
//    public void blameResultsFromAndroidStrings() throws Exception {
//        // test something like
//        // getBlameResults(line, blameResult, textUnit)
//
//        GitBlameCommand gitBlameCommand = new GitBlameCommand();
//        List<TextUnitWithUsage> textUnitWithUsages = new ArrayList<>();
//        TextUnitWithUsage textUnitWithUsage = new TextUnitWithUsage();
//        textUnitWithUsage.setTextUnitName("100_character_description");
//        textUnitWithUsages.add(textUnitWithUsage);
//        TextUnitWithUsage textUnitWithUsage2 = new TextUnitWithUsage();
//        textUnitWithUsage.setTextUnitName("101_character_description");
//        textUnitWithUsages.add(textUnitWithUsage2);
//
//        BlameResult blameResult = gitBlameCommand.getBlameResultForFile('test.js'); // ??? is this right? lol
//    }

//    @Test
//    public void getRepository() throws Exception{
//        GitBlameCommand gitBlameCommand = new GitBlameCommand();
//        org.eclipse.jgit.lib.Repository repository = gitBlameCommand.getGitRepository();
//        assertEquals(repository.getDirectory());
//    }

    @Test
    public void getStringInSourceFile() {
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_zero"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_one"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_two"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_few"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_many"));
        assertEquals("test", GitBlameCommand.textUnitNameToStringInSourceFile("test_other"));
        assertEquals("test_test", GitBlameCommand.textUnitNameToStringInSourceFile("test_test"));
        assertEquals("test_test", GitBlameCommand.textUnitNameToStringInSourceFile("test_test_one"));
    }

    @Test
    public void getTextUnitName() {
        String lines[] = new String[]{"<string name=\"something_new\">Something new</string>\n",
                "<plurals name=\"plural_things\">\n"};
        TextUnitWithUsage textUnitWithUsage1 = new TextUnitWithUsage();
        textUnitWithUsage1.setTextUnitName("something_new");
        TextUnitWithUsage textUnitWithUsage2 = new TextUnitWithUsage();
        textUnitWithUsage2.setTextUnitName("plural_things_one");
        TextUnitWithUsage textUnitWithUsage3 = new TextUnitWithUsage();
        textUnitWithUsage3.setTextUnitName("plural_things_other");

        List<TextUnitWithUsage> textUnitWithUsages = new ArrayList<>();
        textUnitWithUsages.add(textUnitWithUsage1);
        textUnitWithUsages.add(textUnitWithUsage2);
        textUnitWithUsages.add(textUnitWithUsage3);

        GitBlameCommand gitBlameCommand = new GitBlameCommand();

        for (int i = 0; i < lines.length; i++)
            assertEquals(textUnitWithUsages.get(i), gitBlameCommand.getTextUnitNameFromLine(lines[i], textUnitWithUsages));
    }

    @Test
    public void getFileName() {
        assertEquals("file.js", GitBlameCommand.getFileName("file.js"));
        assertEquals("file.js", GitBlameCommand.getFileName("file.js:25"));
        assertEquals("path/to/file.js", GitBlameCommand.getFileName("path/to/file.js:25"));
    }

    @Test
    public void getFileLine() throws Exception {
        assertEquals(24, GitBlameCommand.getLineNumber("file.js:25"));
        assertEquals(24, GitBlameCommand.getLineNumber("path/to/file.js:25"));
    }
}