package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRunAsset;
import com.box.l10n.mojito.entity.PushRunAssetTmTextUnit;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service that manages PushRuns. Allows creation of PushRuns,
 * association and retrieval of connected TextUnit data.
 *
 * @author garion
 */
@Service
public class PushRunService {

    final EntityManager entityManager;

    final PushRunRepository pushRunRepository;

    final PushRunAssetRepository pushRunAssetRepository;

    final PushRunAssetTmTextUnitRepository pushRunAssetTmTextUnitRepository;

    public PushRunService(EntityManager entityManager,
                          PushRunRepository pushRunRepository,
                          PushRunAssetRepository pushRunAssetRepository,
                          PushRunAssetTmTextUnitRepository pushRunAssetTmTextUnitRepository) {
        this.entityManager = entityManager;
        this.pushRunRepository = pushRunRepository;
        this.pushRunAssetRepository = pushRunAssetRepository;
        this.pushRunAssetTmTextUnitRepository = pushRunAssetTmTextUnitRepository;
    }

    /**
     * Creates a new PushRun entry with a new UUID as the logical name.
     */
    public PushRun createPushRun(Repository repository) {
        return createPushRun(repository, null);
    }

    /**
     * Creates a new PushRun entry with the specified name.
     * Note: if the name is null/blank, a UUID will be generated and used instead.
     */
    public PushRun createPushRun(Repository repository, String pushRunName) {
        if (StringUtils.isBlank(pushRunName)) {
            pushRunName = UUID.randomUUID().toString();
        }

        PushRun newPushRun = new PushRun();

        newPushRun.setRepository(repository);
        newPushRun.setName(pushRunName);

        return pushRunRepository.save(newPushRun);
    }

    /**
     * Removes the linked PushRunAssets and pushRunAssetTmTextUnits from the PushRun.
     */
    @Transactional
    public void clearPushRunLinkedData(PushRun pushRun) {
        List<PushRunAsset> existingPushRunAssets = pushRunAssetRepository.findByPushRun(pushRun);
        existingPushRunAssets.forEach(pushRunAssetTmTextUnitRepository::deleteByPushRunAsset);
        pushRunAssetRepository.deleteByPushRun(pushRun);
    }

    /**
     * Associates a set of TextUnits to a PushRunAsset and a PushRun.
     */
    public void associatePushRunToTextUnitIds(PushRun pushRun, Asset asset, List<Long> textUnitIds) {
        // Avoid retrieving the full TmTextUnit entities, as we only need the IDs for the foreign key reference
        List<TMTextUnit> textUnits = textUnitIds.stream()
                .map(tmTextUnitId -> entityManager.getReference(TMTextUnit.class, tmTextUnitId))
                .collect(Collectors.toList());

        associatePushRunToTextUnits(pushRun, asset, textUnits);
    }

    /**
     * Retrieves the list of TextUnits associated with a PushRun.
     */
    public List<TMTextUnit> getPushRunTextUnits(PushRun pushRun, Pageable pageable) {
        return pushRunAssetTmTextUnitRepository.findByPushRun(pushRun, pageable);
    }

    /**
     * Associates a set of TextUnits to a PushRunAsset and a PushRun.
     */
    void associatePushRunToTextUnits(PushRun pushRun, Asset asset, List<TMTextUnit> textUnits) {
        PushRunAsset pushRunAsset = pushRunAssetRepository.findByPushRunAndAsset(pushRun, asset).orElse(null);

        if (pushRunAsset == null) {
            pushRunAsset = new PushRunAsset();

            pushRunAsset.setPushRun(pushRun);
            pushRunAsset.setAsset(asset);

            pushRunAssetRepository.save(pushRunAsset);
        } else {
            pushRunAssetTmTextUnitRepository.deleteByPushRunAsset(pushRunAsset);
        }

        PushRunAsset finalPushRunAsset = pushRunAsset;
        List<PushRunAssetTmTextUnit> pushRunAssetTmTextUnits = textUnits.stream()
                .map(tmTextUnit -> {
                    PushRunAssetTmTextUnit pushRunAssetTmTextUnit = new PushRunAssetTmTextUnit();

                    pushRunAssetTmTextUnit.setPushRunAsset(finalPushRunAsset);
                    pushRunAssetTmTextUnit.setTmTextUnit(tmTextUnit);

                    return pushRunAssetTmTextUnit;
                }).collect(Collectors.toList());

        pushRunAssetTmTextUnitRepository.saveAll(pushRunAssetTmTextUnits);
    }
}
