package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.CONTEXT_COMMENT_REJECT_PATTERN_KEY;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.DICTIONARY_ADDITIONS_PATH_KEY;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.DICTIONARY_AFFIX_FILE_PATH_KEY;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.DICTIONARY_FILE_PATH_KEY;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.GLOSSARY_FILE_PATH_KEY;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.RECOMMEND_STRING_ID_LABEL_IGNORE_PATTERN_KEY;

import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public class CliCheckerOptions {

  private final Set<PlaceholderRegularExpressions> parameterRegexSet;
  private final Set<CliCheckerType> hardFailureSet;
  private final ImmutableMap<String, String> optionsMap;

  public CliCheckerOptions(
      Set<PlaceholderRegularExpressions> parameterRegexSet,
      Set<CliCheckerType> hardFailureSet,
      ImmutableMap<String, String> optionsMap) {
    this.parameterRegexSet = parameterRegexSet;
    this.hardFailureSet = hardFailureSet;
    this.optionsMap = optionsMap;
  }

  public Set<PlaceholderRegularExpressions> getParameterRegexSet() {
    return parameterRegexSet;
  }

  public Set<CliCheckerType> getHardFailureSet() {
    return hardFailureSet;
  }

  public String getDictionaryAdditionsFilePath() {
    return optionsMap.get(DICTIONARY_ADDITIONS_PATH_KEY.getKey());
  }

  public String getGlossaryFilePath() {
    return optionsMap.get(GLOSSARY_FILE_PATH_KEY.getKey());
  }

  public String getDictionaryFilePath() {
    return optionsMap.get(DICTIONARY_FILE_PATH_KEY.getKey());
  }

  public String getDictionaryAffixFilePath() {
    return optionsMap.get(DICTIONARY_AFFIX_FILE_PATH_KEY.getKey());
  }

  public String getContextCommentRejectPattern() {
    return optionsMap.get(CONTEXT_COMMENT_REJECT_PATTERN_KEY.getKey());
  }

  public String getRecommendStringIdLabelIgnorePattern() {
    return optionsMap.get(RECOMMEND_STRING_ID_LABEL_IGNORE_PATTERN_KEY.getKey());
  }

  public ImmutableMap<String, String> getOptionsMap() {
    return optionsMap;
  }
}
