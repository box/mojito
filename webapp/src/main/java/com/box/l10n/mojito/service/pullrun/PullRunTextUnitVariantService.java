package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PullRunTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to manage PullRunTextUnitVariant entities.
 *
 * @author garion
 */
@Service
public class PullRunTextUnitVariantService {

    @Autowired
    PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

    public PullRunTextUnitVariant createPullRunTextUnitVariant(PullRunAsset pullRunAsset, TMTextUnitVariant tmTextUnitVariant) {
        PullRunTextUnitVariant pullRunTextUnitVariant = new PullRunTextUnitVariant();
        pullRunTextUnitVariant.setPullRunAsset(pullRunAsset);
        pullRunTextUnitVariant.setTmTextUnitVariant(tmTextUnitVariant);

        return pullRunTextUnitVariantRepository.save(pullRunTextUnitVariant);
    }
}
