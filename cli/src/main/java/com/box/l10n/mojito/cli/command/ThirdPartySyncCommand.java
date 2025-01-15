package com.box.l10n.mojito.cli.command;

import static org.fusesource.jansi.Ansi.Color.CYAN;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.ThirdPartyWsApi;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.PollableTask;
import com.box.l10n.mojito.cli.model.ThirdPartySync;
import com.box.l10n.mojito.rest.entity.Repository;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"thirdparty-sync", "tps"},
    commandDescription =
        "Third-party command to sychronize text units and screenshots with third party TMS")
public class ThirdPartySyncCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ThirdPartySyncCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {"--project", "-p"},
      arity = 1,
      required = true,
      description = "Third party project to synchronize with")
  String thirdPartyProjectId;

  @Parameter(
      names = {"--actions", "-a"},
      variableArity = true,
      required = false,
      description = "Actions to synchronize",
      converter = ThirdPartySyncActionsConverter.class)
  List<ThirdPartySync.ActionsEnum> actions =
      Arrays.asList(
          ThirdPartySync.ActionsEnum.MAP_TEXTUNIT, ThirdPartySync.ActionsEnum.PUSH_SCREENSHOT);

  /**
   * The plural separator changes depending on the file type: android, po files and can also change
   * depending on the system uploading the plural strings.
   *
   * <p>Default value is for PO files. Android is "_" only
   */
  @Parameter(
      names = {"--plural-separator", "-ps"},
      arity = 1,
      required = false,
      description =
          "Plural separator for name (can use unicode espace: \\u0032 for leading/trailing space. Default value is for PO files.")
  String pluralSeparator = " _";

  @Parameter(
      names = {Param.REPOSITORY_LOCALES_MAPPING_LONG, Param.REPOSITORY_LOCALES_MAPPING_SHORT},
      arity = 1,
      required = false,
      description = Param.REPOSITORY_LOCALES_MAPPING_DESCRIPTION)
  String localeMapping;

  @Parameter(
      names = {"--skip-text-units-with-pattern", "-st"},
      arity = 1,
      required = false,
      description = "Do not process text units matching with the SQL LIKE expression")
  String skipTextUnitsWithPattern;

  @Parameter(
      names = {"--skip-assets-path-pattern", "-sa"},
      arity = 1,
      required = false,
      description = "Do not process text units whose assets path match the SQL LIKE expression")
  String skipAssetsWithPathPattern;

  @Parameter(
      names = {"--include-text-units-with-pattern", "-it"},
      arity = 1,
      required = false,
      description =
          "Only process text units matching with the SQL LIKE expression. Only used in conjunction with the PUSH_TRANSLATION action")
  String includeTextUnitsWithPattern;

  @Parameter(
      names = {"--timeout", "-t"},
      arity = 1,
      required = false,
      description = "Timeout in seconds")
  Long timeoutInSeconds = 3600L;

  @Parameter(
      names = {"--options", "-o"},
      variableArity = true,
      required = false,
      description = "Options to synchronize")
  List<String> options;

  @Autowired ThirdPartyWsApi thirdPartyClient;

  @Autowired CommandHelper commandHelper;

  @Override
  public void execute() throws CommandException {

    pluralSeparator = unescapeUnicodeSpaceSequence(pluralSeparator);

    consoleWriter
        .newLine()
        .a("Third party TMS synchronization for repository: ")
        .fg(CYAN)
        .a(repositoryParam)
        .reset()
        .a(" project id: ")
        .fg(CYAN)
        .a(thirdPartyProjectId)
        .reset()
        .a(" actions: ")
        .fg(CYAN)
        .a(Objects.toString(actions))
        .reset()
        .a(" plural-separator: \"")
        .fg(CYAN)
        .a(pluralSeparator)
        .reset()
        .a("\"")
        .a(" locale-mapping: ")
        .fg(CYAN)
        .a(localeMapping)
        .reset()
        .a(" skip-text-units-with-pattern: ")
        .fg(CYAN)
        .a(skipTextUnitsWithPattern)
        .reset()
        .a(" skip-assets-path-pattern: ")
        .fg(CYAN)
        .a(skipAssetsWithPathPattern)
        .reset()
        .a(" include-text-units-with-pattern")
        .fg(CYAN)
        .a(includeTextUnitsWithPattern)
        .reset()
        .a(" timeout: ")
        .fg(CYAN)
        .a(timeoutInSeconds)
        .reset()
        .a(" options: ")
        .fg(CYAN)
        .a(Objects.toString(options))
        .println(2);

    Repository repository = commandHelper.findRepositoryByName(repositoryParam);

    ThirdPartySync thirdPartySyncBody = new ThirdPartySync();
    thirdPartySyncBody.setRepositoryId(repository.getId());
    thirdPartySyncBody.setProjectId(thirdPartyProjectId);
    thirdPartySyncBody.setPluralSeparator(pluralSeparator);
    thirdPartySyncBody.setLocaleMapping(localeMapping);
    thirdPartySyncBody.setActions(actions);
    thirdPartySyncBody.setSkipTextUnitsWithPattern(skipTextUnitsWithPattern);
    thirdPartySyncBody.setSkipAssetsWithPathPattern(skipAssetsWithPathPattern);
    thirdPartySyncBody.setIncludeTextUnitsWithPattern(includeTextUnitsWithPattern);
    thirdPartySyncBody.setOptions(options);
    thirdPartySyncBody.setTimeout(timeoutInSeconds);
    PollableTask pollableTask = thirdPartyClient.sync(thirdPartySyncBody);

    commandHelper.waitForPollableTask(pollableTask.getId());

    consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
  }

  /**
   * The current version we have for JCommander trims the argument values, even when quoted.
   * https://github.com/cbeust/jcommander/issues/417
   * https://github.com/cbeust/jcommander/commit/4aec38b4a0ea63a8dc6f41636fa81c2ebafddc18
   *
   * <p>So for now we allow passing the unicode escape sequence into the parameter and escape
   */
  String unescapeUnicodeSpaceSequence(String input) {
    return input.replace("\\u0032", " ");
  }
}
