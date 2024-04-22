package com.box.l10n.mojito.rest.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.WSTestDataFactory;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException;
import com.box.l10n.mojito.rest.client.exception.RestClientException;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author wyau
 */
public class RepositoryWSTest extends WSTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryWSTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired RepositoryService repositoryService;

  @Autowired WSTestDataFactory wsTestDataFactory;

  @Autowired RepositoryClient repositoryClient;

  @Autowired LocaleService localeService;

  @Autowired RepositoryRepository repositoryRepository;

  @Test
  public void testGetRepositoryByName()
      throws RestClientException, RepositoryNameAlreadyUsedException {
    Repository expectedRepository = wsTestDataFactory.createRepository(testIdWatcher);
    com.box.l10n.mojito.rest.entity.Repository actualRepository =
        repositoryClient.getRepositoryByName(expectedRepository.getName());

    assertRepositoriesAreEqual(expectedRepository, actualRepository);
  }

  @Test
  public void testGetRepositoryById()
      throws RepositoryNotFoundException, RepositoryNameAlreadyUsedException {
    Repository expectedRepository = wsTestDataFactory.createRepository(testIdWatcher);

    final Optional<Repository> byId = repositoryRepository.findById(expectedRepository.getId());
    System.out.println(byId.get().getName());

    com.box.l10n.mojito.rest.entity.Repository actualRepository =
        repositoryClient.getRepositoryById(expectedRepository.getId());

    assertRepositoriesAreEqual(expectedRepository, actualRepository);
  }

  @Test
  public void testGetRepositoryByIdMissing() {
    try {
      repositoryClient.getRepositoryById(123456789L);
      fail("HTTP error 404 is expected");
    } catch (HttpClientErrorException httpClientErrorException) {
      assertEquals(404, httpClientErrorException.getRawStatusCode());
      assertTrue(
          "The error must have a body that contains a message with details",
          httpClientErrorException
              .getResponseBodyAsString()
              .contains("\"message\":\"Repository with id: 123456789 not found\""));
    }
  }

  @Test
  public void testDeleteRepositoryById()
      throws RepositoryNotFoundException, RepositoryNameAlreadyUsedException {
    Repository expectedRepository = wsTestDataFactory.createRepository(testIdWatcher);
    com.box.l10n.mojito.rest.entity.Repository actualRepository =
        repositoryClient.getRepositoryById(expectedRepository.getId());
    assertRepositoriesAreEqual(expectedRepository, actualRepository);

    repositoryClient.deleteRepositoryByName(expectedRepository.getName());
    actualRepository = repositoryClient.getRepositoryById(expectedRepository.getId());
    assertTrue(actualRepository.getDeleted());
    assertTrue(actualRepository.getName().startsWith("deleted__"));
  }

  @Test
  public void testUpdateRepositoryNameAndDescription()
      throws RepositoryNotFoundException,
          ResourceNotCreatedException,
          RepositoryNameAlreadyUsedException,
          ResourceNotUpdatedException {
    Repository expectedRepository = wsTestDataFactory.createRepository(testIdWatcher);

    String newName = expectedRepository.getName() + "_updated";
    repositoryClient.updateRepository(
        expectedRepository.getName(), newName, null, null, null, null);
    com.box.l10n.mojito.rest.entity.Repository actualRepository =
        repositoryClient.getRepositoryById(expectedRepository.getId());
    assertEquals(
        "Id should have remained the same", expectedRepository.getId(), actualRepository.getId());
    assertEquals("Name should have been updated", newName, actualRepository.getName());
    assertEquals(
        "Description should not be updated yet",
        expectedRepository.getDescription(),
        actualRepository.getDescription());
    assertEquals(
        "Repository locales should not have changed",
        expectedRepository.getRepositoryLocales().size(),
        actualRepository.getRepositoryLocales().size());

    String newDescription = newName + "_description";
    repositoryClient.updateRepository(newName, null, newDescription, null, null, null);
    actualRepository = repositoryClient.getRepositoryById(expectedRepository.getId());
    assertEquals(
        "Id should have remained the same", expectedRepository.getId(), actualRepository.getId());
    assertEquals("Name should have been updated", newName, actualRepository.getName());
    assertEquals(
        "Description should have updated", newDescription, actualRepository.getDescription());
    assertEquals(
        "Repository locales should not have changed",
        expectedRepository.getRepositoryLocales().size(),
        actualRepository.getRepositoryLocales().size());
  }

  @Test
  public void testUpdateRepositoryLocales()
      throws RepositoryNotFoundException,
          RepositoryNameAlreadyUsedException,
          ResourceNotUpdatedException {
    Repository expectedRepository = wsTestDataFactory.createRepository(testIdWatcher);

    Set<RepositoryLocale> repositoryLocales = new HashSet<>();
    repositoryLocales = getRepositoryLocales(Arrays.asList("de-DE"));
    repositoryClient.updateRepository(
        expectedRepository.getName(), null, null, null, repositoryLocales, null);
    com.box.l10n.mojito.rest.entity.Repository actualRepository =
        repositoryClient.getRepositoryById(expectedRepository.getId());
    assertEquals(
        "Id should have remained the same", expectedRepository.getId(), actualRepository.getId());
    assertEquals(
        "Name should have remained the same",
        expectedRepository.getName(),
        actualRepository.getName());
    assertEquals(
        "Description should have remained the same",
        expectedRepository.getDescription(),
        actualRepository.getDescription());
    assertEquals(
        "Should only have de-DE and one root locale",
        2,
        actualRepository.getRepositoryLocales().size());
    for (RepositoryLocale repositoryLocale : actualRepository.getRepositoryLocales()) {
      assertTrue(
          "en".equals(repositoryLocale.getLocale().getBcp47Tag())
              || "de-DE".equals(repositoryLocale.getLocale().getBcp47Tag()));
    }

    repositoryLocales = getRepositoryLocales(Arrays.asList("fr-FR", "ko-KR", "ja-JP", "es-ES"));
    repositoryClient.updateRepository(
        expectedRepository.getName(), null, null, null, repositoryLocales, null);
    actualRepository = repositoryClient.getRepositoryById(expectedRepository.getId());
    assertEquals(
        "Id should have remained the same", expectedRepository.getId(), actualRepository.getId());
    assertEquals(
        "Name should have remained the same",
        expectedRepository.getName(),
        actualRepository.getName());
    assertEquals(
        "Description should have remained the same",
        expectedRepository.getDescription(),
        actualRepository.getDescription());
    assertEquals(
        "Should only have five locales including the roolt locale",
        5,
        actualRepository.getRepositoryLocales().size());
    for (RepositoryLocale repositoryLocale : actualRepository.getRepositoryLocales()) {
      assertFalse(
          "de-DE should have been deleted",
          "de-DE".equals(repositoryLocale.getLocale().getBcp47Tag()));
    }
  }

  protected void assertRepositoriesAreEqual(
      Repository expectedRepository, com.box.l10n.mojito.rest.entity.Repository actualRepository) {
    logger.debug("Basic asserts");
    assertEquals(expectedRepository.getName(), actualRepository.getName());
    assertEquals(expectedRepository.getDescription(), actualRepository.getDescription());
    assertTrue(actualRepository.getRepositoryLocales().size() > 0);
    assertEquals(
        expectedRepository.getRepositoryLocales().size(),
        actualRepository.getRepositoryLocales().size());

    ArrayList<Locale> actualLocales = new ArrayList<>();
    for (com.box.l10n.mojito.rest.entity.RepositoryLocale repositoryLocale :
        actualRepository.getRepositoryLocales()) {
      actualLocales.add(repositoryLocale.getLocale());
    }

    logger.debug("Asserting repositoryLocale list is the same");
    ArrayList<com.box.l10n.mojito.entity.Locale> expectedLocales = new ArrayList<>();
    for (com.box.l10n.mojito.entity.RepositoryLocale repositoryLocale :
        expectedRepository.getRepositoryLocales()) {
      expectedLocales.add(repositoryLocale.getLocale());
    }

    assertTrue(actualLocales.size() > 0);
    assertEquals(expectedLocales.size(), actualLocales.size());

    logger.debug("Asserting actual locale is found in the list of expected locale");
    for (com.box.l10n.mojito.entity.Locale expectedLocale : expectedLocales) {
      Boolean expectedLocaleFound = false;
      for (Locale actualLocale : actualLocales) {
        if (expectedLocale.getId().equals(actualLocale.getId())
            && expectedLocale.getBcp47Tag().equals(actualLocale.getBcp47Tag())) {
          expectedLocaleFound = true;
        }
      }

      assertTrue(expectedLocaleFound);
    }
  }
}
