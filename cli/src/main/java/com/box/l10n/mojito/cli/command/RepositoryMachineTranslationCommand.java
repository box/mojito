package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.MachineTranslationWsApi;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.PollableTask;
import com.box.l10n.mojito.cli.model.RepositoryMachineTranslationBody;
import java.util.List;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to machine translate untranslated strings in a repository.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repository-machine-translate"},
    commandDescription = "Machine translate untranslated strings in a repository")
public class RepositoryMachineTranslationCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryMachineTranslationCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {Param.REPOSITORY_LOCALES_LONG, Param.REPOSITORY_LOCALES_SHORT},
      variableArity = true,
      required = true,
      description = "List of locales (bcp47 tags) to machine translate")
  List<String> locales;

  @Parameter(
      names = {"--source-text-max-count"},
      arity = 1,
      description =
          "Source text max count per locale sent to MT (this param is used to avoid "
              + "sending too many strings to MT)")
  int sourceTextMaxCount = 100;

  @Autowired CommandHelper commandHelper;

  @Autowired MachineTranslationWsApi repositoryMachineTranslationClient;

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Machine translate repository: ")
        .fg(Color.CYAN)
        .a(repositoryParam)
        .reset()
        .a(" for locales: ")
        .fg(Color.CYAN)
        .a(locales.stream().collect(Collectors.joining(", ", "[", "]")))
        .println(2);

    RepositoryMachineTranslationBody repositoryMachineTranslationBody =
        new RepositoryMachineTranslationBody();
    repositoryMachineTranslationBody.setRepositoryName(repositoryParam);
    repositoryMachineTranslationBody.setTargetBcp47tags(locales);
    repositoryMachineTranslationBody.setSourceTextMaxCountPerLocale(sourceTextMaxCount);

    repositoryMachineTranslationBody =
        repositoryMachineTranslationClient.translateRepository(repositoryMachineTranslationBody);

    PollableTask pollableTask = repositoryMachineTranslationBody.getPollableTask();
    commandHelper.waitForPollableTask(pollableTask.getId());
  }
}
