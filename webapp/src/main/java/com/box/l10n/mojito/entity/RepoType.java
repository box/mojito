package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
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
 * <p>Names are unique. Extends {@link AuditableEntity} for {@code created_date} / {@code
 * last_modified_date} only; this entity is not Envers-audited.
 */
@Entity
@Table(
    name = "repo_type",
    indexes = {@Index(name = "UK__REPO_TYPE__NAME", columnList = "name", unique = true)})
public class RepoType extends AuditableEntity {

  public static final int NAME_MAX_LENGTH = 255;
  public static final int DESCRIPTION_MAX_LENGTH = 255;

  /**
   * Unique display name of the type (e.g. {@code React}). Required; max {@link #NAME_MAX_LENGTH}.
   */
  @Basic(optional = false)
  @Column(name = "name", length = NAME_MAX_LENGTH)
  @JsonView(View.IdAndName.class)
  private String name;

  /** Optional human-readable description of what this type is for. May be {@code null} or empty. */
  @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
  @JsonView(View.RepoType.class)
  private String description;

  /**
   * Shared type-layer AI prompt. Defaults to empty string on create. Empty means “no type-layer
   * instructions”; {@code null} should be normalized to empty when persisting.
   *
   * <p>Defaults to {@code null} so omitted JSON on PATCH deserializes as “leave unchanged”. Create
   * normalizes {@code null} to empty string before persist.
   */
  @Lob
  @Column(name = "ai_prompt", length = Integer.MAX_VALUE)
  @JsonView(View.RepoType.class)
  private String aiPrompt;

  /**
   * Integrity checkers owned by this type. Serialized in JSON as {@code integrityCheckers}: each
   * element is only {@code assetExtension} and {@code integrityCheckerType}. Parent FK and row ids
   * stay in the join table and are not part of the JSON contract.
   *
   * <p>Multiple checkers per asset extension are allowed; uniqueness is {@code (assetExtension,
   * integrityCheckerType)}. After load/create, never {@code null} — use an empty set when none are
   * configured. Field defaults to {@code null} so omitted JSON on PATCH means leave checkers
   * unchanged.
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "repo_type_integrity_checker",
      joinColumns =
          @JoinColumn(
              name = "repo_type_id",
              nullable = false,
              foreignKey = @ForeignKey(name = "FK__REPO_TYPE_INTEGRITY_CHECKER__REPO_TYPE__ID")))
  @JsonView(View.RepoType.class)
  private Set<RepoTypeIntegrityChecker> integrityCheckers;

  /**
   * @return unique name of this repo type
   */
  public String getName() {
    return name;
  }

  /**
   * @param name unique name; must be non-null and unique among all repo types
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return description, or {@code null} if unset
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description optional description; {@code null} and empty are both allowed
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return type-layer AI prompt; never expected to be {@code null} after create (defaults to "")
   */
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
   * @return integrity checkers, or {@code null} when the field was never set (omitted JSON on PATCH
   *     means leave checkers unchanged). After load/create this is a set, possibly empty.
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
