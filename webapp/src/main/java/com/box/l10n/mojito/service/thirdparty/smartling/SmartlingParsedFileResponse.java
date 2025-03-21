package com.box.l10n.mojito.service.thirdparty.smartling;

/**
 * Data carrier that should only store the parsed fileContent in the data field. Used in the third
 * party sync on the pull step, if the fileContent cannot be parsed it is re-downloaded and
 * attempted again in the retry clause.
 */
public record SmartlingParsedFileResponse<T>(String fileContent, T data) {}
