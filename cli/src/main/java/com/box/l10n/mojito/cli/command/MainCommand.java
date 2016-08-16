package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.GitInfo;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Main Command of the application, just has help and version options. Uses the
 * empty string as command name.
 *
 * @author jaurambault
 */
@Component
@Parameters(commandNames = "")
public class MainCommand extends Command {

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {"--version", "-v"}, description = "Show version")
    private boolean versionParam;

    @Value("${info.build.version}")
    String version;

    @Autowired
    GitInfo gitInfo;

    @Override
    void showUsage() {
        new L10nJCommander().usage();
    }

    @Override
    protected void execute() throws CommandException {
        if (versionParam) {
            consoleWriter.fg(Ansi.Color.CYAN).a(version).reset();

            if (gitInfo.getCommit().getId() != null) {
                consoleWriter.a(" (git commit id: ").a(gitInfo.getCommit().getId()).a(")");
            }

            consoleWriter.println();
        } else {
            showUsage();
        }
    }

}
