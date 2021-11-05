package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;

import java.util.Set;

public class CliCheckerOptions {

    private final Set<PlaceholderRegularExpressions> parameterRegexSet;
    private final Set<String> hardFailureSet;
    private final String dictionaryAdditionsFilePath;
    private final String glossaryFilePath;

    public CliCheckerOptions(Set<PlaceholderRegularExpressions> parameterRegexSet, Set<String> hardFailureSet, String dictionaryAdditionsFilePath, String glossaryFilePath) {
        this.parameterRegexSet = parameterRegexSet;
        this.hardFailureSet = hardFailureSet;
        this.dictionaryAdditionsFilePath = dictionaryAdditionsFilePath;
        this.glossaryFilePath = glossaryFilePath;
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
}
