package com.box.l10n.mojito.service.cli;

import com.box.l10n.mojito.mustache.MustacheBaseContext;

import java.net.URL;

public class InstallCliContext extends MustacheBaseContext {

    String installDirectory;
    String scheme;
    String host;
    String port;

    public InstallCliContext(String installDirectory, String scheme, String host, String port) {
        this.installDirectory = installDirectory;
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }
}
