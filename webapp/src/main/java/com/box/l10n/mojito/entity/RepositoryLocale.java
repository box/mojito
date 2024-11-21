package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
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
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * Entity that describes a locale associated to a repository.
 *
 * @author aloison
 */
@Entity
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Table(
    name = "repository_locale",
    indexes = {
      @Index(
          name = "UK__REPOSITORY_LOCALE__REPOSITORY_ID__LOCALE_ID",
          columnList = "repository_id, locale_id",
          unique = true)
    })
@NamedEntityGraph(
    name = "RepositoryLocale.legacy",
    attributeNodes = {
      @NamedAttributeNode("repository"),
      @NamedAttributeNode("locale"),
      @NamedAttributeNode(value = "parentLocale", subgraph = "RepositoryLocale.legacy.parentLocale")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "RepositoryLocale.legacy.parentLocale",
          attributeNodes = {@NamedAttributeNode("locale"), @NamedAttributeNode("parentLocale")})
    })
public class RepositoryLocale extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference("repositoryLocales")
  @Schema(hidden = true)
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__REPOSITORY_LOCALE__REPOSITORY__ID"),
      nullable = false)
  private Repository repository;

  @JsonView(View.LocaleSummary.class)
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__REPOSITORY_LOCALE__LOCALE__ID"),
      nullable = false)
  private Locale locale;

  /**
   * "Fully translated" means that ALL the strings for the locale will need to be translated by
   * translators to avoid missing translations.
   *
   * <p>If this value is set to false, the strings resolution may use locale inheritance or any
   * other technique to fill the missing translations. Use this for locales that need minor
   * adjustments.
   *
   * <p>For the root locale this is set to {@code false} as we won't send it for translation.
   */
  @JsonView(View.LocaleSummary.class)
  @Column(name = "to_be_fully_translated")
  private boolean toBeFullyTranslated = true;

  /**
   * Attribute used to specify that {@link RepositoryLocale} inherits form
   * another {@link RepositoryLocale}.
   *
   * <p>
   * If {@code null} it means that this {@link RepositoryLocale} represent the
   * root locale of the repository. The root locale is unique, hence, only a
   * single {@link RepositoryLocale} can have this attribute set to
   * {@code null} per repository.
   *
   * <p>
   * <code>
   * This is used to defined inheritance chain like:
   *
   *                  en (root)
   *                 /  \
   *               fr    ja-JP
   *              /  \
   *         fr-FR   fr-CA
   * <code>
   */
  @JsonView(View.RepositorySummary.class)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "parent_locale",
      foreignKey = @ForeignKey(name = "FK__REPOSITORY_LOCALE__PARENT_LOCALE__ID"))
  private RepositoryLocale parentLocale;

  @JsonIgnore
  @Override
  public Long getId() {
    return super.getId();
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public boolean isToBeFullyTranslated() {
    return toBeFullyTranslated;
  }

  public void setToBeFullyTranslated(boolean toBeFullyTranslated) {
    this.toBeFullyTranslated = toBeFullyTranslated;
  }

  public RepositoryLocale getParentLocale() {
    return parentLocale;
  }

  public void setParentLocale(RepositoryLocale parentLocale) {
    this.parentLocale = parentLocale;
  }

  public RepositoryLocale() {}

  public RepositoryLocale(
      Repository repository,
      Locale locale,
      boolean toBeFullyTranslated,
      RepositoryLocale parentLocale) {
    this.repository = repository;
    this.locale = locale;
    this.toBeFullyTranslated = toBeFullyTranslated;
    this.parentLocale = parentLocale;
  }
}
