package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.phabricator.DifferentialDiff;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;

import java.util.Set;

public class CliCheckerOptions {

    private final Set<PlaceholderRegularExpressions> parameterRegexSet;
    private final Set<String> hardFailureSet;
    private final String dictionaryAdditionsFilePath;
    private final String glossaryFilePath;
    private final String diffId;
    private final DifferentialDiff differentialDiff;
    private final DifferentialRevision differentialRevision;

    public CliCheckerOptions(Set<PlaceholderRegularExpressions> parameterRegexSet, Set<String> hardFailureSet, String dictionaryAdditionsFilePath, String glossaryFilePath) {
        this.parameterRegexSet = parameterRegexSet;
        this.hardFailureSet = hardFailureSet;
        this.dictionaryAdditionsFilePath = dictionaryAdditionsFilePath;
        this.glossaryFilePath = glossaryFilePath;
        this.diffId = null;
        this.differentialRevision = null;
        this.differentialDiff = null;
    }

    public CliCheckerOptions(Set<PlaceholderRegularExpressions> parameterRegexSet, Set<String> hardFailureSet,
                             String dictionaryAdditionsFilePath, String glossaryFilePath, String diffId, DifferentialDiff differentialDiff, DifferentialRevision differentialRevision) {
        this.parameterRegexSet = parameterRegexSet;
        this.hardFailureSet = hardFailureSet;
        this.dictionaryAdditionsFilePath = dictionaryAdditionsFilePath;
        this.glossaryFilePath = glossaryFilePath;
        this.diffId = diffId;
        this.differentialDiff = differentialDiff;
        this.differentialRevision = differentialRevision;
    }
    
    public Set<PlaceholderRegularExpressions> getParameterRegexSet() {
        return parameterRegexSet;
    }

    public Set<String> getHardFailureSet() {
        return hardFailureSet;
    }

    public String getDictionaryAdditionsFilePath() {
        return dictionaryAdditionsFilePath;
    }

    public String getGlossaryFilePath() {
        return glossaryFilePath;
    }

    public String getDiffId() {
        return diffId;
    }

    public DifferentialDiff getDifferentialDiff() {
        return differentialDiff;
    }

    public DifferentialRevision getDifferentialRevision() {
        return differentialRevision;
    }
}
