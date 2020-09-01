package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.io.Files;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.google.common.base.Strings;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * By default this run on HSQL, so point it to proper configuration to run with MySQL for example by changing the
 * pom.xml to use webapp configuration instead of cli's.
 */
public class PerformanceTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PerformanceTest.class);

    static int NUMBER_OF_TEXTUNITS = 5;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    AssetClient assetClient;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Value("${test.l10n.cli.performance:false}")
    boolean runPerformance;

//    String repoName = "perfclitest-a-" + NUMBER_OF_TEXTUNITS;
    String repoName = "perf6";

    @Before
    public void before() {
        Assume.assumeTrue(runPerformance);
    }

    @Test
    public void createRepository() throws Exception {
       getOrCreateRepository();
    }

    @Test
    public void push() throws Exception {
        Repository repository = getOrCreateRepository();

        generateInputFiles(getTargetTestDir("input"), 0, NUMBER_OF_TEXTUNITS);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getTargetTestDir("input").getAbsolutePath(),
                "--filter-options", "0"
        );
    }

    @Test
    public void pushSmallDiff() throws Exception {
        Repository repository = getOrCreateRepository();

        generateInputFiles(getTargetTestDir("input"), 0, NUMBER_OF_TEXTUNITS + 20);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getTargetTestDir("input").getAbsolutePath(),
                "--filter-options", "0"
        );
    }

    @Test
    public void extractDiff() throws Exception {
        Repository repository = getOrCreateRepository();

        generateInputFiles(getTargetTestDir("input/master"), 0, NUMBER_OF_TEXTUNITS);
        generateInputFiles(getTargetTestDir("input/branch1"), 0, NUMBER_OF_TEXTUNITS, NUMBER_OF_TEXTUNITS, NUMBER_OF_TEXTUNITS + 5);
        generateInputFiles(getTargetTestDir("input/branch2"), 5, NUMBER_OF_TEXTUNITS, NUMBER_OF_TEXTUNITS + 5, NUMBER_OF_TEXTUNITS + 10);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getTargetTestDir("input/master").getAbsolutePath(),
                "-b", "master",
                "--filter-options", "0"
        );

        getL10nJCommander().run("extract",
                "-s", getTargetTestDir("input/master").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "master",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract",
                "-s", getTargetTestDir("input/branch1").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "branch1",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract",
                "-s", getTargetTestDir("input/branch2").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "branch2",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "branch1",
                "-b", "master",
                "--push-to", repository.getName(),
                "--push-to-branch", "branch1");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "branch2",
                "-b", "master",
                "--push-to", repository.getName(),
                "--push-to-branch", "branch2");

    }

    @Test
    public void changeUsed() throws Exception {
        Repository repository = getOrCreateRepository();

        generateInputFiles(getTargetTestDir("input/master-10"), 0, 10);
        generateInputFiles(getTargetTestDir("input/master-100"), 0, 100);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getTargetTestDir("input/master-10").getAbsolutePath(),
                "-b", "master",
                "--filter-options", "0"
        );

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getTargetTestDir("input/master-100").getAbsolutePath(),
                "-b", "master",
                "--filter-options", "0"
        );

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getTargetTestDir("input/master-10").getAbsolutePath(),
                "-b", "master",
                "--filter-options", "0"
        );
    }

    Repository getOrCreateRepository() throws Exception {
        Repository repository = repositoryRepository.findByName(repoName);

        if (repository == null) {
            repository = repositoryService.createRepository(repoName, repoName + " description", null, false);

            repositoryService.addRepositoryLocale(repository, "fr-FR");
            repositoryService.addRepositoryLocale(repository, "fr-CA", "fr-FR", false);
            repositoryService.addRepositoryLocale(repository, "ja-JP");
        }
        return repository;
    }


    @Test
    public void generate() {
        generateInputFiles(new File("/Users/jeanaurambault/tmp/"+ repoName + "/master"), 0, NUMBER_OF_TEXTUNITS);
        generateInputFiles(new File("/Users/jeanaurambault/tmp/"+ repoName + "/branch1"), 0, NUMBER_OF_TEXTUNITS, NUMBER_OF_TEXTUNITS, NUMBER_OF_TEXTUNITS + 5);
        generateInputFiles(new File("/Users/jeanaurambault/tmp/"+ repoName + "/branch2"), 0, NUMBER_OF_TEXTUNITS, NUMBER_OF_TEXTUNITS + 10, NUMBER_OF_TEXTUNITS + 15);
    }


    void generateInputFiles(File inputsDirectory, int startIdxRange1, int endIdxRange1) {
        generateInputFiles(inputsDirectory, startIdxRange1, endIdxRange1, 0, 0);
    }

    void generateInputFiles(File inputsDirectory, int startIdxRange1, int endIdxRange1, int startIdxRange2, int endIdxRange2) {
        String fileContent = IntStream.concat(IntStream.range(startIdxRange1, endIdxRange1),
                IntStream.range(startIdxRange2, endIdxRange2)).mapToObj(idx -> {
            return String.format("# %s\n%s=%s\n\n",
                    "comment-" + idx + "-" + Strings.padStart("", 50, 'a'),
                    "name-" + idx,
                    "value-" + idx + "-" + Strings.padStart("", 30, 'a'));
        }).collect(Collectors.joining());
        Files.createDirectories(inputsDirectory.toPath());
        Files.write(inputsDirectory.toPath().resolve("performance.properties"), fileContent);
    }

//    void createSourceStrings(VirtualAsset virtualAsset) {
//        logger.debug("create the source strings");
//        List<VirtualAssetTextUnit> virtualAssetTextUnits = IntStream.range(0, NUMBER_OF_TEXTUNITS).mapToObj(idx -> {
//            VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
//            virtualAssetTextUnit.setName("name-" + idx);
//            virtualAssetTextUnit.setContent("content-" + idx + "-" + Strings.padStart("", 30, 'a'));
//            virtualAssetTextUnit.setComment("comment-" + idx + "-" + Strings.padStart("", 50, 'a'));
//
//            return virtualAssetTextUnit;
//        }).collect(Collectors.toList());
//
//        PollableTask pollableTask = virtualAssetClient.repalceTextUnits(virtualAsset.getId(), virtualAssetTextUnits);
//        pollableTaskClient.waitForPollableTask(pollableTask.getId());
//        pollableTask = pollableTaskClient.getPollableTask(pollableTask.getId());
//        logger.debug("create source strings: {}", getElapsedTime(pollableTask));
//    }
}
