package com.box.l10n.mojito.rest.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.service.ai.LLMService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

public class AIChecksWSTest extends WSTestBase {

  @Autowired private AIChecksWS aiChecksWS;

  @Mock private LLMService llmService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    aiChecksWS.llmService = llmService;
  }

  @Test
  public void testExecuteAIChecks() {
    AICheckRequest request = new AICheckRequest();
    AICheckResponse expectedResponse = new AICheckResponse();
    when(llmService.executeAIChecks(request)).thenReturn(expectedResponse);

    AICheckResponse actualResponse = aiChecksWS.executeAIChecks(request);
    assertEquals(expectedResponse, actualResponse);
    verify(llmService).executeAIChecks(request);
  }

  @Test
  public void testExecuteAIChecksFailure() {
    AICheckRequest request = new AICheckRequest();
    when(llmService.executeAIChecks(request)).thenThrow(new AIException("Failure in processing"));

    AICheckResponse response = aiChecksWS.executeAIChecks(request);
    assertTrue(response.isError());
    assertEquals("Failure in processing", response.getErrorMessage());
  }
}
