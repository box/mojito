package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;

/**
 * REST client DTO mirroring the server {@link com.box.l10n.mojito.entity.RepoType} JSON shape.
 *
 * <p>Used by {@link com.box.l10n.mojito.rest.client.RepoTypeClient} (CLI and tests). Field names and
 * the {@code integrityCheckers} array must stay aligned with the server API.
 */
public class RepoType {

  private Long id;
  private String name;
  private String description;
  private String aiPrompt;

  /**
   * Integrity checkers for this type. JSON property name is {@code integrityCheckers} (same shape
   * as server: {@code id}, {@code assetExtension}, {@code integrityCheckerType}).
   */
  @JsonManagedReference("integrityCheckers")
  @JsonProperty("integrityCheckers")
  private Set<RepoTypeIntegrityChecker> integrityCheckers = new HashSet<>();

  /** @return server-assigned id, or {@code null} before create */
  public Long getId() {
    return id;
  }

  /** @param id server-assigned id */
  public void setId(Long id) {
    this.id = id;
  }

  /** @return unique name */
  public String getName() {
    return name;
  }

  /** @param name unique name required on create */
  public void setName(String name) {
    this.name = name;
  }

  /** @return optional description */
  public String getDescription() {
    return description;
  }

  /** @param description optional description */
  public void setDescription(String description) {
    this.description = description;
  }

  /** @return shared type-layer AI prompt */
  public String getAiPrompt() {
    return aiPrompt;
  }

  /** @param aiPrompt type-layer prompt; empty string clears it */
  public void setAiPrompt(String aiPrompt) {
    this.aiPrompt = aiPrompt;
  }

  /** @return integrity checkers; never {@code null} after construction */
  public Set<RepoTypeIntegrityChecker> getIntegrityCheckers() {
    return integrityCheckers;
  }

  /** @param integrityCheckers full checker set to send on create/update */
  public void setIntegrityCheckers(Set<RepoTypeIntegrityChecker> integrityCheckers) {
    this.integrityCheckers = integrityCheckers;
  }
}
