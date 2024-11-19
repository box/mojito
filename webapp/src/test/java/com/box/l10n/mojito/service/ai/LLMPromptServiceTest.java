package com.box.l10n.mojito.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.rest.ai.AIPromptCreateRequest;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  @Mock RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Mock RepositoryRepository repositoryRepository;

  @Mock LocaleRepository localeRepository;

  @Captor ArgumentCaptor<RepositoryLocaleAIPrompt> repositoryAIPromptCaptor;

  @Captor ArgumentCaptor<Collection<RepositoryLocaleAIPrompt>> repositoryAIPromptCollectionCaptor;

  @InjectMocks LLMPromptService LLMPromptService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testPromptCreation() {
    Repository repository = new Repository();
    repository.setId(1L);
    RepositoryLocaleAIPrompt repositoryLocaleAIPrompt = new RepositoryLocaleAIPrompt();
    repositoryLocaleAIPrompt.setId(1L);
    AIPromptType promptType = new AIPromptType();
    promptType.setId(1L);
    promptType.setName("SOURCE_STRING_CHECKER");
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
    when(repositoryLocaleAIPromptRepository.save(any())).thenReturn(repositoryLocaleAIPrompt);

    LLMPromptService.createPrompt(AIPromptCreateRequest);

    verify(aiPromptTypeRepository, times(1)).findByName("SOURCE_STRING_CHECKER");
    verify(aiPromptRepository, times(1)).save(any());
    verify(repositoryLocaleAIPromptRepository, times(1)).save(any());
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
    when(repositoryLocaleAIPromptRepository.save(any())).thenReturn(1L);

    AIException exception =
        assertThrows(AIException.class, () -> LLMPromptService.createPrompt(AIPromptCreateRequest));
    assertEquals("Prompt type not found: SOURCE_STRING_CHECKER", exception.getMessage());

    verify(aiPromptTypeRepository, times(1)).findByName("SOURCE_STRING_CHECKER");
    verify(aiPromptTypeRepository, times(0)).save(any());
    verify(repositoryLocaleAIPromptRepository, times(0)).save(any());
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
    verify(repositoryLocaleAIPromptRepository, times(1)).save(repositoryAIPromptCaptor.capture());
    assertEquals(1L, repositoryAIPromptCaptor.getValue().getAiPrompt().getId());
    assertEquals(2L, repositoryAIPromptCaptor.getValue().getRepository().getId());
  }

  @Test
  void testCreateAiPromptOverride() {
    Repository repository = new Repository();
    repository.setId(1L);
    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setId(1L);
    Locale frLocale = new Locale();
    frLocale.setId(1L);
    frLocale.setBcp47Tag("fr-FR");
    Locale deLocale = new Locale();
    deLocale.setId(2L);
    deLocale.setBcp47Tag("de-DE");
    RepositoryLocale repositoryLocaleFr = new RepositoryLocale();
    repositoryLocaleFr.setLocale(frLocale);
    RepositoryLocale repositoryLocaleDe = new RepositoryLocale();
    repositoryLocaleDe.setLocale(deLocale);
    repository.setRepositoryLocales(Set.of(repositoryLocaleFr, repositoryLocaleDe));
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(aiPromptRepository.findById(1L)).thenReturn(Optional.of(aiPrompt));
    when(localeRepository.findByBcp47Tag("fr-FR")).thenReturn(frLocale);
    when(localeRepository.findByBcp47Tag("de-DE")).thenReturn(deLocale);

    LLMPromptService.createOrUpdateRepositoryLocaleTranslationPromptOverrides(
        "testRepo", Set.of("fr-FR", "de-DE"), 1L, true);

    verify(repositoryLocaleAIPromptRepository, times(2)).save(repositoryAIPromptCaptor.capture());
    assertThat(repositoryAIPromptCaptor.getAllValues())
        .extracting(RepositoryLocaleAIPrompt::getAiPrompt)
        .extracting(AIPrompt::getId)
        .containsExactlyInAnyOrder(1L, 1L);
    assertThat(repositoryAIPromptCaptor.getAllValues())
        .extracting(RepositoryLocaleAIPrompt::getLocale)
        .extracting(Locale::getId)
        .containsExactlyInAnyOrder(1L, 2L);
    assertThat(repositoryAIPromptCaptor.getAllValues())
        .extracting(RepositoryLocaleAIPrompt::isDisabled)
        .containsExactlyInAnyOrder(true, true);
  }

  @Test
  void testDeleteLocaleOverrides() {
    Repository repository = new Repository();
    repository.setId(1L);
    Locale frLocale = new Locale();
    frLocale.setId(1L);
    frLocale.setBcp47Tag("fr-FR");
    Locale deLocale = new Locale();
    deLocale.setId(2L);
    deLocale.setBcp47Tag("de-DE");
    Locale itLocale = new Locale();
    itLocale.setId(3L);
    itLocale.setBcp47Tag("it-IT");
    RepositoryLocale repositoryLocaleFr = new RepositoryLocale();
    repositoryLocaleFr.setLocale(frLocale);
    RepositoryLocale repositoryLocaleDe = new RepositoryLocale();
    repositoryLocaleDe.setLocale(deLocale);
    RepositoryLocale repositoryLocaleIt = new RepositoryLocale();
    repositoryLocaleIt.setLocale(itLocale);
    repository.setRepositoryLocales(
        Set.of(repositoryLocaleFr, repositoryLocaleDe, repositoryLocaleIt));
    RepositoryLocaleAIPrompt repositoryLocaleAIPromptFr = new RepositoryLocaleAIPrompt();
    repositoryLocaleAIPromptFr.setId(1L);
    repositoryLocaleAIPromptFr.setRepository(repository);
    repositoryLocaleAIPromptFr.setLocale(frLocale);
    RepositoryLocaleAIPrompt repositoryLocaleAIPromptDe = new RepositoryLocaleAIPrompt();
    repositoryLocaleAIPromptDe.setId(2L);
    repositoryLocaleAIPromptDe.setRepository(repository);
    repositoryLocaleAIPromptDe.setLocale(deLocale);
    RepositoryLocaleAIPrompt repositoryLocaleAIPromptIt = new RepositoryLocaleAIPrompt();
    repositoryLocaleAIPromptIt.setId(3L);
    repositoryLocaleAIPromptIt.setRepository(repository);
    repositoryLocaleAIPromptIt.setLocale(itLocale);

    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(repositoryLocaleAIPromptRepository.getRepositoryLocaleTranslationPromptOverrides(
            repository))
        .thenReturn(
            Optional.of(
                List.of(
                    repositoryLocaleAIPromptDe,
                    repositoryLocaleAIPromptFr,
                    repositoryLocaleAIPromptIt)));

    LLMPromptService.deleteRepositoryLocaleTranslationPromptOverride(
        "testRepo", Set.of("fr-FR", "de-DE"));

    verify(repositoryLocaleAIPromptRepository, times(1))
        .deleteAll(repositoryAIPromptCollectionCaptor.capture());
    assertThat(repositoryAIPromptCollectionCaptor.getValue())
        .extracting(RepositoryLocaleAIPrompt::getId)
        .containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  void testAiExceptionThrownIfLocalesNotConfiguredForRepositoryLocalePromptOverride() {
    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setId(1L);
    Repository repository = new Repository();
    repository.setId(2L);
    AIPromptType promptType = new AIPromptType();
    promptType.setId(3L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    assertThrows(
        AIException.class,
        () ->
            LLMPromptService.deleteRepositoryLocaleTranslationPromptOverride(
                "testRepo", Set.of("fr-FR", "de-DE")));
  }
}
