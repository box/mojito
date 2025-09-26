package com.box.l10n.mojito.rest.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class CliWSTest {

  CliWS cliWS = new CliWS();

  @Test
  public void getAuthenticationHeadersEmptyWhenMissing() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    Map<String, String> headers = cliWS.getAuthenticationHeaders(request);
    assertTrue(headers.isEmpty());
  }

  @Test
  public void getAuthenticationHeadersReturnsConfigured() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CliWS.CF_ACCESS_HEADER_CLIENT_ID, "id");
    request.addHeader(CliWS.CF_ACCESS_HEADER_CLIENT_SECRET, "secret");

    Map<String, String> headers = cliWS.getAuthenticationHeaders(request);

    assertEquals(2, headers.size());
    assertEquals(
        "L10N_RESTTEMPLATE_HEADER_HEADERS_CF_ACCESS_CLIENT_ID",
        headers.get(CliWS.CF_ACCESS_HEADER_CLIENT_ID));
    assertEquals(
        "L10N_RESTTEMPLATE_HEADER_HEADERS_CF_ACCESS_CLIENT_SECRET",
        headers.get(CliWS.CF_ACCESS_HEADER_CLIENT_SECRET));
  }
}
