package com.box.l10n.mojito.apiclient.model;

/**
 * Mirrors: com.box.l10n.mojito.security.Role
 *
 * @author jyi
 */
public enum Role {
  ROLE_PM,
  ROLE_TRANSLATOR,
  ROLE_ADMIN,
  ROLE_USER;

  public static Role fromString(String roleName) {
    if (roleName == null || roleName.isBlank()) {
      return null;
    }

    if (!roleName.startsWith("ROLE_")) {
      roleName = "ROLE_" + roleName;
    }
    return Role.valueOf(roleName);
  }
}
