package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.RepoType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * GET {@code /api/repo-types}, optionally with {@code ?name=}.
   *
   * @param name exact name filter, or {@code null} for all types
   * @return matching types; empty list if none (never {@code null})
   */
  public List<RepoType> getRepoTypes(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * POST {@code /api/repo-types}.
   *
   * @param repoType type to create (must include {@code name})
   * @return created type with id
   * @throws org.springframework.web.client.HttpClientErrorException.Conflict on duplicate name
   */
  public RepoType createRepoType(RepoType repoType) {
    throw new UnsupportedOperationException("Not implemented");
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
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * DELETE {@code /api/repo-types/{id}}.
   *
   * @param repoTypeId id of the type to delete
   * @throws org.springframework.web.client.HttpClientErrorException.NotFound if missing
   */
  public void deleteRepoType(Long repoTypeId) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
