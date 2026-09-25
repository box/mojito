package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.RepoType;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * {@code repo-view} {@code execute()} against stubbed clients (no JCommander, no HTTP). Covers
 * degrading when type checkers cannot be loaded.
 */
public class RepoViewTypeCheckersLoadFailureCommandTest {

  @Rule public OutputCaptureRule outputCapture = new OutputCaptureRule();

  RepositoryClient repositoryClient;
  RepoTypeClient repoTypeClient;
  RepoViewCommand command;

  @Before
  public void setUp() {
    repositoryClient = mock(RepositoryClient.class);
    repoTypeClient = mock(RepoTypeClient.class);
    command = new RepoViewCommand();
    command.repositoryClient = repositoryClient;
    command.repoTypeClient = repoTypeClient;
    command.consoleWriter =
        new ConsoleWriter(false, ConsoleWriter.OutputType.ANSI_CONSOLE_AND_LOGGER);
    command.nameParam = "demo";
  }

  @Test
  public void printsNoteAndContinuesWhenTypeCheckersCannotBeLoaded() throws Exception {
    Repository repository = typedRepository();
    when(repositoryClient.getRepositoryByName("demo")).thenReturn(repository);
    when(repoTypeClient.getRepoTypeById(9L))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8));

    command.execute();

    String output = outputCapture.toString();
    assertTrue(output.contains("Repository id --> 1"));
    assertTrue(output.contains("Repository type --> React"));
    assertTrue(output.contains("Repository type checkers --> could not be loaded"));
    assertTrue(output.contains("Repository locales --> fr-FR"));
    assertFalse(output.contains("Integrity checkers -->"));
  }

  private static Repository typedRepository() {
    RepoType repoType = new RepoType();
    repoType.setId(9L);
    repoType.setName("React");

    Locale frFR = new Locale();
    frFR.setBcp47Tag("fr-FR");
    RepositoryLocale repositoryLocale = new RepositoryLocale();
    repositoryLocale.setLocale(frFR);
    repositoryLocale.setToBeFullyTranslated(true);

    Repository repository = new Repository();
    repository.setId(1L);
    repository.setName("demo");
    repository.setRepoType(repoType);
    repository.setRepositoryLocales(Set.of(repositoryLocale));
    return repository;
  }
}
