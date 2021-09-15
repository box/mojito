package com.box.l10n.mojito.rest.entity;

/**
 * Mirrors: com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType
 *
 * @author wyau
 */
public enum IntegrityCheckerType {

    MESSAGE_FORMAT,
    PRINTF_LIKE,
    PRINTF_LIKE_IGNORE_PERCENTAGE_AFTER_BRACKETS,
    PRINTF_LIKE_VARIABLE_TYPE,
    SIMPLE_PRINTF_LIKE,
    COMPOSITE_FORMAT,
    WHITESPACE,
    TRAILING_WHITESPACE,
    HTML_TAG,
    ELLIPSIS,
    BACKQUOTE,
    EMPTY_TARGET_NOT_EMPTY_SOURCE;
}
