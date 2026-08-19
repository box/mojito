package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.RepoType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * HTTP client for the {@code /api/repo-types} endpoints.
 *
 * <p>Used by the CLI and integration tests. Base path is {@code /api/repo-types} via {@link
 * #getEntityName()}.
 */
@Component
public class RepoTypeClient extends BaseClient {

  static Logger logger = LoggerFactory.getLogger(RepoTypeClient.class);

  /**
   * @return path segment {@code repo-types} (full base path {@code /api/repo-types})
   */
  @Override
  public String getEntityName() {
    return "repo-types";
  }

  /**
   * GET {@code /api/repo-types/{id}}.
   *
   * @param repoTypeId type id
   * @return the type including integrity checkers
   * @throws org.springframework.web.client.HttpClientErrorException.NotFound if the id does not
   *     exist
   */
  public RepoType getRepoTypeById(Long repoTypeId) {
    logger.debug("Getting repo type by id = [{}]", repoTypeId);
    return authenticatedRestTemplate.getForObject(
        getBasePathForResource(repoTypeId), RepoType.class);
  }

  /**
   * GET {@code /api/repo-types}, optionally with {@code ?name=}.
   *
   * @param name exact name filter, or {@code null} for all types
   * @return matching types; empty list if none (never {@code null})
   */
  public List<RepoType> getRepoTypes(String name) {
    Map<String, String> filterParams = new HashMap<>();
    if (name != null) {
      filterParams.put("name", name);
    }
    return authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(
        getBasePathForEntity(), RepoType[].class, filterParams);
  }

  /**
   * POST {@code /api/repo-types}.
   *
   * @param repoType type to create (must include {@code name})
   * @return created type with id
   * @throws org.springframework.web.client.HttpClientErrorException.Conflict on duplicate name
   */
  public RepoType createRepoType(RepoType repoType) {
    logger.debug("Creating repo type with name = {}", repoType.getName());
    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity(), repoType, RepoType.class);
  }

  /**
   * PATCH {@code /api/repo-types/{id}}.
   *
   * @param repoTypeId id of the type to update
   * @param repoType fields to change
   * @return updated type
   * @throws org.springframework.web.client.HttpClientErrorException.NotFound if missing
   * @throws org.springframework.web.client.HttpClientErrorException.Conflict on name conflict
   */
  public RepoType updateRepoType(Long repoTypeId, RepoType repoType) {
    logger.debug("Updating repo type id = [{}]", repoTypeId);
    ResponseEntity<RepoType> response =
        authenticatedRestTemplate
            .getRestTemplate()
            .exchange(
                authenticatedRestTemplate.getURIForResource(getBasePathForResource(repoTypeId)),
                HttpMethod.PATCH,
                new HttpEntity<>(repoType),
                RepoType.class);
    return response.getBody();
  }

  /**
   * DELETE {@code /api/repo-types/{id}}.
   *
   * @param repoTypeId id of the type to delete
   * @throws org.springframework.web.client.HttpClientErrorException.NotFound if missing
   */
  public void deleteRepoType(Long repoTypeId) {
    logger.debug("Deleting repo type id = [{}]", repoTypeId);
    authenticatedRestTemplate.delete(getBasePathForResource(repoTypeId));
  }
}
