package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.IntegrityCheckerType;
import java.util.HashSet;
import java.util.Set;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 *
 * @author jyi
 */
public abstract class RepoCommand extends Command {

    protected static final String INTEGRITY_CHECK_LONG_PARAM = "--integrity-check";
    protected static final String INTEGRITY_CHECK_SHORT_PARAM = "-it";
    protected static final String INTEGRITY_CHECK_DESCRIPTION
            = "Integrity Checker by File Extension, comma seperated format: \"FILE_EXTENSION_1:CHECKER_TYPE_1,FILE_EXTENSION_2:CHECKER_TYPE_2\"\n       "
            + "Available Checker types: MESSAGE_FORMAT, PRINTF_LIKE, SIMPLE_PRINTF_LIKE, COMPOSITE_FORMAT, TRAILING_WHITESPACE, HTML_TAG\n       "
            + "For examples: \"properties:MESSAGE_FORMAT,xliff:PRINTF_LIKE\"";

    @Autowired
    protected ConsoleWriter consoleWriter;

    @Autowired
    protected RepositoryClient repositoryClient;

    @Autowired
    protected LocaleHelper localeHelper;

    /**
     * Extract {@link IntegrityChecker} Set from
     * {@link RepoCreateCommand#integrityCheckParam} to prep for
     * {@link Repository} creation
     *
     * @param integrityCheckParam
     * @param doPrint
     * @return
     */
    protected Set<IntegrityChecker> extractIntegrityCheckersFromInput(String integrityCheckParam, boolean doPrint) throws CommandException {
        Set<IntegrityChecker> integrityCheckers = null;
        if (integrityCheckParam != null) {
            integrityCheckers = new HashSet<>();
            Set<String> integrityCheckerParams = StringUtils.commaDelimitedListToSet(integrityCheckParam);
            if (doPrint) {
                consoleWriter.a("Extracted Integrity Checkers").println();
            }

            for (String integrityCheckerParam : integrityCheckerParams) {
                String[] param = StringUtils.delimitedListToStringArray(integrityCheckerParam, ":");
                if (param.length != 2) {
                    throw new ParameterException("Invalid integrity checker format [" + integrityCheckerParam + "]");
                }
                String fileExtension = param[0];
                String checkerType = param[1];
                IntegrityChecker integrityChecker = new IntegrityChecker();
                integrityChecker.setAssetExtension(fileExtension);
                try {
                    integrityChecker.setIntegrityCheckerType(IntegrityCheckerType.valueOf(checkerType));
                } catch (IllegalArgumentException ex) {
                    throw new ParameterException("Invalid integrity checker type [" + checkerType + "]");
                }

                if (doPrint) {
                    consoleWriter.fg(Ansi.Color.BLUE).a("-- file extension = ").fg(Ansi.Color.GREEN).a(integrityChecker.getAssetExtension()).println();
                    consoleWriter.fg(Ansi.Color.BLUE).a("-- checker type = ").fg(Ansi.Color.GREEN).a(integrityChecker.getIntegrityCheckerType().toString()).println();
                }

                integrityCheckers.add(integrityChecker);
            }
        }
        return integrityCheckers;
    }

}
