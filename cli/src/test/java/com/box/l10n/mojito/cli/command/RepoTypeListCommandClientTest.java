package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * {@code repo-type-list} {@code execute()} against a stubbed {@link RepoTypeClient} (no JCommander,
 * no HTTP). Empty list print and 4xx mapping.
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

  @Test
  public void listHttpErrorDoesNotUseMutateNotFoundCopy() throws Exception {
    when(repoTypeClient.getRepoTypes(null))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8));

    try {
      command.execute();
      fail("expected CommandException");
    } catch (CommandException e) {
      assertEquals("Failed to list repo types", e.getMessage());
    }
  }

  @Test
  public void listHttpErrorUsesResponseBodyWhenPresent() throws Exception {
    when(repoTypeClient.getRepoTypes(null))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                HttpHeaders.EMPTY,
                "list request rejected".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8));

    try {
      command.execute();
      fail("expected CommandException");
    } catch (CommandException e) {
      assertEquals("list request rejected", e.getMessage());
    }
  }
}
