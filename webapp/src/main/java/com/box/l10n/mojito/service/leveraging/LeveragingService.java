package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.asset.AssetWithIdNotFoundException;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author jaurambault
 */
@Service
public class LeveragingService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(LeveragingService.class);

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    LeveragerByNameAndContentForSourceLeveraging leveragerByNameAndContentForSourceLeveraging;

    @Autowired
    LeveragerByNameAndContentUnusedForSourceLeveraging leveragerByNameAndContentUnusedForSourceLeveraging;

    @Autowired
    LeveragerByContentForSourceLeveraging leveragerByContentForSourceLeveraging;

    @Autowired
    LeveragerByNameForSourceLeveraging leveragerByNameForSourceLeveraging;

    @Autowired
    LeveragerByMd5 leveragerByMd5;

    @Autowired
    LeveragerByContent leveragerByContent;

    @Autowired
    LeveragerByNameAndContent leveragerByNameAndContent;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    AssetRepository assetRepository;
    @Autowired
    TextUnitSearcher textUnitSearcher;
    @Autowired
    TMService tmService;

    /**
     * Performs "source" leveraging for a list of {@link TMTextUnit}s.
     * <p/>
     * This process is about trying to identify renamed keys, comment changes,
     * source string modification. When such a change happens the system will
     * copy over all the {@link TMTextUnitVariant}s from the old
     * {@link TMTextUnit} into the new one.
     * <p/>
     * This code is not trying to find matches based on the target locale, it's
     * really about trying to find partial matches of the text units following
     * maintenance operation (renamed key, minor change to source string, etc).
     * <p>
     * Changes might be minor to fix typo or could require a review. Right now
     * only a change in content will require a review. Modification to a comment
     * or renaming a key won't trigger a review.
     * <p/>
     * Note that if {@link TMTextUnitVariant} are added via TK import or
     * workbench on the old {@link TMTextUnit} the new mapped {@link TMTextUnit}
     * will be missing the new translation. That case will be handled later with
     * regular "target" leveraging. This case should be rare and on minimal
     * dataset.
     *
     * @param tmTextUnits list of {@link TMTextUnit}s that needs to be
     *                    processed. {@link TMTextUnit}s for which leveraged translations were
     *                    found are removed from the list to prevent further processing.
     */
    public void performSourceLeveraging(List<TMTextUnit> tmTextUnits) {

        logger.debug("Perform source leveraging for {} text units", tmTextUnits.size());

        leveragerByNameAndContentForSourceLeveraging.performLeveragingFor(tmTextUnits, null, null);
        leveragerByNameForSourceLeveraging.performLeveragingFor(tmTextUnits, null, null);
        leveragerByContentForSourceLeveraging.performLeveragingFor(tmTextUnits, null, null);
        leveragerByNameAndContentUnusedForSourceLeveraging.performLeveragingFor(tmTextUnits, null, null);
    }

    /**
     * This will copy all translations from the source repository into the
     * target repository (or asset), overriding any translation already
     * existing.
     * <p>
     * 2 match modes are available:
     * <p>
     * 1) Matches are performed based on MD5, if the repository has multiple
     * text units with same MD5 the source will be arbitrarily chosen.
     * <p>
     * 2) Matches are performed based on content only (exact match), if the
     * repository has multiple text units with same content it will first check
     * for string with same IDs and then the source will be arbitrarily chosen.
     *
     * @param copyTmConfig
     * @return
     */
    @Pollable(async = true, message = "Start copying translations between repository")
    public PollableFuture copyTm(CopyTmConfig copyTmConfig) throws AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {

        if (CopyTmConfig.Mode.TUIDS.equals(copyTmConfig.getMode())) {
            copyTranslationBetweenTextUnits(copyTmConfig.getSourceToTargetTmTextUnitIds());
        } else {
            copyTmBetweenRepositories(copyTmConfig);
        }

        return new PollableFutureTaskResult();
    }

    void copyTmBetweenRepositories(CopyTmConfig copyTmConfig) throws RepositoryWithIdNotFoundException, AssetWithIdNotFoundException {

        logger.debug("Copy TM, source repository: {}, source asset: {}, target repository: {}, target asset: {}",
                copyTmConfig.getSourceRepositoryId(),
                copyTmConfig.getSourceAssetId(),
                copyTmConfig.getTargetRepositoryId(),
                copyTmConfig.getTargetAssetId());

        Repository sourceRepository = getRepositoryForCopy(copyTmConfig.getSourceRepositoryId(), copyTmConfig.getSourceAssetId());
        Repository targetRepository = getRepositoryForCopy(copyTmConfig.getTargetRepositoryId(), copyTmConfig.getTargetAssetId());

        List<TMTextUnit> textUnitsForCopyTM = getTextUnitsForCopyTM(targetRepository, copyTmConfig.getTargetAssetId(), copyTmConfig.getNameRegex());

        if (CopyTmConfig.Mode.TUIDS.equals(copyTmConfig.getMode())) {
            copyTranslationBetweenTextUnits(copyTmConfig.getSourceToTargetTmTextUnitIds());
        } else if (CopyTmConfig.Mode.MD5.equals(copyTmConfig.getMode())) {
            leveragerByMd5.performLeveragingFor(textUnitsForCopyTM, sourceRepository.getTm().getId(), copyTmConfig.getSourceAssetId());
        } else {
            logger.debug("First perform leveraging by name and content (to give priority to string with same tags");
            leveragerByNameAndContent.performLeveragingFor(textUnitsForCopyTM, sourceRepository.getTm().getId(), copyTmConfig.getSourceAssetId());

            logger.debug("Now, perform leveraging only on the name");
            leveragerByContent.performLeveragingFor(textUnitsForCopyTM, sourceRepository.getTm().getId(), copyTmConfig.getSourceAssetId());
        }
    }


    void copyTranslationBetweenTextUnits(Map<Long, Long> sourceToTargetTmTextUnitId) {
        if (sourceToTargetTmTextUnitId != null) {
            for (Map.Entry<Long, Long> sourceToTargetTmTextUnit : sourceToTargetTmTextUnitId.entrySet()) {
                copyTranslationsBetweenTextUnits(sourceToTargetTmTextUnit.getKey(), sourceToTargetTmTextUnit.getValue());
            }
        } else {
            logger.debug("copyTranslationBetweenTextUnits called without a map, do nothing");
        }
    }

    void copyTranslationsBetweenTextUnits(Long sourceTmTextUnitId, Long targetTmTextUnitId) {
        logger.debug("Copy translations from: {} to {}", sourceTmTextUnitId, targetTmTextUnitId);
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setTmTextUnitIds(sourceTmTextUnitId);
        textUnitSearcherParameters.setRootLocaleExcluded(true);
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED_AND_NOT_REJECTED);
        List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(textUnitSearcherParameters);

        for (TextUnitDTO textUnitDTO : textUnitDTOS) {
            tmService.addCurrentTMTextUnitVariant(
                    targetTmTextUnitId,
                    textUnitDTO.getLocaleId(),
                    textUnitDTO.getTarget(),
                    textUnitDTO.getStatus(),
                    textUnitDTO.isIncludedInLocalizedFile()
            );
        }
    }

    Repository getRepositoryForCopy(Long repositoryId, Long assetId) throws AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {

        Repository repository;

        if (assetId == null) {
            repository = repositoryRepository.findOne(repositoryId);

            if (repository == null) {
                throw new RepositoryWithIdNotFoundException(repositoryId);
            }
        } else {
            Asset asset = assetRepository.findOne(assetId);
            if (asset == null) {
                throw new AssetWithIdNotFoundException(assetId);
            }
            repository = asset.getRepository();
        }

        return repository;
    }

    List<TMTextUnit> getTextUnitsForCopyTM(Repository targetRepository, Long targetAssetId, String nameRegex) {
        logger.debug("Get TmTextUnit that must be processed");
        List<TMTextUnit> tmTextUnits;

        if (targetAssetId != null) {
            logger.debug("Process a single asset");
            tmTextUnits = tmTextUnitRepository.findByAssetId(targetAssetId);
        } else {
            logger.debug("Process the whole TM");
            tmTextUnits = tmTextUnitRepository.findByTm_id(targetRepository.getTm().getId());
        }
        removeTmTextUnitsIfNameMatches(tmTextUnits, nameRegex);
        return tmTextUnits;
    }

    void removeTmTextUnitsIfNameMatches(List<TMTextUnit> tmTextUnits, String tmTextUnitNameRegex) {

        if (tmTextUnitNameRegex != null) {
            final Pattern pattern = Pattern.compile(tmTextUnitNameRegex);

            Iterables.removeIf(tmTextUnits, new Predicate<TMTextUnit>() {
                @Override
                public boolean apply(TMTextUnit tmTextUnit) {
                    return !pattern.matcher(tmTextUnit.getName()).matches();
                }
            });
        }
    }

}
