package com.box.l10n.mojito.cli.command.checks;

import java.util.Set;

public class CliCheckerOptions {

    private final Set<String> parameterRegexSet;
    private final Set<String> hardFailureSet;
    private final String dictionaryAdditionsFilePath;
    private final String glossaryFilePath;

    public CliCheckerOptions(Set<String> parameterRegexSet, Set<String> hardFailureSet, String dictionaryAdditionsFilePath, String glossaryFilePath) {
        this.parameterRegexSet = parameterRegexSet;
        this.hardFailureSet = hardFailureSet;
        this.dictionaryAdditionsFilePath = dictionaryAdditionsFilePath;
        this.glossaryFilePath = glossaryFilePath;
    }
    
    public Set<String> getParameterRegexSet() {
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
