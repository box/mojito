package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;

public record AssetIntegrityCheckerRow(
    Long id, Long repositoryId, String assetExtension, IntegrityCheckerType integrityCheckerType) {}
