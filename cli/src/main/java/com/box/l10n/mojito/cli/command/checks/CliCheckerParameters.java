package com.box.l10n.mojito.cli.command.checks;

public enum CliCheckerParameters {
  DICTIONARY_ADDITIONS_PATH_KEY("dictionaryAdditionsFilePath"),
  GLOSSARY_FILE_PATH_KEY("glossaryFilePath"),
  DICTIONARY_FILE_PATH_KEY("dictionaryFilePath"),
  DICTIONARY_AFFIX_FILE_PATH_KEY("dictionaryAffixFilePath"),
  CONTEXT_COMMENT_REJECT_PATTERN_KEY("contextCommentRejectPattern"),
  RECOMMEND_STRING_ID_LABEL_IGNORE_PATTERN_KEY("recommendStringIdLabelIgnorePattern");

  private String key;

  CliCheckerParameters(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
