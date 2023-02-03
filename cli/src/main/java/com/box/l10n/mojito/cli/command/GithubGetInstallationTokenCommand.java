package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClients;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"github-install-token"},
    commandDescription = "Retrieves a github installation token for the specified installation")
public class GithubGetInstallationTokenCommand extends Command {

  @Autowired(required = false)
  GithubClients githubClients;

  @Qualifier("ansiCodeEnabledFalse")
  @Autowired
  ConsoleWriter consoleWriter;

  @Parameter(
      names = {"--owner", "-o"},
      required = true,
      arity = 1,
      description = "The Github repository owner")
  String owner;

  @Parameter(
      names = {"--repository", "-r"},
      required = true,
      arity = 1,
      description = "The Github repository name")
  String repository;

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  protected void execute() throws CommandException {
    try {
      if (githubClients == null) {
        throw new CommandException(
            "Github must be configured with properties: l10n.githubClients.<client>.appId, l10n.githubClients.<client>.key and l10n.githubClients.<client>.owner");
      }
      consoleWriter
          .a(githubClients.getClient(owner).getGithubAppInstallationToken(repository).getToken())
          .print();
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new CommandException(e);
    }
  }
}
