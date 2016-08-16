package com.box.l10n.mojito.service.assetintegritychecker;

import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author wyau
 */
@Service
public class AssetIntegrityCheckerService {

    @Autowired
    AssetIntegrityCheckerRepository assetIntegrityCheckerRepository;

    @Transactional
    public void addToRepository(Repository repository, String assetPath, IntegrityCheckerType integrityCheckerType) {
        AssetIntegrityChecker assetIntegrityChecker = new AssetIntegrityChecker();
        assetIntegrityChecker.setRepository(repository);
        assetIntegrityChecker.setAssetExtension(FilenameUtils.getExtension(assetPath));
        assetIntegrityChecker.setIntegrityCheckerType(integrityCheckerType);
        assetIntegrityCheckerRepository.save(assetIntegrityChecker);
    }
}
