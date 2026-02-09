package com.box.l10n.mojito.service.repository;

public record RepositoryLocaleRow(
    Long id, Long repositoryId, Long localeId, boolean toBeFullyTranslated, Long parentLocaleId) {}
