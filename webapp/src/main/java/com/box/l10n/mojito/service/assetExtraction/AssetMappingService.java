package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.leveraging.LeveragingService;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.tm.TMRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to map {@link AssetTextUnit} to {@link TMTextUnit}.
 *
 * @author jaurambault
 */
@Service
public class AssetMappingService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetMappingService.class);

    @Autowired
    TMService tmService;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    LeveragingService leveragingService;

    @Autowired
    AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TMRepository tmRepository;

    @Autowired
    RetryTemplate retryTemplate;

    /**
     * Maps the {@link AssetTextUnit}s extracted during the extraction process
     * to the existing {@link TMTextUnit}s. If no mapping is found, a new
     * {@link TMTextUnit} is created and we store the mapping.
     *
     * @param assetExtractionId a valid {@link AssetExtraction#id}
     * @param tmId              a valid {@link TM#id}
     * @param assetId           a valid {@link Asset#id}
     * @param createdByUser     user creates text unit
     * @param parentTask        the parent task to be updated
     */
    @Pollable(message = "Mapping AssetTextUnit to TMTextUnit")
    public void mapAssetTextUnitAndCreateTMTextUnit(Long assetExtractionId, Long tmId, Long assetId, User createdByUser, @ParentTask PollableTask parentTask) {

        logger.debug("Map exact matches a first time to map to existing text units");
        long mapExactMatches = mapExactMatches(assetExtractionId, tmId, assetId);
        logger.debug("{} text units were mapped the first time for asset extraction id: {} and tmId: {}", mapExactMatches, assetExtractionId, tmId);

        logger.debug("Create text units for unmapped asset text units");
        List<TMTextUnit> newlyCreatedTMTextUnits = createTMTextUnitForUnmappedAssetTextUnitsWithRetry(assetExtractionId, tmId, assetId, createdByUser);

        logger.debug("Map exact matches a second time to map newly created text units");
        int mapExactMatches2 = mapExactMatches(assetExtractionId, tmId, assetId);
        logger.debug("{} text units were mapped the second time for asset extraction id: {} and tmId: {}", mapExactMatches2, assetExtractionId, tmId);

        logger.debug("All AssetTextUnits are now mapped to TMTextUnits, start source leveraging");
        leveragingService.performSourceLeveraging(newlyCreatedTMTextUnits);

        logger.debug("Asset text unit and tm text unit mapping complete");
    }

    /**
     * Creates {@link TMTextUnit} for {@link AssetTextUnit}s that don't have a
     * MD5 matches.
     *
     * @param assetExtractionId a valid {@link AssetExtraction#id}
     * @param tmId              a valid {@link TM#id}
     * @param assetId           a valid {@link Asset#id}
     * @return the newly created {@link TMTextUnit}s
     */
    protected List<TMTextUnit> createTMTextUnitForUnmappedAssetTextUnitsWithRetry(final Long assetExtractionId, final Long tmId, final Long assetId, User createdByUser) {
        return retryTemplate.execute(new RetryCallback<List<TMTextUnit>, DataIntegrityViolationException>() {
            @Override
            public List<TMTextUnit> doWithRetry(RetryContext context) throws DataIntegrityViolationException {

                if (context.getRetryCount() > 0) {
                    long mapExactMatches = mapExactMatches(assetExtractionId, tmId, assetId);
                    logger.error("Assume concurrent modification happened, perform remapping: {}", mapExactMatches);
                }

                return createTMTextUnitForUnmappedAssetTextUnits(createdByUser, assetExtractionId, tmId, assetId);
            }
        });
    }

    @Transactional
    protected List<TMTextUnit> createTMTextUnitForUnmappedAssetTextUnits(User createdByUser, Long assetExtractionId, Long tmId, Long assetId) {

        logger.debug("Create TMTextUnit for unmapped AssetTextUnits, assetExtractionId: {} tmId: {}", assetExtractionId, tmId);
        List<TMTextUnit> newlyCreatedTMTextUnits = new ArrayList<>();

        Asset asset = assetRepository.findOne(assetId);
        TM tm = tmRepository.findOne(tmId);

        for (AssetTextUnit unmappedAssetTextUnit : assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtractionId)) {
            TMTextUnit addTMTextUnit = tmService.addTMTextUnit(
                    tm,
                    asset,
                    unmappedAssetTextUnit.getName(),
                    unmappedAssetTextUnit.getContent(),
                    unmappedAssetTextUnit.getComment(),
                    createdByUser,
                    null,
                    unmappedAssetTextUnit.getPluralForm(),
                    unmappedAssetTextUnit.getPluralFormOther());

            newlyCreatedTMTextUnits.add(addTMTextUnit);
        }

        logger.debug("Created {} TMTextUnits", newlyCreatedTMTextUnits.size());

        return newlyCreatedTMTextUnits;
    }

    /**
     * Maps exact matches by mapping {@link AssetTextUnit} to {@link TMTextUnit}
     * based on MD5s for a given {@link AssetExtraction} and {@link TM}.
     * <p>
     * The relationship is 1 to 1 because of the unique constraint on the
     * {@link TMTextUnit}: UK__TM_TEXT_UNIT__MD5__TM_ID. So
     * {@link AssetTextUnit} can be map uniquely to a {@link TMTextUnit}.
     * <p>
     * The select statement looks for all {@link AssetTextUnit}s that are not
     * yet mapped and try to find a match by MD5 in a specified {@link TM}.
     *
     * @param assetExtractionId {@link AssetExtraction} id
     * @param tmId              {@link TM} id
     * @param assetId           {@link Asset} id
     * @return number of exact matches
     */
    protected int mapExactMatches(Long assetExtractionId, Long tmId, Long assetId) {
        List<AssetMappingDTO> exactMatches = getExactMatches(assetExtractionId, tmId, assetId);
        return saveExactMatches(exactMatches);
    }

    /**
     * Finds exact matches that are not mapped in
     * {@link AssetTextUnitToTMTextUnit}. It maps {@link AssetTextUnit} to
     * {@link TMTextUnit} based on MD5s for a given {@link AssetExtraction} and
     * {@link TM}.
     *
     * @param assetExtractionId
     * @param tmId
     * @param assetId
     * @return
     */
    @Transactional
    protected List<AssetMappingDTO> getExactMatches(Long assetExtractionId, Long tmId, Long assetId) {
        Query createNativeQuery = entityManager.createNamedQuery("AssetTextUnitToTMTextUnit.getExactMatches");
        createNativeQuery.setParameter(1, assetExtractionId);
        createNativeQuery.setParameter(2, tmId);
        createNativeQuery.setParameter(3, assetId);
        return createNativeQuery.getResultList();
    }

    /**
     * Saves exact matches in {@link AssetTextUnitToTMTextUnit}.
     *
     * @param exactMatches to be saved
     * @return
     */
    @Transactional
    protected int saveExactMatches(List<AssetMappingDTO> exactMatches) {
        List<AssetTextUnitToTMTextUnit> assetTextUnitToTMTextUnits = new ArrayList<>();
        for (AssetMappingDTO exactMatch : exactMatches) {
            AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();

            assetTextUnitToTMTextUnit.setAssetExtraction(assetExtractionRepository.getOne(exactMatch.getAssetExtractionId()));
            assetTextUnitToTMTextUnit.setAssetTextUnit(assetTextUnitRepository.getOne(exactMatch.getAssetTextUnitId()));
            assetTextUnitToTMTextUnit.setTmTextUnit(tmTextUnitRepository.getOne(exactMatch.getTmTextUnitId()));
            assetTextUnitToTMTextUnits.add(assetTextUnitToTMTextUnit);
        }
        assetTextUnitToTMTextUnits = assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnits);
        return assetTextUnitToTMTextUnits.size();
    }

}
