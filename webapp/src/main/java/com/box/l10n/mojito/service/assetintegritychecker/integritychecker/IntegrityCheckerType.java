package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Integrity Checker Type enum
 *
 * @author wyau
 */
public enum IntegrityCheckerType {

    MESSAGE_FORMAT(MessageFormatIntegrityChecker.class.getName()),
    PRINTF_LIKE(PrintfLikeIntegrityChecker.class.getName()),
    SIMPLE_PRINTF_LIKE(SimplePrintfLikeIntegrityChecker.class.getName()),
    COMPOSITE_FORMAT(CompositeFormatIntegrityChecker.class.getName()),
    TRAILING_WHITESPACE(TrailingWhitespaceIntegrityChecker.class.getName()),
    HTML_TAG(HtmlTagIntegrityChecker.class.getName());

    String className;

    IntegrityCheckerType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

}
