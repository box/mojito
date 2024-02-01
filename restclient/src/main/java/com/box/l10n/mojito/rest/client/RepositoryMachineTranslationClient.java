package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.RepositoryMachineTranslationBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author jaurambault
 */
@Component
public class RepositoryMachineTranslationClient extends BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryMachineTranslationClient.class);

  @Override
  public String getEntityName() {
    return "machine-translation";
  }

  /**
   * Machine translate untranslated strings in a repository for a given list of locales
   *
   * @param repositoryMachineTranslationBody
   * @return {@link RepositoryMachineTranslationBody}
   */
  public RepositoryMachineTranslationBody translateRepository(
      RepositoryMachineTranslationBody repositoryMachineTranslationBody) {

    String translateRepositoryPath =
        UriComponentsBuilder.fromPath(getBasePathForEntity())
            .pathSegment("repository")
            .toUriString();

    return authenticatedRestTemplate.postForObject(
        translateRepositoryPath,
        repositoryMachineTranslationBody,
        RepositoryMachineTranslationBody.class);
  }
}
