package com.box.l10n.mojito.service.cli;

import com.box.l10n.mojito.mustache.MustacheBaseContext;
import java.util.Collections;
import java.util.List;

public class InstallCliContext extends MustacheBaseContext {

  String installDirectory;
  String scheme;
  String host;
  String port;
  String authenticationMode;
  boolean hasHeaders;
  List<Header> headers = Collections.emptyList();

  public InstallCliContext(String installDirectory, String scheme, String host, String port) {
    this.installDirectory = installDirectory;
    this.scheme = scheme;
    this.host = host;
    this.port = port;
    this.hasHeaders = false;
  }

  public record Header(
      String name, String envVar, String envVarBraced, String envVarPresenceCheck) {

    public Header(String name, String envVar) {
      this(name, envVar, "${" + envVar + "}", "${" + envVar + ":-}");
    }
  }
}
