package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.TextContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 *
 *
 * @author jaurambault
 */
@Configurable
public class ImportTranslationsFromLocalizedAssetStep extends AbstractImportTranslationsStep {

    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(ImportTranslationsFromLocalizedAssetStep.class);

    @Autowired
    TextUnitSearcher textUnitSearcher;

    Asset asset;
    RepositoryLocale repositoryLocale;
    StatusForEqualTarget statusForEqualTarget;

    Map<String, TMTextUnit> textUnitsByMd5 = new HashMap<>();
    Map<String, TMTextUnit> textUnitsByName = new HashMap<>();
    TranslatorWithInheritance translatorWithInheritance;
    boolean hasTranslationWithoutInheritance;

    public enum StatusForEqualTarget {
        SKIPPED,
        REVIEW_NEEDED,
        TRANSLATION_NEEDED,
        APPROVED
    };

    public ImportTranslationsFromLocalizedAssetStep(
            Asset asset,
            RepositoryLocale repositoryLocale,
            StatusForEqualTarget statusForEqualTarget) {
        this.asset = asset;
        this.repositoryLocale = repositoryLocale;
        this.statusForEqualTarget = statusForEqualTarget;
    }

    @Override
    protected Event handleStartDocument(Event event) {
        event = super.handleStartDocument(event);
        initTmTextUnitsMapsForAsset();
        translatorWithInheritance = new TranslatorWithInheritance(asset, repositoryLocale, InheritanceMode.USE_PARENT);
        hasTranslationWithoutInheritance = translatorWithInheritance.hasTranslationWithoutInheritance();
        return event;
    }

    void initTmTextUnitsMapsForAsset() {
        logger.debug("Init TmTextUnit maps for asset");
        List<TMTextUnit> textUnits = tmTextUnitRepository.findByAsset(asset);

        for (TMTextUnit tmTextUnit : textUnits) {
            if (isMultilingual) {
                textUnitsByMd5.put(tmTextUnit.getMd5(), tmTextUnit);
            } else {
                textUnitsByName.put(tmTextUnit.getName(), tmTextUnit);
            }
        }
    }

    @Override
    public String getName() {
        return "Import translations from a localized asset (multilingual or not)";
    }

    @Override
    public String getDescription() {
        return "Updates the TM with the extracted new/changed variants."
                + " Expects: raw document. Sends back: original events.";
    }

    @Override
    TMTextUnit getTMTextUnit() {

        TMTextUnit tmTextUnit;
        if (isMultilingual) {
            tmTextUnit = textUnitsByMd5.get(md5);
        } else {
            tmTextUnit = textUnitsByName.get(name);
        }

        return tmTextUnit;
    }

    @Override
    protected TMTextUnitVariant.Status getStatusForImport(TMTextUnit tmTextUnit, TextContainer target) {

        TMTextUnitVariant.Status status;

        TextUnitDTO currentTranslation = translatorWithInheritance.getTextUnitDTO(
                tmTextUnit.getName(),
                tmTextUnit.getContent(),
                tmTextUnit.getMd5());
        
        boolean hasSameTarget;
        String targetAsString = target.toString();

        logger.debug("Check if new target: [{}] is different to either the current, parent or source string: [{}]",
                targetAsString, currentTranslation);
        
        if (currentTranslation != null) {
            hasSameTarget = targetAsString.equals(currentTranslation.getTarget());
        } else {
            logger.debug("No current or parent, compare with the source");
            hasSameTarget = targetAsString.equals(tmTextUnit.getContent());
        }

        if (hasSameTarget) {
            logger.debug("Target is the same");
            if (repositoryLocale.isToBeFullyTranslated()) {                
                boolean isForTargetLocale = currentTranslation != null && currentTranslation.getLocaleId().equals(repositoryLocale.getLocale().getId());
                status = getStatusForSameTargetAndFullyTranslated(isForTargetLocale);
            } else {
                logger.debug("Locale is not fully translated, skip target as it is the same");
                status = null;
            }
        } else {
            logger.debug("Target is different, import as approved");
            status = TMTextUnitVariant.Status.APPROVED;
        }
        
        return status;
    }

    TMTextUnitVariant.Status getStatusForSameTargetAndFullyTranslated(boolean isForTargetLocale) {
        logger.debug("Get status when target is same for a fully translated locale");
        
        TMTextUnitVariant.Status status;
        
        if(isForTargetLocale) {
            logger.debug("Same target for target locale, skip");
            status = null;
        } else if (StatusForEqualTarget.TRANSLATION_NEEDED.equals(statusForEqualTarget)) {
            status = TMTextUnitVariant.Status.TRANSLATION_NEEDED;
        } else if (StatusForEqualTarget.REVIEW_NEEDED.equals(statusForEqualTarget)) {
            status = TMTextUnitVariant.Status.REVIEW_NEEDED;
        } else if (StatusForEqualTarget.SKIPPED.equals(statusForEqualTarget)) {
            status = null;
        } else {
            status = TMTextUnitVariant.Status.APPROVED;
        }
        
        return status;
    }

    /**
     * Optimize by skipping the look up of the tmTextUnitCurrentVariant when then there is no translation.
     * 
     * This is to optimize for the first import of big projects, looking up for tmTextUnitCurrentVariants is expensive
     * and not required as none is present yet.
     * 
     * @param localeId
     * @param tmTextUnit
     * @return 
     */
    @Override
    TMTextUnitCurrentVariant getTMTextUnitCurrentVariant(Long localeId, TMTextUnit tmTextUnit) {
        
        TMTextUnitCurrentVariant tmTextUnitCurrentVariant = null;
        
        if (hasTranslationWithoutInheritance) {
            return super.getTMTextUnitCurrentVariant(localeId, tmTextUnit);
        }
        
        return tmTextUnitCurrentVariant;
    }
    
}
