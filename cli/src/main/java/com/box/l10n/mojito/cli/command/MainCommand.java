package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.GitInfo;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.CliClient;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Main Command of the application, just has help and version options. Uses the empty string as
 * command name.
 *
 * @author jaurambault
 */
@Component
@Parameters(commandNames = "")
public class MainCommand extends Command {

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {"--version", "-v"},
      description = "Show version")
  private boolean versionParam;

  @Parameter(
      names = {"--check-server-version"},
      description = "Check if this CLI matches the server's version")
  private boolean checkVersionServerParam;

  @Value("${info.build.version}")
  String version;

  @Autowired GitInfo gitInfo;

  @Autowired CliClient cliClient;

  @Override
  void showUsage() {
    new L10nJCommander().usage();
  }

  @Override
  protected void execute() throws CommandException {
    if (versionParam) {
      showVersion();
    } else if (checkVersionServerParam) {
      checkServerVersion();
    } else {
      showUsage();
    }
  }

  void checkServerVersion() throws CommandException {
    String serverVersion = cliClient.getVersion();
    String cliVersion = getCliVersion();
    if (!cliVersion.equals(serverVersion)) {
      throw new CommandException(
          "CLI version: " + cliVersion + " not the same as the server version: " + serverVersion);
    }
  }

  void showVersion() {
    consoleWriter.fg(Ansi.Color.CYAN).a(version).reset();

    if (gitInfo.getCommit().getId() != null) {
      consoleWriter.a(" (git commit id: ").a(gitInfo.getCommit().getId()).a(")");
    }

    consoleWriter.println();
  }

  String getCliVersion() {
    String fullVersion = version;
    if (gitInfo.getCommit() != null) {
      fullVersion += " (git commit id: " + gitInfo.getCommit().getId() + ")";
    }
    return fullVersion;
  }
}
