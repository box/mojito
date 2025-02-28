package com.box.l10n.mojito.cli.command;

import static java.util.Optional.ofNullable;

import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.apiclient.RepositoryClient;
import com.box.l10n.mojito.apiclient.model.AssetIntegrityChecker;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @author jyi
 */
public abstract class RepoCommand extends Command {

  protected static final String INTEGRITY_CHECK_LONG_PARAM = "--integrity-check";
  protected static final String INTEGRITY_CHECK_SHORT_PARAM = "-it";
  protected static final String INTEGRITY_CHECK_DESCRIPTION =
      "Integrity Checker by File Extension, comma seperated format: \"FILE_EXTENSION_1:CHECKER_TYPE_1,FILE_EXTENSION_2:CHECKER_TYPE_2\"\n       "
          + "Available Checker types: MESSAGE_FORMAT, MESSAGE_FORMAT_DOUBLE_BRACES, PRINTF_LIKE, PRINTF_LIKE_IGNORE_PERCENTAGE_AFTER_BRACKETS, PRINTF_LIKE_VARIABLE_TYPE, PRINTF_LIKE_ADD_PARAMETER_SPECIFIER, SIMPLE_PRINTF_LIKE, COMPOSITE_FORMAT, WHITESPACE, TRAILING_WHITESPACE, HTML_TAG, ELLIPSIS, BACKQUOTE\n       "
          + "For examples: \"properties:MESSAGE_FORMAT,xliff:PRINTF_LIKE\"";

  @Autowired protected ConsoleWriter consoleWriter;

  @Autowired protected RepositoryClient repositoryClient;

  @Autowired protected LocaleHelper localeHelper;

  /**
   * Extract {@link AssetIntegrityChecker} Set from {@link RepoCreateCommand#integrityCheckParam} to
   * prep for {@link com.box.l10n.mojito.rest.apiclient.model.Repository} creation
   *
   * @param integrityCheckParam
   * @param doPrint
   * @return
   */
  protected List<AssetIntegrityChecker> extractIntegrityCheckersFromInput(
      String integrityCheckParam, boolean doPrint) throws CommandException {
    Set<AssetIntegrityChecker> integrityCheckers = null;
    if (integrityCheckParam != null) {
      integrityCheckers = new HashSet<>();
      Set<String> integrityCheckerParams = StringUtils.commaDelimitedListToSet(integrityCheckParam);
      if (doPrint) {
        consoleWriter.a("Extracted Integrity Checkers").println();
      }

      for (String integrityCheckerParam : integrityCheckerParams) {
        String[] param = StringUtils.delimitedListToStringArray(integrityCheckerParam, ":");
        if (param.length != 2) {
          throw new ParameterException(
              "Invalid integrity checker format [" + integrityCheckerParam + "]");
        }
        String fileExtension = param[0];
        String checkerType = param[1];
        AssetIntegrityChecker integrityChecker = new AssetIntegrityChecker();
        integrityChecker.setAssetExtension(fileExtension);
        try {
          integrityChecker.setIntegrityCheckerType(
              AssetIntegrityChecker.IntegrityCheckerTypeEnum.valueOf(checkerType));
        } catch (IllegalArgumentException ex) {
          throw new ParameterException("Invalid integrity checker type [" + checkerType + "]");
        }

        if (doPrint) {
          consoleWriter
              .fg(Ansi.Color.BLUE)
              .a("-- file extension = ")
              .fg(Ansi.Color.GREEN)
              .a(integrityChecker.getAssetExtension())
              .println();
          consoleWriter
              .fg(Ansi.Color.BLUE)
              .a("-- checker type = ")
              .fg(Ansi.Color.GREEN)
              .a(integrityChecker.getIntegrityCheckerType().toString())
              .println();
        }

        integrityCheckers.add(integrityChecker);
      }
    }
    return ofNullable(integrityCheckers).map(checkers -> checkers.stream().toList()).orElse(null);
  }
}
