package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
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
    LeveragerByContentForSourceLeveraging leveragerByContentForSourceLeveraging;

    @Autowired
    LeveragerByNameForSourceLeveraging leveragerByNameForSourceLeveraging;

    @Autowired
    LeveragerByMd5 leveragerByMd5;
    
    @Autowired
    LeveragerByContent leveragerByContent;
    
    @Autowired
    LeveragerByNameAndContent leveragerByNameAndContent;
    
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
     * processed. {@link TMTextUnit}s for which leveraged translations were
     * found are removed from the list to prevent further processing.
     */
    public void performSourceLeveraging(List<TMTextUnit> tmTextUnits) {

        logger.debug("Perform source leveraging for {} text units", tmTextUnits.size());

        leveragerByNameAndContentForSourceLeveraging.performLeveragingFor(tmTextUnits, null);
        leveragerByNameForSourceLeveraging.performLeveragingFor(tmTextUnits, null);
        leveragerByContentForSourceLeveraging.performLeveragingFor(tmTextUnits, null);
    }

    /**
     * This will copy all translations from the source repository into the
     * target repository, overriding any translation in the target repository.
     *
     * Matches are performed based on MD5, if the repository has multiple text
     * units with same MD5 the source will be arbitrarily chosen.
     *
     * @param source the source repository
     * @param target the target repository
     */
    @Pollable(async = true, message = "Start copying all translations with MD5 match between repository")
    public PollableFuture copyAllTranslationsWithMD5MatchBetweenRepositories(Repository source, Repository target) {
        
        logger.debug("Get TmTextUnit that must be processed");
        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(target.getTm().getId());
        leveragerByMd5.performLeveragingFor(tmTextUnits, source.getTm().getId());
        
        return new PollableFutureTaskResult();
    }

    /**
     * This will copy all translations from the source repository into the
     * target repository, overriding any translation in the target repository.
     *
     * Matches are performed based on content only (exact match), 
     * if the repository has multiple text units with same content it will 
     * first check for string with same IDs and then the source will 
     * be arbitrarily chosen.
     *
     * @param source the source repository
     * @param target the target repository
     */
    @Pollable(async = true, message = "Start copying all translations with exact match between repository")
    public PollableFuture copyAllTranslationsWithExactMatchBetweenRepositories(Repository source, Repository target) {

        logger.debug("Get TmTextUnit that must be processed");
        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(target.getTm().getId());
       
        logger.debug("First perform leveraging by name and content (to give priority to string with same tags");
        leveragerByNameAndContent.performLeveragingFor(tmTextUnits, source.getTm().getId());
        
        logger.debug("Now, perform leveraging only on the name");
        leveragerByContent.performLeveragingFor(tmTextUnits, source.getTm().getId());
        
        return new PollableFutureTaskResult();
    }

}
