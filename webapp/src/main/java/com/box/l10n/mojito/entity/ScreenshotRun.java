package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

/**
 * Used to group together screenshots for example when coming from the same test suite execution.
 *
 * @author jaurambault
 */
@Entity
@Table(
    name = "screenshot_run",
    indexes = {@Index(name = "UK__SCREENSHOT_RUN__NAME", columnList = "name", unique = true)})
@NamedEntityGraph(
    name = "ScreenshotRunGraph",
    attributeNodes = {@NamedAttributeNode("screenshots")})
@BatchSize(size = 1000)
public class ScreenshotRun extends SettableAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__SCREENSHOT_RUN__REPOSITORY__ID"),
      nullable = false)
  @JsonBackReference
  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
  private Repository repository;

  @Column(name = "name")
  private String name;

  @JsonDeserialize(as = LinkedHashSet.class)
  @OneToMany(mappedBy = "screenshotRun", fetch = FetchType.LAZY)
  Set<Screenshot> screenshots = new LinkedHashSet<>();

  @Column(name = "lastSuccessfulRun")
  Boolean lastSuccessfulRun = false;

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<Screenshot> getScreenshots() {
    return screenshots;
  }

  public void setScreenshots(Set<Screenshot> screenshots) {
    this.screenshots = screenshots;
  }

  public Boolean getLastSuccessfulRun() {
    return lastSuccessfulRun;
  }

  public void setLastSuccessfulRun(Boolean lastSuccessfulRun) {
    this.lastSuccessfulRun = lastSuccessfulRun;
  }
}
