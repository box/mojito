package com.box.l10n.mojito.service.ai.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryAIPrompt;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.rest.ai.AIPromptCreateRequest;
import com.box.l10n.mojito.service.ai.AIPromptRepository;
import com.box.l10n.mojito.service.ai.AIPromptTypeRepository;
import com.box.l10n.mojito.service.ai.RepositoryAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LLMPromptServiceTest {

  @Mock AIPromptRepository aiPromptRepository;

  @Mock AIPromptTypeRepository aiPromptTypeRepository;

  @Mock RepositoryAIPromptRepository repositoryAIPromptRepository;

  @Mock RepositoryRepository repositoryRepository;

  @Captor ArgumentCaptor<RepositoryAIPrompt> repositoryAIPromptCaptor;

  @InjectMocks LLMPromptService LLMPromptService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testPromptCreation() {
    Repository repository = new Repository();
    repository.setId(1L);
    RepositoryAIPrompt repositoryAIPrompt = new RepositoryAIPrompt();
    repositoryAIPrompt.setId(1L);
    AIPromptType promptType = new AIPromptType();
    promptType.setId(1L);
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    AIPromptCreateRequest AIPromptCreateRequest = new AIPromptCreateRequest();
    AIPromptCreateRequest.setRepositoryName("testRepo");
    AIPromptCreateRequest.setPromptType("SOURCE_STRING_CHECKER");
    AIPromptCreateRequest.setUserPrompt("Check strings for spelling");
    AIPromptCreateRequest.setModelName("gtp-3.5-turbo");
    AIPromptCreateRequest.setPromptTemperature(0.0F);
    when(aiPromptRepository.save(any())).thenReturn(prompt);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(promptType);
    when(repositoryAIPromptRepository.save(any())).thenReturn(repositoryAIPrompt);

    LLMPromptService.createPrompt(AIPromptCreateRequest);

    verify(aiPromptTypeRepository, times(1)).findByName("SOURCE_STRING_CHECKER");
    verify(aiPromptRepository, times(1)).save(any());
    verify(repositoryAIPromptRepository, times(1)).save(any());
  }

  @Test
  void testPromptCreationNoPromptType() {

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    AIPromptCreateRequest AIPromptCreateRequest = new AIPromptCreateRequest();
    AIPromptCreateRequest.setRepositoryName("testRepo");
    AIPromptCreateRequest.setPromptType("SOURCE_STRING_CHECKER");
    AIPromptCreateRequest.setUserPrompt("Check strings for spelling");
    AIPromptCreateRequest.setModelName("gtp-3.5-turbo");
    AIPromptCreateRequest.setPromptTemperature(0.0F);
    when(aiPromptRepository.save(any())).thenReturn(prompt);
    when(repositoryRepository.findByName("testRepo")).thenReturn(new Repository());
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(null);
    when(repositoryAIPromptRepository.save(any())).thenReturn(1L);

    AIException exception =
        assertThrows(AIException.class, () -> LLMPromptService.createPrompt(AIPromptCreateRequest));
    assertEquals("Prompt type not found: SOURCE_STRING_CHECKER", exception.getMessage());

    verify(aiPromptTypeRepository, times(1)).findByName("SOURCE_STRING_CHECKER");
    verify(aiPromptTypeRepository, times(0)).save(any());
    verify(repositoryAIPromptRepository, times(0)).save(any());
  }

  @Test
  void testPromptDeletion() {
    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setId(1L);
    aiPrompt.setDeleted(false);
    when(aiPromptRepository.findById(1L)).thenReturn(Optional.of(aiPrompt));
    LLMPromptService.deletePrompt(1L);
    verify(aiPromptRepository, times(1)).save(aiPrompt);
    assertTrue(aiPrompt.isDeleted());
  }

  @Test
  void testPromptDeletionError() {
    when(aiPromptRepository.findById(1L)).thenReturn(Optional.empty());
    AIException exception =
        assertThrows(AIException.class, () -> LLMPromptService.deletePrompt(1L));
    assertEquals("Prompt not found: 1", exception.getMessage());
  }

  @Test
  void testGetPrompt() {
    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setId(1L);
    aiPrompt.setUserPrompt("Check strings for spelling");
    aiPrompt.setModelName("gtp-3.5-turbo");
    aiPrompt.setPromptTemperature(0.0F);
    when(aiPromptRepository.findById(1L)).thenReturn(Optional.of(aiPrompt));
    AIPrompt openAIPrompt = LLMPromptService.getPrompt(1L);
    assertNotNull(openAIPrompt);
    assertEquals("Check strings for spelling", openAIPrompt.getUserPrompt());
    assertEquals("gtp-3.5-turbo", openAIPrompt.getModelName());
    assertEquals(0.0F, openAIPrompt.getPromptTemperature());
  }

  @Test
  void testAddPromptToRepository() {
    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setId(1L);
    Repository repository = new Repository();
    repository.setId(2L);
    AIPromptType promptType = new AIPromptType();
    promptType.setId(3L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(promptType);
    when(aiPromptRepository.findById(1L)).thenReturn(Optional.of(aiPrompt));
    LLMPromptService.addPromptToRepository(1L, "testRepo", "SOURCE_STRING_CHECKER");
    verify(aiPromptTypeRepository, times(1)).findByName("SOURCE_STRING_CHECKER");
    verify(repositoryAIPromptRepository, times(1)).save(repositoryAIPromptCaptor.capture());
    assertEquals(1L, repositoryAIPromptCaptor.getValue().getAiPromptId());
    assertEquals(2L, repositoryAIPromptCaptor.getValue().getRepositoryId());
    assertEquals(3L, repositoryAIPromptCaptor.getValue().getPromptTypeId());
  }
}
