package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.assetExtraction.AssetMappingService;
import com.box.l10n.mojito.service.tm.AddTMTextUnitCurrentVariantResult;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Iterator;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract class that performs leveraging. See {@link AssetMappingService#performSourceLeveraging(java.util.List)
 * }, for an explanation on leveraging usage.
 *
 * @author jaurambault
 */
public abstract class AbstractLeverager {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AbstractLeverager.class);

    @Autowired
    protected TextUnitSearcher textUnitSearcher;

    @Autowired
    TMService tmService;

    @Autowired
    TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

    /**
     * Gets {@link TextUnitDTO}s that matches the {@link TMTextUnit} based on
     * different criteria defined by the implementing class.
     *
     * <p>
     * Those matches potentially contain entries for all locales and there might
     * be multiple results per locale (don't assume uniqueness in that case).
     * The results can then be filter using {@link AbstractLeverager#filterTextUnitDTOWithSameTMTextUnitId(java.util.List)
     * }.
     *
     * @param tmTextUnit the {@link TMTextUnit}
     * @param sourceTmId the {@link TM#id} of TM to use to look for matches into
     * (can be null)
     * @return a list of {@link TextUnitDTO}s for leveraging
     */
    public abstract List<TextUnitDTO> getLeveragingMatches(TMTextUnit tmTextUnit, Long sourceTmId);

    /**
     * Indicates if the translations must be re-translated regardless of if
     * there is a unique match for leveraging or not.
     *
     * @return {@code true} if the translation must be translated else
     * {@code false}
     */
    public abstract boolean isTranslationNeededIfUniqueMatch();

    /**
     * A string representation of the leveraging type.
     *
     * @return the leveraging type
     */
    public abstract String getType();

    /**
     * Performs leveraging for a list of {@link TMTextUnit}s.
     * <p/>
     * This process gets a list of candidate translations to be copied over for
     * each {@link TMTextUnit}. If there are candidate translations, first
     * filter them to make sure the copied translations will come from a unique
     * {@link TMTextUnit}s and then copies them. The TMTextUnits for which
     * translations were added are removed from the list to prevent further
     * processing.
     *
     * @param tmTextUnits list of {@link TMTextUnit}s that needs to be
     * processed. {@link TMTextUnit}s for which leveraged translations were
     * found are removed from the list to prevent further processing.
     * @param sourceTmId the {@link TM#id} of TM to use to look for matches into
     * (can be null)
     */
    public void performLeveragingFor(List<TMTextUnit> tmTextUnits, Long sourceTmId) {

        logger.debug("Perform leveraging: {}", getType());

        for (Iterator<TMTextUnit> tmTextUnitsIterator = tmTextUnits.iterator(); tmTextUnitsIterator.hasNext();) {

            TMTextUnit tmTextUnit = tmTextUnitsIterator.next();

            logger.debug("Get list of TextUnitDTOs (contains translations to be copied) for name: {}", tmTextUnit.getName());
            List<TextUnitDTO> textUnitDTOsForLeveraging = getLeveragingMatches(tmTextUnit, sourceTmId);

            if (!textUnitDTOsForLeveraging.isEmpty()) {

                logger.debug("Match found for this TMTextUnit wiht name: {}, remove from the list of TMTextUnit that needs leveraging", tmTextUnit.getName());
                tmTextUnitsIterator.remove();

                logger.debug("Filters the translations and check for uniqueness of the matches");
                int textUnitDTOsForLeveragingSize = textUnitDTOsForLeveraging.size();
                filterTextUnitDTOWithSameTMTextUnitId(textUnitDTOsForLeveraging);
                boolean uniqueTMTextUnitMatched = textUnitDTOsForLeveragingSize == textUnitDTOsForLeveraging.size();

                logger.debug("Determine if re-translation is needed for the strings that will be copied");
                boolean translationNeeded = isTranslationNeededIfUniqueMatch() || !uniqueTMTextUnitMatched;

                addLeveragedTranslations(tmTextUnit, textUnitDTOsForLeveraging, translationNeeded, uniqueTMTextUnitMatched);
            } else {
                logger.debug("No Match found for this TMTextUnit with name: {}", tmTextUnit.getName());
            }
        }
    }

    /**
     * Adds translations (potentially to be re-translated) into the
     * {@link TMTextUnit}.
     *
     * @param tmTextUnit that will receive leveraged translations (if any
     * matches)
     * @param translations the translations to be copied over
     * @param translationNeeded {@code true} if the translation needs to be send
     * for translation
     * @param uniqueTMTextUnitMatched {@link true} if there was a unique
     * {@link TMTextUnit} match when getting the translations. if {@code false}
     * it could indicate that wrong translations were picked up as it chooses
     * arbitrarily the one to use for leveraging.
     */
    @Transactional
    private void addLeveragedTranslations(TMTextUnit tmTextUnit, List<TextUnitDTO> translations, boolean translationNeeded, boolean uniqueTMTextUnitMatched) {

        logger.debug("Add leveraged translations in tmTextUnit, id: {}", tmTextUnit.getId());

        for (TextUnitDTO translation : translations) {

            AddTMTextUnitCurrentVariantResult addTMTextUnitCurrentVariantWithResult = tmService.addTMTextUnitCurrentVariantWithResult(tmTextUnit.getId(),
                    translation.getLocaleId(),
                    translation.getTarget(),
                    translation.getTargetComment(),
                    translationNeeded ? TMTextUnitVariant.Status.TRANSLATION_NEEDED : translation.getStatus(),
                    translation.isIncludedInLocalizedFile(),
                    null);
            
            TMTextUnitCurrentVariant addTMTextUnitCurrentVariant = addTMTextUnitCurrentVariantWithResult.getTmTextUnitCurrentVariant();
            
            if (addTMTextUnitCurrentVariantWithResult.isTmTextUnitCurrentVariantUpdated()) {
                logger.debug("Changed were made to the TmTextUnitCurrentVariant, need to copy comments and add the leveraging comment");
                tmTextUnitVariantCommentService.copyComments(translation.getTmTextUnitVariantId(), addTMTextUnitCurrentVariant.getTmTextUnitVariant().getId());

                tmTextUnitVariantCommentService.addComment(
                        addTMTextUnitCurrentVariant.getTmTextUnitVariant(),
                        TMTextUnitVariantComment.Type.LEVERAGING,
                        TMTextUnitVariantComment.Severity.INFO,
                        getLeverageComment(translation, uniqueTMTextUnitMatched));
            }
            
            logger.debug("Added leveraged translation, id: {}", addTMTextUnitCurrentVariant.getId());
        }
    }

    private String getLeverageComment(TextUnitDTO translation, boolean uniqueTMTextUnitMatched) {
        return getType() + " leveraging from: " + translation.getTmTextUnitVariantId() + ", unique match: " + uniqueTMTextUnitMatched;
    }

    /**
     * Arbitrarily take the first TMTextUnit id in the list and filters the
     * {@link TextUnitDTO} list by this id.
     *
     * <p>
     * For consistency we want to use translations that come only from a single
     * TMTextUnit. Most of the time the dataset will contain translation only
     * from a single TMTextUnit but it is not mandatory.
     *
     * @param textUnitDTOs list of {@link TextUnitDTO}s that needs to be
     * filtered
     */
    protected void filterTextUnitDTOWithSameTMTextUnitId(List<TextUnitDTO> textUnitDTOs) {

        logger.debug("Filter the TextUnitDTOs that have the same TMTextUnit id");

        if (!textUnitDTOs.isEmpty()) {
            logger.debug("Take arbitrarily the first TMTextUnit id as the one that will be used to perform leveraging");
            Long tmTextUnitIdForLeveraging = textUnitDTOs.get(0).getTmTextUnitId();

            for (Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator(); iterator.hasNext();) {

                TextUnitDTO textUnitDTO = iterator.next();

                if (textUnitDTO.getTmTextUnitId().equals(tmTextUnitIdForLeveraging)) {
                    logger.debug("This translation comes from the same TMTextUnit, keep it");
                } else {
                    logger.debug("This translation comes from another TMTextUnit, for consistency skip it.");
                    iterator.remove();
                }
            }
        }
    }

}
