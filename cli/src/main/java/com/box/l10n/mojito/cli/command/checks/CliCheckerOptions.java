package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.phabricator.DifferentialDiff;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;

import java.util.Set;

public class CliCheckerOptions {

    private final Set<PlaceholderRegularExpressions> parameterRegexSet;
    private final Set<CliCheckerType> hardFailureSet;
    private final String dictionaryAdditionsFilePath;
    private final String glossaryFilePath;
    private String dictionaryFilePath;
    private String dictionaryAffixFilePath;
    private String contextCommentRejectPattern;

    public CliCheckerOptions(Set<PlaceholderRegularExpressions> parameterRegexSet, Set<CliCheckerType> hardFailureSet, String dictionaryAdditionsFilePath, String glossaryFilePath) {
        this.parameterRegexSet = parameterRegexSet;
        this.hardFailureSet = hardFailureSet;
        this.dictionaryAdditionsFilePath = dictionaryAdditionsFilePath;
        this.glossaryFilePath = glossaryFilePath;
    }

    public CliCheckerOptions(Set<PlaceholderRegularExpressions> parameterRegexSet, Set<CliCheckerType> hardFailureSet, String dictionaryAdditionsFilePath,
                             String glossaryFilePath, String dictionaryFilePath, String dictionaryAffixFilePath, String contextCommentRejectPattern) {
        this(parameterRegexSet, hardFailureSet, dictionaryAdditionsFilePath, glossaryFilePath);
        this.dictionaryFilePath = dictionaryFilePath;
        this.dictionaryAffixFilePath = dictionaryAffixFilePath;
        this.contextCommentRejectPattern = contextCommentRejectPattern;
    }
    
    public Set<PlaceholderRegularExpressions> getParameterRegexSet() {
        return parameterRegexSet;
    }

    public Set<CliCheckerType> getHardFailureSet() {
        return hardFailureSet;
    }

    public String getDictionaryAdditionsFilePath() {
        return dictionaryAdditionsFilePath;
    }

    public String getGlossaryFilePath() {
        return glossaryFilePath;
    }

    public String getDictionaryFilePath() {
        return dictionaryFilePath;
    }

    public String getDictionaryAffixFilePath() {
        return dictionaryAffixFilePath;
    }

    public String getContextCommentRejectPattern() {
        return contextCommentRejectPattern;
    }


}
