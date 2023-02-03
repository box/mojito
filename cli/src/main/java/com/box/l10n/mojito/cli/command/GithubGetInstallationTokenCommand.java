package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"github-install-token"},
    commandDescription = "Retrieves a github installation token for the specified installation")
public class GithubGetInstallationTokenCommand extends Command {

  @Autowired
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
      consoleWriter
          .a(githubClients.getClient(owner).getGithubAppInstallationToken(repository).getToken())
          .print();
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new CommandException(e);
    }
  }
}
