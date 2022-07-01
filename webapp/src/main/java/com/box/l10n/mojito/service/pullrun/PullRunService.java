package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PullRunTextUnitVariant;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service to manage PullRun data.
 * 
 * @author garion
 */
@Service
public class PullRunService {

    @Autowired
    EntityManager entityManager;

    @Autowired
    PullRunRepository pullRunRepository;

    @Autowired
    PullRunAssetRepository pullRunAssetRepository;

    @Autowired
    PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

    /**
     * Creates a new PullRun entry with a new UUID as the logical name.
     */
    public PullRun createPullRun(Repository repository) {
        return createPullRun(repository, null);
    }

    /**
     * Creates a new PullRun entry with the specified name.
     * Note: if the name is null/blank, a UUID will be generated and used instead.
     */
    public PullRun createPullRun(Repository repository, String pullRunName) {
        if (StringUtils.isBlank(pullRunName)) {
            pullRunName = UUID.randomUUID().toString();
        }

        PullRun newPullRun = new PullRun();

        newPullRun.setRepository(repository);
        newPullRun.setName(pullRunName);

        return pullRunRepository.save(newPullRun);
    }

    /**
     * Removes the linked PullRunAssets and pullRunAssetTmTextUnitVariants from the PullRun.
     */
    @Transactional
    public void clearPullRunLinkedData(PullRun pullRun) {
        List<PullRunAsset> existingPullRunAssets = pullRunAssetRepository.findByPullRun(pullRun);
        existingPullRunAssets.forEach(pullRunTextUnitVariantRepository::deleteByPullRunAsset);
        pullRunAssetRepository.deleteByPullRun(pullRun);
    }

    /**
     * Associates a set of TextUnitVariants to a PullRunAsset and a PullRun.
     */
    public void associatePullRunToTextUnitIds(PullRun pullRun, Asset asset, List<Long> textUnitVariantIds) {
        // Avoid retrieving the full TmTextUnitVariant entities, as we only need the IDs for the foreign key reference
        List<TMTextUnitVariant> textUnitVariants = textUnitVariantIds.stream()
                .map(tmTextUnitVariantId -> entityManager.getReference(TMTextUnitVariant.class, tmTextUnitVariantId))
                .collect(Collectors.toList());

        associatePullRunToTextUnitVariants(pullRun, asset, textUnitVariants);
    }

    public List<TMTextUnitVariant> getTextUnitVariants(PullRun pullRun, Pageable pageable) {
        return pullRunTextUnitVariantRepository.findByPullRun(pullRun, pageable);
    }

    /**
     * Associates a set of TextUnitVariants to a PullRunAsset and a PullRun.
     */
    void associatePullRunToTextUnitVariants(PullRun pullRun, Asset asset, List<TMTextUnitVariant> textUnitVariants) {
        PullRunAsset pullRunAsset = pullRunAssetRepository.findByPullRunAndAsset(pullRun, asset).orElse(null);

        if (pullRunAsset == null) {
            pullRunAsset = new PullRunAsset();

            pullRunAsset.setPullRun(pullRun);
            pullRunAsset.setAsset(asset);

            pullRunAssetRepository.save(pullRunAsset);
        } else {
            pullRunTextUnitVariantRepository.deleteByPullRunAsset(pullRunAsset);
        }

        PullRunAsset finalPullRunAsset = pullRunAsset;
        List<PullRunTextUnitVariant> pullRunTextUnitVariants = textUnitVariants.stream()
                .map(tmTextUnitVariant -> {
                    PullRunTextUnitVariant pullRunTextUnitVariant = new PullRunTextUnitVariant();

                    pullRunTextUnitVariant.setPullRunAsset(finalPullRunAsset);
                    pullRunTextUnitVariant.setTmTextUnitVariant(tmTextUnitVariant);

                    return pullRunTextUnitVariant;
                })
                .collect(Collectors.toList());

        pullRunTextUnitVariantRepository.saveAll(pullRunTextUnitVariants);
    }
}
