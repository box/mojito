package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jyi
 */
public abstract class RepoCommand extends Command {

  protected static final String INTEGRITY_CHECK_LONG_PARAM = "--integrity-check";
  protected static final String INTEGRITY_CHECK_SHORT_PARAM = "-it";
  protected static final String INTEGRITY_CHECK_DESCRIPTION =
      "Integrity Checker by File Extension, comma seperated format: \"FILE_EXTENSION_1:CHECKER_TYPE_1,FILE_EXTENSION_2:CHECKER_TYPE_2\"\n       "
          + "Stores checkers on this repository only. If a repository type is assigned, Mojito also runs that type's checkers.\n       "
          + "Available Checker types: "
          + IntegrityCheckerCli.AVAILABLE_CHECKER_TYPES_TOKEN
          + "\n       "
          + "For examples: \"properties:MESSAGE_FORMAT,xliff:PRINTF_LIKE\"";

  @Autowired protected ConsoleWriter consoleWriter;

  @Autowired protected RepositoryClient repositoryClient;

  @Autowired protected LocaleHelper localeHelper;

  /**
   * Extract {@link IntegrityChecker} Set from {@link RepoCreateCommand#integrityCheckParam} to prep
   * for {@link Repository} creation
   *
   * @param integrityCheckParam
   * @param doPrint
   * @return
   */
  protected Set<IntegrityChecker> extractIntegrityCheckersFromInput(
      String integrityCheckParam, boolean doPrint) throws CommandException {
    return IntegrityCheckerCli.parse(integrityCheckParam, doPrint ? consoleWriter : null);
  }
}
