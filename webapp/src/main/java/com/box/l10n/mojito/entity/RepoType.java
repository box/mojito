package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared configuration for repositories of the same kind (e.g. React, Android).
 *
 * <p>A repo type holds settings that apply to every repository assigned to it:
 *
 * <ul>
 *   <li>{@link #aiPrompt} — one type-layer AI prompt used for both translation and review (stack
 *       rules such as placeholders, ICU plurals/selects, markup)
 *   <li>{@link #integrityCheckers} — integrity checkers that will later be unioned with
 *       repository-level checkers when a push or import runs
 * </ul>
 *
 * <p>Names are unique. Extends {@link AuditableEntity} for {@code created_date} /
 * {@code last_modified_date} only; this entity is not Envers-audited.
 */
@Entity
@Table(
    name = "repo_type",
    indexes = {@Index(name = "UK__REPO_TYPE__NAME", columnList = "name", unique = true)})
public class RepoType extends AuditableEntity {

  public static final int NAME_MAX_LENGTH = 255;

  /** Unique display name of the type (e.g. {@code React}). Required; max {@link #NAME_MAX_LENGTH}. */
  @Basic(optional = false)
  @Column(name = "name", length = NAME_MAX_LENGTH)
  @JsonView(View.IdAndName.class)
  private String name;

  /** Optional human-readable description of what this type is for. May be {@code null} or empty. */
  @Column(name = "description")
  @JsonView(View.IdAndName.class)
  private String description;

  /**
   * Shared type-layer AI prompt. Defaults to empty string on create. Empty means “no type-layer
   * instructions”; {@code null} should be normalized to empty when persisting.
   */
  @Lob
  @Column(name = "ai_prompt", length = Integer.MAX_VALUE)
  @JsonView(View.IdAndName.class)
  private String aiPrompt = "";

  /**
   * Integrity checkers owned by this type. Serialized in JSON as {@code integrityCheckers}.
   *
   * <p>Multiple checkers per asset extension are allowed; the logical key is {@code
   * (assetExtension, integrityCheckerType)}. Never {@code null} — use an empty set when none are
   * configured.
   */
  @JsonManagedReference("integrityCheckers")
  @OneToMany(mappedBy = "repoType", fetch = FetchType.EAGER)
  @JsonView(View.IdAndName.class)
  private Set<RepoTypeIntegrityChecker> integrityCheckers = new HashSet<>();

  /** @return unique name of this repo type */
  public String getName() {
    return name;
  }

  /** @param name unique name; must be non-null and unique among all repo types */
  public void setName(String name) {
    this.name = name;
  }

  /** @return description, or {@code null} if unset */
  public String getDescription() {
    return description;
  }

  /** @param description optional description; {@code null} and empty are both allowed */
  public void setDescription(String description) {
    this.description = description;
  }

  /** @return type-layer AI prompt; never expected to be {@code null} after create (defaults to "") */
  public String getAiPrompt() {
    return aiPrompt;
  }

  /**
   * @param aiPrompt type-layer prompt; prefer empty string over {@code null} when clearing
   */
  public void setAiPrompt(String aiPrompt) {
    this.aiPrompt = aiPrompt;
  }

  /**
   * @return integrity checkers for this type; never {@code null} (may be empty)
   */
  public Set<RepoTypeIntegrityChecker> getIntegrityCheckers() {
    return integrityCheckers;
  }

  /**
   * @param integrityCheckers replacement set; {@code null} should be treated as empty by callers
   *     that persist this field
   */
  public void setIntegrityCheckers(Set<RepoTypeIntegrityChecker> integrityCheckers) {
    this.integrityCheckers = integrityCheckers;
  }
}
