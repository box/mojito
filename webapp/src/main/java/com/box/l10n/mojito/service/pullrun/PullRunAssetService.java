package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  Service to manage PullRunAsset entities.
 *
 * @author garion
 */
@Service
public class PullRunAssetService {
    @Autowired
    PullRunAssetRepository pullRunAssetRepository;

    public PullRunAsset createPullRunAsset(PullRun pullRun, Asset asset) {
        PullRunAsset pullRunAsset = new PullRunAsset();
        pullRunAsset.setPullRun(pullRun);
        pullRunAsset.setAsset(asset);

        return pullRunAssetRepository.save(pullRunAsset);
    }
}
