package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.IntegrityCheckerType;
import com.google.common.base.Splitter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jyi
 */
public abstract class RepoCommand extends Command {

    protected static final String INTEGRITY_CHECK_LONG_PARAM = "--integrity-check";
    protected static final String INTEGRITY_CHECK_SHORT_PARAM = "-it";
    protected static final String INTEGRITY_CHECK_DESCRIPTION
            = "Integrity Checker by File Extension, comma seperated format: \"FILE_EXTENSION_1:CHECKER_TYPE_1,FILE_EXTENSION_2:CHECKER_TYPE_2\"\n"
            + "Available Checker types: MESSAGE_FORMAT, PRINTF_LIKE, COMPOSITE_FORMAT\n"
            + "For examples: \"properties:MESSAGE_FORMAT,xliff:PRINTF_LIKE\"";

    @Autowired
    protected ConsoleWriter consoleWriter;

    @Autowired
    protected RepositoryClient repositoryClient;

    @Autowired
    protected LocaleHelper localeHelper;

    /**
     * Contains a map of {@link IntegrityChecker} with the file extension as key
     * and the value as {@link IntegrityCheckerType}
     */
    protected Map<String, String> integrityCheckerMapping;

    /**
     * Extract {@link IntegrityChecker} Set from
     * {@link RepoCreateCommand#integrityCheckParam} to prep for
     * {@link Repository} creation
     *
     * @param integrityCheckParam
     * @param doPrint
     * @return
     */
    protected Set<IntegrityChecker> extractIntegrityCheckersFromInput(String integrityCheckParam, boolean doPrint) {
        Set<IntegrityChecker> integrityCheckers = new HashSet<>();
        setIntegrityCheckerMapping(integrityCheckParam);
        if (integrityCheckerMapping != null) {

            if (doPrint) {
                consoleWriter.a("Extracted Integrity Checkers").println();
            }

            for (Map.Entry<String, String> integrityCheckerParam : integrityCheckerMapping.entrySet()) {
                String fileExtension = integrityCheckerParam.getKey();
                String checkerType = integrityCheckerParam.getValue();
                IntegrityChecker integrityChecker = new IntegrityChecker();
                integrityChecker.setAssetExtension(fileExtension);
                integrityChecker.setIntegrityCheckerType(IntegrityCheckerType.valueOf(checkerType));

                if (doPrint) {
                    consoleWriter.fg(Ansi.Color.BLUE).a("-- file extension = ").fg(Ansi.Color.GREEN).a(integrityChecker.getAssetExtension()).println();
                    consoleWriter.fg(Ansi.Color.BLUE).a("-- checker type = ").fg(Ansi.Color.GREEN).a(integrityChecker.getIntegrityCheckerType().toString()).println();
                }

                integrityCheckers.add(integrityChecker);
            }
        }
        return integrityCheckers;
    }

    /**
     * @param integrityCheckerParam
     */
    protected void setIntegrityCheckerMapping(String integrityCheckerParam) {
        if (integrityCheckerParam != null) {
            integrityCheckerMapping = Splitter.on(",").withKeyValueSeparator(":").split(integrityCheckerParam);
        }
    }

}
