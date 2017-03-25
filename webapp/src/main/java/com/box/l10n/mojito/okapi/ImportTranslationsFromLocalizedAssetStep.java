package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.List;
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
    StatusForSourceEqTarget statusForSourceEqTarget;

    public enum StatusForSourceEqTarget {
        SKIPPED,
        REVIEW_NEEDED,
        TRANSLATION_NEEDED,
        APPROVED
    };

    public ImportTranslationsFromLocalizedAssetStep(
            Asset asset,
            RepositoryLocale repositoryLocale,
            StatusForSourceEqTarget sourceEqualTargetProcessing) {
        this.asset = asset;
        this.repositoryLocale = repositoryLocale;
        this.statusForSourceEqTarget = sourceEqualTargetProcessing;        
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
            tmTextUnit = tmTextUnitRepository.findFirstByAssetAndMd5(asset, md5);
        } else {
            tmTextUnit = tmTextUnitRepository.findFirstByAssetAndName(asset, name);
        }

        return tmTextUnit;
    }

    /**
     * Decide if the translation should be imported.
     *
     * For not fully translated locales, translations are imported only if they are
     * different of the translation of the parent locale.
     *
     * If source = target the string is imported and will be marked as needs
     * review. We could have an option to mark them as accept or skip import
     *
     * @param tmTextUnit
     * @param target
     * @return
     */
    @Override
    protected boolean shouldImport(TMTextUnit tmTextUnit, TextContainer target) {

        boolean shouldImport = true;

        if (isSourceEqualsToTarget(target, tmTextUnit)) {
            shouldImport = !StatusForSourceEqTarget.SKIPPED.equals(statusForSourceEqTarget) && repositoryLocale.isToBeFullyTranslated();
        } else if (!repositoryLocale.isToBeFullyTranslated()) {
            shouldImport = !isSameAsParentLocale(tmTextUnit, target);
        }

        return shouldImport;

    }

    /**
     * Check if the target to be imported is the same as the parent target.
     * 
     * @param tmTextUnit
     * @param target
     * @return if the target is the same as the parent target or not
     */
    boolean isSameAsParentLocale(TMTextUnit tmTextUnit, TextContainer target) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        
        textUnitSearcherParameters.setTmTextUnitId(tmTextUnit.getId());
        textUnitSearcherParameters.setLocaleId(repositoryLocale.getParentLocale().getLocale().getId());
        List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
        
        return !search.isEmpty() && search.get(0).getTarget().equals(target.toString());
    }
 
    @Override
    protected TMTextUnitVariant.Status getStatusForAddingTranslation(TextContainer target, boolean hasError, TMTextUnit tmTextUnit) {
        TMTextUnitVariant.Status status = TMTextUnitVariant.Status.APPROVED;

        if (isSourceEqualsToTarget(target, tmTextUnit)) {
            if (StatusForSourceEqTarget.APPROVED.equals(statusForSourceEqTarget)) {
                status = TMTextUnitVariant.Status.APPROVED;
            } else if (StatusForSourceEqTarget.TRANSLATION_NEEDED.equals(statusForSourceEqTarget)) {
                status = TMTextUnitVariant.Status.TRANSLATION_NEEDED;
            } else if (StatusForSourceEqTarget.REVIEW_NEEDED.equals(statusForSourceEqTarget)) {
                status = TMTextUnitVariant.Status.REVIEW_NEEDED;
            } else if (StatusForSourceEqTarget.SKIPPED.equals(statusForSourceEqTarget)) {
                throw new RuntimeException("Shouldn't be adding a translation with SKIP");
            } 
        } 
        
        return status;
    }

    /**
     * Indicates if the source is the same as the target
     * 
     * @param target
     * @param tmTextUnit
     * @return if the source is the same as the target or not
     */
    private boolean isSourceEqualsToTarget(TextContainer target, TMTextUnit tmTextUnit) {
        return target.toString().equals(tmTextUnit.getContent());
    }

}
