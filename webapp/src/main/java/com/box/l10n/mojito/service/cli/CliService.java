package com.box.l10n.mojito.service.cli;

import com.box.l10n.mojito.mustache.MustacheTemplateEngine;
import com.box.l10n.mojito.rest.cli.GitInfo;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author jeanaurambault
 */
@Component
public class CliService {

    static final String INSTALL_CLI_TEMPLATE = "cli/install.sh";
    static Logger logger = LoggerFactory.getLogger(CliService.class);

    @Value("${info.build.version}")
    String version;

    @Autowired
    @Qualifier("GitInfoWebapp")
    GitInfo gitInfo;

    @Autowired
    CliConfig cliConfig;

    @Autowired
    MustacheTemplateEngine mustacheTemplateEngine;

    /**
     * Get the local CLI file (referenced in "cli.file" property) if it exists.
     */
    public Optional<FileSystemResource> getLocalCliFile() {
        Optional<FileSystemResource> fileSystemResource = Optional.empty();

        Path cliPath = Paths.get(cliConfig.getFile());

        if (Files.exists(cliPath)) {
            fileSystemResource = Optional.of(new FileSystemResource(cliPath.toFile()));
        }

        return fileSystemResource;
    }

    /**
     * Get the URL where the CLI can be downloaded.
     * <p>
     * The URL is built out of the "cli.url" property which can be a template with following placeholder: {version},
     * {gitCommit} and {gitCommitShort}
     * <p>
     * By default Github release is referenced (no local file), so that it works for standard releases process.
     */
    public String getCliUrl() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("version", version);
        arguments.put("gitCommit", gitInfo.getCommit().getId());
        arguments.put("gitShortCommit", gitInfo.getCommit().getId().substring(0, 7));

        String format = MessageFormat.format(cliConfig.getUrl(), arguments);
        return format;
    }

    /**
     * Generates a bash script to install the CLI.
     * <p>
     * The install process download the jar file from the server and create a bash wrapper with proper settings to
     * talk to the server.
     *
     * @param requestUrl       the URL of the server that will be accessed
     * @param installDirectory the directory where the CLI and wrapper should be installed
     * @return the bash script
     * @throws IOException
     */
    public String generateInstallCliScript(String requestUrl, String installDirectory) {
        return mustacheTemplateEngine.render(INSTALL_CLI_TEMPLATE, getInstallCliContext(requestUrl, installDirectory));
    }

    InstallCliContext getInstallCliContext(String requestUrl, String installDirectory) {
        try {
            URL url = new URL(requestUrl);
            return new InstallCliContext(
                    installDirectory,
                    url.getProtocol(),
                    url.getHost(),
                    getPort(url));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Can't get install CLI context because of malformed URL", e);
        }
    }

    String getPort(URL url) {
        int port = url.getPort();

        if (port == -1) {
            port = url.getDefaultPort();
        }

        return Integer.toString(port);
    }

    public String getVersion() {
        String fullVersion = version;

        if (gitInfo.getCommit() != null) {
            fullVersion += " (git commit id: " + gitInfo.getCommit().getId() + ")";
        }

        return fullVersion;
    }
}
