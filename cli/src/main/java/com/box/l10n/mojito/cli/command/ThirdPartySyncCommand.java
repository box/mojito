package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.ThirdPartyClient;
import com.box.l10n.mojito.rest.client.ThirdPartySync;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.fusesource.jansi.Ansi.Color.CYAN;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"thirdparty-sync", "tps"}, commandDescription = "Third-party command to sychronize text units and screenshots with third party TMS")
public class ThirdPartySyncCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ThirdPartySyncCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"--project", "-p"}, arity = 1, required = true, description = "Third party project to synchronize with")
    String thirdPartyProjectId;

    @Parameter(names = {"--actions", "-a"}, variableArity = true, required = false, description = "Actions to synchronize", converter = ThirdPartySyncActionsConverter.class)
    List<ThirdPartySync.Action> actions = Arrays.asList(ThirdPartySync.Action.MAP_TEXTUNIT, ThirdPartySync.Action.PUSH_SCREENSHOT);

    @Parameter(names = {"--plural-separator", "-ps"}, arity = 1, required = false, description = "Plural separator for name")
    String pluralSeparator;

    @Parameter(names = {Param.REPOSITORY_LOCALES_MAPPING_LONG, Param.REPOSITORY_LOCALES_MAPPING_SHORT}, arity = 1, required = false, description = "Locale mapping")
    String localeMapping;

    @Parameter(names = {"--options", "-o"}, variableArity = true, required = false, description = "Options to synchronize")
    List<String> options;

    @Autowired
    ThirdPartyClient thirdPartyClient;

    @Autowired
    CommandHelper commandHelper;

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Third party TMS synchronization for repository: ").fg(CYAN).a(repositoryParam).reset()
                .a(" project id: ").fg(CYAN).a(thirdPartyProjectId).reset()
                .a(" actions: ").fg(CYAN).a(Objects.toString(actions)).reset()
                .a(" plural-separator: ").fg(CYAN).a(Objects.toString(pluralSeparator)).reset()
                .a(" locale-mapping: ").fg(CYAN).a(Objects.toString(localeMapping)).reset()
                .a(" options: ").fg(CYAN).a(Objects.toString(options)).println(2);

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        PollableTask pollableTask = thirdPartyClient.sync(repository.getId(), thirdPartyProjectId, pluralSeparator, localeMapping, actions, options);

        commandHelper.waitForPollableTask(pollableTask.getId());

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

}