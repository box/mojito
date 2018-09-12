package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
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
    public void getRepository() throws Exception{
        File sourceDirectory = getInputResourcesTestDir("source");
        String filepath = sourceDirectory.getAbsolutePath();

        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        gitBlameCommand.sourceDirectoryParam = filepath;

        org.eclipse.jgit.lib.Repository repository = gitBlameCommand.getGitRepository();
        logger.info(repository.toString());

        // Make sure source file is in the same repository as git repository
        assertTrue(sourceDirectory.toPath().startsWith(repository.getDirectory().toPath().getParent()));
    }

    @Test
    public void getBlameResultForLine() throws Exception{
        File sourceDirectory = getInputResourcesTestDir("source");
        String filepath = sourceDirectory.getAbsolutePath();

        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        gitBlameCommand.sourceDirectoryParam = filepath;
        org.eclipse.jgit.lib.Repository repository = gitBlameCommand.getGitRepository();
        String relativePath = repository.getDirectory().toPath().getParent().relativize(sourceDirectory.toPath()).toString();
        relativePath = relativePath + "/res/values/strings.xml";
        BlameResult blameResult = gitBlameCommand.getBlameResultForFile(relativePath);

        // Will not hold up if file is committed by another person and/or at another time
        String expectedAuthor = "Liz Magalindan";
        String expectedEmail = "emagalindan@pinterest.com";
        String expectedSourceCommit = "commit 418b15b2eadf3e8f844ea5595b4e1cd6932e0237 1536096408 -----p";
        int expectedTime = 1536096408;
        for (int lineNumber = 0; lineNumber < blameResult.getResultContents().size(); lineNumber++) {
            assertEquals(expectedAuthor, blameResult.getSourceAuthor(lineNumber).getName());
            assertEquals(expectedEmail, blameResult.getSourceAuthor(lineNumber).getEmailAddress());
            assertEquals(expectedSourceCommit, blameResult.getSourceCommit(lineNumber).toString());
            assertEquals(expectedTime, blameResult.getSourceCommit(lineNumber).getCommitTime());
        }
    }

    @Test
    public void getFileName() {
        assertEquals("file.js", GitBlameCommand.getFileName("file.js"));
        assertEquals("file.js", GitBlameCommand.getFileName("file.js:25"));
        assertEquals("path/to/file.js", GitBlameCommand.getFileName("path/to/file.js"));
        assertEquals("path/to/file.js", GitBlameCommand.getFileName("path/to/file.js:25"));
    }

    @Test
    public void getFileLine() throws Exception {
        assertEquals(24, GitBlameCommand.getLineNumber("file.js:25"));
        assertEquals(24, GitBlameCommand.getLineNumber("path/to/file.js:25"));
    }

}