package com.box.l10n.mojito.service.repotype;

/**
 * Thrown when creating or renaming a {@link com.box.l10n.mojito.entity.RepoType} would violate the
 * unique name constraint.
 *
 * <p>Mapped by the REST layer to HTTP 409 Conflict. {@link #getName()} is the trimmed name that was
 * actually checked/persisted.
 */
public class RepoTypeNameAlreadyUsedException extends Exception {

  private final String name;

  /**
   * @param name trimmed name that conflicted
   */
  public RepoTypeNameAlreadyUsedException(String name) {
    super(name + " is used by another repo type");
    this.name = name;
  }

  /**
   * @return trimmed name that conflicted
   */
  public String getName() {
    return name;
  }
}
