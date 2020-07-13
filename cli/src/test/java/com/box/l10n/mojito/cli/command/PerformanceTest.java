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

/**
 * By default this run on HSQL, so point it to proper configuration to run with MySQL for example by changing the
 * pom.xml to use webapp configuration instead of cli's.
 */
public class PerformanceTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PerformanceTest.class);

    static int NUMBER_OF_TEXTUNITS = 100000;

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

    String repoName = "perfclitest-1";

    @Before
    public void before() {
        Assume.assumeTrue(runPerformance);
    }

    @Test
    public void prep() throws Exception {
        Repository repository = repositoryService.createRepository(repoName, repoName + " description", null, false);

        repositoryService.addRepositoryLocale(repository, "fr-FR");
        repositoryService.addRepositoryLocale(repository, "fr-CA", "fr-FR", false);
        repositoryService.addRepositoryLocale(repository, "ja-JP");
    }

    @Test
    public void push() throws Exception {
        Repository repository = repositoryRepository.findByName(repoName);

        generateInputFiles(getTargetTestDir("input"), 0, NUMBER_OF_TEXTUNITS);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getTargetTestDir("input").getAbsolutePath(),
                "--filter-options", "0"
        );
    }

    @Test
    public void pushSmallDiff() throws Exception {
        Repository repository = repositoryRepository.findByName(repoName);

        generateInputFiles(getTargetTestDir("input"), 0, NUMBER_OF_TEXTUNITS + 20);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getTargetTestDir("input").getAbsolutePath(),
                "--filter-options", "2"
        );
    }


    @Test
    public void generate() {
        generateInputFiles(new File("/Users/jeanaurambault/tmp/dofijadoij1309/master"), 0, NUMBER_OF_TEXTUNITS);
        generateInputFiles(new File("/Users/jeanaurambault/tmp/dofijadoij1309/branch1"), 5, NUMBER_OF_TEXTUNITS + 5);
        generateInputFiles(new File("/Users/jeanaurambault/tmp/dofijadoij1309/branch2"), 10, NUMBER_OF_TEXTUNITS + 10);
    }

    void generateInputFiles(File inputsDirectory, int startIdx, int endIdx) {
        String fileContent = IntStream.range(startIdx, endIdx).mapToObj(idx -> {
            return String.format("# %s\n%s=%s\n\n",
                    "comment-" + idx + "-" + Strings.padStart("", 50, 'a'),
                    "name-" + idx,
                    "value-" + idx + "-" + Strings.padStart("", 30, 'a'));
        }).collect(Collectors.joining());
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
