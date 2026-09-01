package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class CommandHelperRepoTypeClientErrorTest {

  @Test
  public void mapsNotFoundBodyWhenPresent() {
    HttpClientErrorException ex = exception(HttpStatus.NOT_FOUND, "RepoType with id: 9 not found");
    assertEquals(
        "RepoType with id: 9 not found", CommandHelper.repoTypeClientError(ex).getMessage());
  }

  @Test
  public void mapsNotFoundFallbackWhenBodyBlank() {
    HttpClientErrorException ex = exception(HttpStatus.NOT_FOUND, "");
    assertEquals("Repo type is not found", CommandHelper.repoTypeClientError(ex).getMessage());
  }

  @Test
  public void stillMapsBadRequestAndConflict() {
    assertEquals(
        "name is required",
        CommandHelper.repoTypeClientError(exception(HttpStatus.BAD_REQUEST, "name is required"))
            .getMessage());
    assertEquals(
        "Repo type already exists",
        CommandHelper.repoTypeClientError(exception(HttpStatus.CONFLICT, "")).getMessage());
  }

  @Test
  public void forbiddenAndUnauthorizedAreRethrown() {
    HttpClientErrorException forbidden = exception(HttpStatus.FORBIDDEN, "Access Denied");
    try {
      CommandHelper.repoTypeClientError(forbidden);
      fail("expected rethrow");
    } catch (HttpClientErrorException rethrown) {
      assertSame(forbidden, rethrown);
    }

    HttpClientErrorException unauthorized = exception(HttpStatus.UNAUTHORIZED, "nope");
    try {
      CommandHelper.repoTypeClientError(unauthorized);
      fail("expected rethrow");
    } catch (HttpClientErrorException rethrown) {
      assertSame(unauthorized, rethrown);
    }
  }

  private static HttpClientErrorException exception(HttpStatus status, String body) {
    return HttpClientErrorException.create(
        status,
        status.getReasonPhrase(),
        HttpHeaders.EMPTY,
        body.getBytes(StandardCharsets.UTF_8),
        StandardCharsets.UTF_8);
  }
}
