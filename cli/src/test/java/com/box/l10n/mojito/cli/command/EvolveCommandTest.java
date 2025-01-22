package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.model.LocaleRepository;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.evolve.Course;
import com.box.l10n.mojito.evolve.Evolve;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class EvolveCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(EvolveCommandTest.class);

  @Autowired(required = false)
  Evolve evolve;

  @Test
  public void execute() throws Exception {
    Assume.assumeNotNull(evolve);
    Repository repository = createTestRepoUsingRepoService();
    getL10nJCommander().run("evolve-sync", "-r", repository.getName());
  }

  @Test
  public void writeJsonTo() {
    EvolveCommand evolveCommand = new EvolveCommand();
    evolveCommand.writeJsonTo = getTargetTestDir().toString();

    RepositoryRepository repository = new RepositoryRepository();
    repository.setName("evolveRepository");

    Course course = new Course();
    course.setId("course1");

    LocaleRepository locale = new LocaleRepository();
    locale.setBcp47Tag("fr-FR");

    evolveCommand.writeJsonToFile(repository, course, locale, "{\"key\" : \"value\"}");
    checkExpectedGeneratedResources();
  }
}
