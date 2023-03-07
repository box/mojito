package com.box.l10n.mojito.cli.command.utils;

public class DiffInfoUtils {

  public static String getUsernameForAuthorEmail(String email) {
    String username = null;

    if (email != null) {
      username = email.replaceFirst("(.*)@.*$", "$1");
    }

    return username;
  }
}
