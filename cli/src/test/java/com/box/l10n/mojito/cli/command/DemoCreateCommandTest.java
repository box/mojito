package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** @author jaurambault */
public class DemoCreateCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DemoCreateCommandTest.class);

  protected static final String COMMAND_ERROR_MESSAGE = "Error creating repository";

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryService repositoryService;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Test
  public void testDemoCreate() throws Exception {

    String testRepoName = testIdWatcher.getEntityName("repository");

    logger.debug("Creating repo with name: {}", testRepoName);

    getL10nJCommander()
        .run(
            "demo-create",
            "-n",
            testRepoName,
            "-o",
            getTargetTestDir("outputDir").getAbsolutePath());

    Repository repository = repositoryRepository.findByName(testRepoName);

    TextUnitSearcherParameters searchParameters = new TextUnitSearcherParameters();
    searchParameters.setRepositoryIds(repository.getId());

    List<TextUnitDTO> search = textUnitSearcher.search(searchParameters);
    Assert.assertEquals("Number of translations added not correct", 1575, search.size());

    checkExpectedGeneratedResources();
  }
}
