package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Integrity Checker Type enum
 *
 * @author wyau
 */
public enum IntegrityCheckerType {
  MESSAGE_FORMAT(MessageFormatIntegrityChecker.class.getName()),
  MESSAGE_FORMAT_DOUBLE_BRACES(MessageFormatDoubleBracesIntegrityChecker.class.getName()),
  PRINTF_LIKE(PrintfLikeIntegrityChecker.class.getName()),
  SIMPLE_PRINTF_LIKE(SimplePrintfLikeIntegrityChecker.class.getName()),
  PRINTF_LIKE_IGNORE_PERCENTAGE_AFTER_BRACKETS(
      PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker.class.getName()),
  PRINTF_LIKE_VARIABLE_TYPE(PrintfLikeVariableTypeIntegrityChecker.class.getName()),
  PRINTF_LIKE_ADD_PARAMETER_SPECIFIER(
      PrintfLikeAddParameterSpecifierIntegrityChecker.class.getName()),
  COMPOSITE_FORMAT(CompositeFormatIntegrityChecker.class.getName()),
  WHITESPACE(WhitespaceIntegrityChecker.class.getName()),
  TRAILING_WHITESPACE(TrailingWhitespaceIntegrityChecker.class.getName()),
  HTML_TAG(HtmlTagIntegrityChecker.class.getName()),
  ELLIPSIS(EllipsisIntegrityChecker.class.getName()),
  BACKQUOTE(BackquoteIntegrityChecker.class.getName()),
  EMPTY_TARGET_NOT_EMPTY_SOURCE(EmptyTargetNotEmptySourceIntegrityChecker.class.getName()),
  MARKDOWN_LINKS(MarkdownLinkIntegrityChecker.class.getName()),
  PYTHON_FPRINT(PythonFStringIntegrityChecker.class.getName()),
  EMAIL(EmailIntegrityChecker.class.getName());

  String className;

  IntegrityCheckerType(String className) {
    this.className = className;
  }

  public String getClassName() {
    return className;
  }
}
