package com.box.l10n.mojito.cli.command.checks;

import java.util.Set;

public class CliCheckerOptions {

    private final String parameterRegex;
    private final Set<String> hardFailureSet;
    private final String dictionaryAdditionsFilePath;
    private final String glossaryFilePath;

    public CliCheckerOptions(String parameterRegex, Set<String> hardFailureSet, String dictionaryAdditionsFilePath, String glossaryFilePath) {
        this.parameterRegex = parameterRegex;
        this.hardFailureSet = hardFailureSet;
        this.dictionaryAdditionsFilePath = dictionaryAdditionsFilePath;
        this.glossaryFilePath = glossaryFilePath;
    }
    
    public String getParameterRegex() {
        return parameterRegex;
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
