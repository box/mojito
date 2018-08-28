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


    @Test
    public void testCommandName() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");


        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath());

        logger.info("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("git-blame", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath(),
                "-ft", "ANDROID_STRINGS");

    }

    @Test
    public void testBlameWithTextUnitUsages() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");


        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath());

        logger.info("test po file");
        logger.info("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("git-blame", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath(),
                "-ft", "PO");
    }


    @Test
    public void testSplit() {
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _zero", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _one", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _two", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _few", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _many", true));
    logger.info("after trans: {}", GitBlameCommand.textUnitNameToStringInSourceFile("test _other", true));

}

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

//    @Test
//    @StopWatch
//    public void testBlame() throws Exception {
//
//    }


//    public void androidFile() throws Exception {
//        logger.info("Annotate asset:");
//
//        FileRepositoryBuilder builder = new FileRepositoryBuilder();
//        org.eclipse.jgit.lib.Repository repository = builder
//                .setWorkTree(new File("/Users/jeanaurambault/code/android"))
//                .readEnvironment()
//                .build();
//
//
//        BlameCommand blamer = new BlameCommand(repository);
//        ObjectId commitID = repository.resolve("HEAD");
//        blamer.setStartCommit(commitID);
//        blamer.setFilePath("Pinterest/src/main/res/values/strings.xml");
//        BlameResult blame = blamer.call();
//
//        for (int i = 0; i < blame.getResultContents().size(); i++) {
//            String lineText = blame.getResultContents().getString(i);
//
//            String textUnitName = getTextUnitNameFromLine(lineText);
//
//            if (textUnitName != null) {
//                logger.info("{} --> {}", textUnitName, lineText);
//            }
//        }
//    }

    @Test
    public void importPo() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        poFile();

    }

    public void poFile() {
        try {

            logger.info("Lookup file usages");

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository repository = builder
                    .setWorkTree(new File("/Users/emagalindan/code/pinboard/"))
                    .readEnvironment()
                    .build();

            logger.info("after test blame; repository: {}", repository);


            Map<String, List<Integer>> filesAndLinesToBlame = getUsages();

            for (Map.Entry<String, List<Integer>> stringListEntry : filesAndLinesToBlame.entrySet()) {
                String filename = stringListEntry.getKey();

                logger.info("filename {}", filename);
                if (!Paths.get("/Users/emagalindan/code/pinboard/", filename).toFile().exists()) {
                    logger.info("file: {}, doesn't not exist any more, skip.", filename);
                    continue;
                }

                logger.info("blame file: {}", filename);

                BlameCommand blamer = new BlameCommand(repository);
                ObjectId commitID = repository.resolve("HEAD");
                blamer.setStartCommit(commitID);
                blamer.setFilePath(filename);
                BlameResult blame = blamer.call();

                if (blame == null) {
                    logger.info("blame is null, continue");
                    continue;
                }
                for (Integer line : stringListEntry.getValue()) {

                    try {
                        String content = blame.getResultContents().getString(line - 1);
                        logger.info("blame, line: {} --> {}", line, content);
                        logger.info("blame, author: {}", blame.getSourceAuthor(line - 1));
//                        logger.info("blame, commit: {}", blame.getSourceCommit(line - 1));
                    } catch (Exception ex) {
                        logger.error("get source author failed: {}:{}", filename, line - 1);
                    }
                }
            }
        } catch (IOException io) {
            logger.error("git blame failed", io);
        } catch (GitAPIException e) {
            logger.error("git blame failed", e);
        }
    }

    // po files
    Map<String, List<Integer>> getUsages() {
        Page<AssetTextUnit> all = assetTextUnitRepository.findAll(new PageRequest(0, 10));

        logger.info("in getUsages()");
        logger.info("all: {}", all);

        Map<String, List<Integer>> filesAndLines = new HashMap<>();

        for (AssetTextUnit assetTextUnit : all) {
//            logger.info("assetTextUnit: {}", assetTextUnit.getName());
            Set<String> usages = assetTextUnit.getUsages();

//            if (usages.isEmpty()) {
//                logger.info("No usages for text unit {}", assetTextUnit.getName());
//            }
//            else {
//                logger.info("Usages: {}", usages);
//            }

            for (String usage : usages) {
                String[] split = usage.split(":");
                String filename = split[0];
                // fix for files starting with "/mnt/jenkins/workspace/webapp-l10n-string-extract/"
                filename = filename.replace("/mnt/jenkins/workspace/webapp-l10n-string-extract/", "");
                Integer line = Integer.valueOf(split[1]);

                List<Integer> integers = filesAndLines.get(filename);

                if (integers == null) {
                    integers = new ArrayList<>();
                    filesAndLines.put(filename, integers);
                }

                integers.add(line);
            }
        }

        logger.info("filesAndLines: {}", filesAndLines);

        return filesAndLines;
    }
}