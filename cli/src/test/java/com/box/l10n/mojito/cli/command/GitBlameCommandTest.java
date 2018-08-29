package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.aspect.StopWatch;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author wyau
 */
public class GitBlameCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameCommandTest.class);


    // tests android_strings (.xml files)
    @Test
    public void blameAndroidStrings() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");


        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath());

        logger.info("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("git-blame", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath(),
//                "-s", "/Users/emagalindan/code/android",
                "-ft", "ANDROID_STRINGS");

    }

    // tests po files
    @Test
    public void blamePoFile() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");


        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath());

        logger.info("test po file");
        logger.info("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("git-blame", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath(), // TODO: create a test file for this
//                "-s", "/Users/emagalindan/code/pinboard/",
                "-ft", "PO");
    }

    @Test
    public void testSplit() {
        //tests textUnitNameToStringInSourceFile()
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _zero", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _one", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _two", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _few", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _many", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _other", true));

    }
}