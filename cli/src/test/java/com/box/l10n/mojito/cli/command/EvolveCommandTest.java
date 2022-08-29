package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.evolve.Course;
import com.box.l10n.mojito.evolve.Evolve;
import com.box.l10n.mojito.rest.entity.Locale;
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

    com.box.l10n.mojito.rest.entity.Repository repository =
        new com.box.l10n.mojito.rest.entity.Repository();
    repository.setName("evolveRepository");

    Course course = new Course();
    course.setId("course1");

    Locale locale = new Locale();
    locale.setBcp47Tag("fr-FR");

    evolveCommand.writeJsonToFile(repository, course, locale, "{\"key\" : \"value\"}");
    checkExpectedGeneratedResources();
  }
}
