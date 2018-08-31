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

/**
 * @author emagalindan
 */
public class GitBlameCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameCommandTest.class);

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
        String lines[] = new String[]{
                "<string name=\"test_0\">Test 0</string>\n",
                "<plurals name=\"plural_tests\">\n",
                "    <string name=\"test_1\">Test 1</string>"};
        TextUnitWithUsage textUnitWithUsage1 = new TextUnitWithUsage();
        textUnitWithUsage1.setTextUnitName("test_0");
        TextUnitWithUsage textUnitWithUsage2 = new TextUnitWithUsage();
        textUnitWithUsage2.setTextUnitName("plural_tests_one");
        TextUnitWithUsage textUnitWithUsage3 = new TextUnitWithUsage();
        textUnitWithUsage3.setTextUnitName("test_1");

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

//    Integration test for this?
//    @Test
//    public void getRepository() throws Exception{
//        GitBlameCommand gitBlameCommand = new GitBlameCommand();
//        org.eclipse.jgit.lib.Repository repository = gitBlameCommand.getGitRepository();
//        File sourceDirectory = getInputResourcesTestDir("source");
//        assertEquals(sourceDirectory, repository.getDirectory());
//    }

}