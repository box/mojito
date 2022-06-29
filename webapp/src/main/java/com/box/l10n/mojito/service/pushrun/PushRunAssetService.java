package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRunAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to manage PushRunAsset entities.
 *
 * @author garion
 */
@Service
public class PushRunAssetService {
    @Autowired
    PushRunAssetRepository pushRunAssetRepository;

    public PushRunAsset createPushRunAsset(PushRun pushRun, Asset asset) {
        PushRunAsset pushRunAsset = new PushRunAsset();
        pushRunAsset.setPushRun(pushRun);
        pushRunAsset.setAsset(asset);

        return pushRunAssetRepository.save(pushRunAsset);
    }
}
