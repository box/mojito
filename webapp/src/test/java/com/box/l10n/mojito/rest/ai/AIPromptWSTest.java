package com.box.l10n.mojito.rest.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.service.ai.PromptService;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

public class AIPromptWSTest extends WSTestBase {

  @Autowired private AIPromptWS aiPromptWS;

  @Mock private PromptService aiPromptService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    aiPromptWS.promptService = aiPromptService;
  }

  @Test
  public void testCreatePrompt() {
    AIPromptCreateRequest request = new AIPromptCreateRequest();
    aiPromptWS.createPrompt(request);
    verify(aiPromptService, times(1)).createPrompt(request);
  }

  @Test
  public void testDeletePrompt() {
    Long promptId = 1L;
    aiPromptWS.deletePrompt(promptId);
    verify(aiPromptService, times(1)).deletePrompt(promptId);
  }

  @Test
  public void testGetPrompt() {
    Long promptId = 1L;
    com.box.l10n.mojito.entity.AIPrompt aiPrompt = new com.box.l10n.mojito.entity.AIPrompt();
    when(aiPromptService.getPrompt(promptId)).thenReturn(aiPrompt);

    AIPrompt prompt = aiPromptWS.getPrompt(promptId);
    assertNotNull(prompt);
    verify(aiPromptService, times(1)).getPrompt(promptId);
  }

  @Test
  public void testGetAllActivePrompts() {
    com.box.l10n.mojito.entity.AIPrompt aiPrompt1 = new com.box.l10n.mojito.entity.AIPrompt();
    com.box.l10n.mojito.entity.AIPrompt aiPrompt2 = new com.box.l10n.mojito.entity.AIPrompt();
    when(aiPromptService.getAllActivePrompts()).thenReturn(Arrays.asList(aiPrompt1, aiPrompt2));

    List<AIPrompt> prompts = aiPromptWS.getAllActivePrompts();
    assertEquals(2, prompts.size());
    verify(aiPromptService, times(1)).getAllActivePrompts();
  }

  @Test
  public void testGetAllActivePromptsForRepository() {
    String repositoryName = "repo1";
    com.box.l10n.mojito.entity.AIPrompt aiPrompt1 = new com.box.l10n.mojito.entity.AIPrompt();
    com.box.l10n.mojito.entity.AIPrompt aiPrompt2 = new com.box.l10n.mojito.entity.AIPrompt();

    when(aiPromptService.getAllActivePromptsForRepository(repositoryName))
        .thenReturn(Arrays.asList(aiPrompt1, aiPrompt2));

    List<AIPrompt> prompts = aiPromptWS.getAllActivePromptsForRepository(repositoryName);
    assertEquals(2, prompts.size());
    verify(aiPromptService, times(1)).getAllActivePromptsForRepository(repositoryName);
  }
}
