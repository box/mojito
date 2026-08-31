package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.system.OutputCaptureRule;

/**
 * {@code repo-type-list} {@code execute()} against a stubbed {@link RepoTypeClient} (no JCommander,
 * no HTTP). Empty list print only; HTTP errors are not mapped (same as {@code repo-type-view}).
 */
public class RepoTypeListCommandClientTest {

  @Rule public OutputCaptureRule outputCapture = new OutputCaptureRule();

  RepoTypeClient repoTypeClient;
  RepoTypeListCommand command;

  @Before
  public void setUp() {
    repoTypeClient = mock(RepoTypeClient.class);
    command = new RepoTypeListCommand();
    command.repoTypeClient = repoTypeClient;
    command.consoleWriter =
        new ConsoleWriter(false, ConsoleWriter.OutputType.ANSI_CONSOLE_AND_LOGGER);
  }

  @Test
  public void printsNoRepoTypesFoundAndNoTypeBlocks() throws Exception {
    when(repoTypeClient.getRepoTypes(null)).thenReturn(Collections.emptyList());

    command.execute();

    String output = outputCapture.toString();
    assertTrue(output.contains("List repo types"));
    assertTrue(output.contains("No repo types found"));
    assertFalse(output.contains("Repo type id -->"));
  }
}
