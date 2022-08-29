package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

/** @author wyau */
public abstract class BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(BaseClient.class);

  @Autowired AuthenticatedRestTemplate authenticatedRestTemplate;

  String basePath = "/api";

  /**
   * Gets the base path
   *
   * @return
   */
  public String getBasePath() {
    return basePath;
  }

  /**
   * Sets the base path
   *
   * @param basePath
   */
  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  /**
   * Override this to have a custom entity name
   *
   * @return
   */
  public abstract String getEntityName();

  /**
   * Construct a base path related to this entity
   *
   * @return
   */
  protected String getBasePathForEntity() {
    return getBasePath() + "/" + getEntityName();
  }

  /**
   * Construct a base path for a resource matching the {@param resourceId}
   *
   * @param resourceId The resourceId of the resource
   * @return
   */
  protected String getBasePathForResource(Long resourceId) {
    return getBasePathForEntity() + "/" + resourceId;
  }

  /**
   * Construct a base path for a resource and subresources matching the {@param resourceId}
   *
   * <p>Example 1:
   *
   * <pre class="code">
   * getBasePathForResource(repoId, "repositoryLocales");
   * </pre>
   *
   * will print:
   *
   * <blockquote>
   *
   * {@code /api/entityName/repoId/repositoryLocales/}
   *
   * </blockquote>
   *
   * Example 2:
   *
   * <pre class="code">
   * getBasePathForResource(repoId, "repositoryLocales", 1);
   * </pre>
   *
   * will print:
   *
   * <blockquote>
   *
   * {@code /api/entityName/repoId/repositoryLocales/1}
   *
   * </blockquote>
   *
   * @param resourceId The resourceId of the resource
   * @param pathSegments An undefined number of path segments to concatenate to the URI
   * @return
   */
  protected String getBasePathForResource(Long resourceId, Object... pathSegments) {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromPath(getBasePathForResource(resourceId));

    if (!ObjectUtils.isEmpty(pathSegments)) {
      for (Object pathSegment : pathSegments) {
        uriBuilder.pathSegment(pathSegment.toString());
      }
    }

    return uriBuilder.toUriString();
  }
}
