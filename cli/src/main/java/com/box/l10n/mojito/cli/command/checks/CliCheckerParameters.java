package com.box.l10n.mojito.cli.command.checks;

public enum CliCheckerParameters {
  DICTIONARY_ADDITIONS_PATH_KEY("dictionaryAdditionsFilePath"),
  GLOSSARY_FILE_PATH_KEY("glossaryFilePath"),
  DICTIONARY_FILE_PATH_KEY("dictionaryFilePath"),
  DICTIONARY_AFFIX_FILE_PATH_KEY("dictionaryAffixFilePath"),
  CONTEXT_COMMENT_REJECT_PATTERN_KEY("contextCommentRejectPattern"),
  RECOMMEND_STRING_ID_LABEL_IGNORE_PATTERN_KEY("recommendStringIdLabelIgnorePattern"),
  CONTEXT_COMMENT_PLURAL_SKIP("contextCommentSkipPlurals"),
  REPOSITORY_NAME("repositoryName"),
  OPEN_AI_RETRY_ERROR_MSG("openAIRetryErrorMessage"),
  CONTEXT_COMMENT_EXCLUDE_FILES_PATTERN_KEY("contextCommentExcludeFilesPattern");

  private String key;

  CliCheckerParameters(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
