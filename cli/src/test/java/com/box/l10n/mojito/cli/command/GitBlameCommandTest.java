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

import java.io.File;
import java.io.IOException;
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
//        File sourceDirectory = getInputResourcesTestDir("source");

//        logger.debug("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("git-blame", "-r", repository.getName(),
                "-s", "/Users/jeanaurambault/code/android-l10n",
                "-ft", "ANDROID_STRINGS");



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

    public void poFile() {
        try {

            logger.info("Lookup file usages");

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository repository = builder
                    .setWorkTree(new File("/Users/jeanaurambault/code/android"))
                    .readEnvironment()
                    .build();

            logger.info("after test blame");


            Map<String, List<Integer>> filesAndLinesToBlame = getUsages();

            for (Map.Entry<String, List<Integer>> stringListEntry : filesAndLinesToBlame.entrySet()) {
                String filename = stringListEntry.getKey();
                logger.info("filename: {}", filename);

                if (!Paths.get("/Users/jeanaurambault/code/pinboard/", filename).toFile().exists()) {
                    logger.info("file: {}, doesn't not exist any more, skip.", filename);
                    continue;
                }

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
                        String content = blame.getResultContents().getString(line);
                        logger.info("blame, line: {} --> {}", line, content);
                        logger.info("blame, author: {}", blame.getSourceAuthor(line - 1));
                    } catch (Exception ex) {
                        logger.error("get source author failed: {}:{}", filename, line);
                    }
                }
            }
        } catch (IOException io) {
            logger.error("git blame failed", io);
        } catch (GitAPIException e) {
            logger.error("git blame failed", e);
        }
    }


    Map<String, List<Integer>> getUsages() {
        Page<AssetTextUnit> all = assetTextUnitRepository.findAll(new PageRequest(0, 10));


        Map<String, List<Integer>> filesAndLines = new HashMap<>();

        for (AssetTextUnit assetTextUnit : all) {
            Set<String> usages = assetTextUnit.getUsages();

            for (String usage : usages) {
                String[] split = usage.split(":");
                String filename = split[0];
                Integer line = Integer.valueOf(split[1]);

                List<Integer> integers = filesAndLines.get(filename);

                if (integers == null) {
                    integers = new ArrayList<>();
                    filesAndLines.put(filename, integers);
                }

                integers.add(line);
            }
        }

        return filesAndLines;
    }
}